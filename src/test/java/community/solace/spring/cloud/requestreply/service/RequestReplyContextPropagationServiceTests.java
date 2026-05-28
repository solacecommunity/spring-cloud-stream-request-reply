package community.solace.spring.cloud.requestreply.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.integration.Slf4jThreadLocalAccessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Verifies that the MDC (and any other registered thread-local context) captured on the
 * calling thread is propagated to every stage of the asynchronous request/reply pipeline,
 * which is the behaviour requested in
 * <a href="https://github.com/solacecommunity/spring-cloud-stream-request-reply/issues/50">issue&nbsp;#50</a>.
 *
 * <p>Out of the box neither {@code context-propagation} nor this library registers a
 * thread-local accessor for the SLF4J MDC; observability libraries normally do. These tests
 * register the {@link Slf4jThreadLocalAccessor} on the global {@link ContextRegistry} for the
 * duration of the class to mirror that production setup, and remove it again afterwards so no
 * other test is affected.</p>
 */
class RequestReplyContextPropagationServiceTests extends AbstractRequestReplySimpleIT {

    private static final String TRACE_ID = "traceId";
    private static final String TOPIC = "last_value/temperature/celsius/demo";

    @MockitoBean
    private StreamBridge streamBridge;
    @Autowired
    private RequestReplyServiceImpl requestReplyService;

    @BeforeAll
    void registerMdcAccessor() {
        ContextRegistry.getInstance()
                       .registerThreadLocalAccessor(new Slf4jThreadLocalAccessor());

        // Guard against a logging backend without working MDC support: the whole suite is
        // meaningless if MDC.put is a no-op.
        MDC.put(TRACE_ID, "sanity");
        assertEquals("sanity", MDC.get(TRACE_ID), "MDC is not backed by a real SLF4J implementation");
        MDC.remove(TRACE_ID);
    }

    @AfterAll
    void removeMdcAccessor() {
        ContextRegistry.getInstance()
                       .removeThreadLocalAccessor(Slf4jThreadLocalAccessor.KEY);
    }

    @BeforeEach
    void clearMdc() {
        MDC.clear();
    }

    @AfterEach
    void clearMdcAfter() {
        MDC.clear();
    }

    @Test
    void context_isPropagatedToRequestSendingThread() throws TimeoutException, RemoteErrorException, InterruptedException {
        AtomicReference<String> traceIdOnSendThread = new AtomicReference<>();
        AtomicReference<String> sendThreadName = new AtomicReference<>();

        stubSendAndAutoReply(traceIdOnSendThread, sendThreadName, new SensorReading());

        MDC.put(TRACE_ID, "trace-send");

        requestReplyService.requestAndAwaitReplyToTopic(
                new SensorReading(),
                TOPIC,
                SensorReading.class,
                Duration.ofSeconds(10)
        );

        // The request is sent from a pooled executor thread, so propagation genuinely crossed a
        // thread boundary rather than reading the caller thread's own MDC.
        assertNotEquals(Thread.currentThread().getName(), sendThreadName.get(),
                "request should be sent on a pooled executor thread, not the caller thread");
        assertEquals("trace-send", traceIdOnSendThread.get(),
                "MDC set on the caller thread must be visible on the request-sending executor thread");

        resetMocks();
    }

    @Test
    void context_isPropagatedToStageChainedOnReturnedFuture() throws Exception {
        CountDownLatch sendHappened = new CountDownLatch(1);
        AtomicReference<Message<?>> sentMessage = new AtomicReference<>();

        // Do NOT reply from within send(): keep the internal future pending so the stage we
        // chain below is guaranteed to run asynchronously on a (context-restored) executor
        // thread rather than synchronously on the test thread.
        Mockito.when(streamBridge.send(anyString(), any(Message.class)))
               .thenAnswer(invocation -> {
                   sentMessage.set(invocation.getArgument(1));
                   sendHappened.countDown();
                   return true;
               });

        MDC.put(TRACE_ID, "trace-chain");

        CompletableFuture<SensorReading> future = requestReplyService.requestReplyToTopic(
                new SensorReading(),
                TOPIC,
                SensorReading.class,
                Duration.ofSeconds(10)
        );

        AtomicReference<String> traceIdInChainedStage = new AtomicReference<>();
        AtomicReference<String> chainedStageThread = new AtomicReference<>();
        CompletableFuture<SensorReading> observed = future.whenComplete((result, error) -> {
            chainedStageThread.set(Thread.currentThread().getName());
            traceIdInChainedStage.set(MDC.get(TRACE_ID));
        });

        // Reply from a separate thread that carries no MDC of its own, emulating the Solace
        // consumer thread. This proves the context comes from the original request, not from
        // whichever thread happens to deliver the reply.
        assertTrue(sendHappened.await(5, TimeUnit.SECONDS), "request was never sent");
        Thread replier = new Thread(() -> {
            MDC.clear();
            requestReplyService.onReplyReceived(
                    MessageBuilder.createMessage(new SensorReading(), sentMessage.get().getHeaders())
            );
        }, "reply-injector");
        replier.start();
        replier.join();

        observed.get(5, TimeUnit.SECONDS);

        assertEquals("trace-chain", traceIdInChainedStage.get(),
                "MDC must be propagated to an application stage chained on the returned CompletableFuture");
        assertNotEquals("reply-injector", chainedStageThread.get(),
                "the chained stage must not run on the reply-delivering thread");

        resetMocks();
    }

    @Test
    void context_isNotLeakedToASubsequentRequestWithoutContext() throws TimeoutException, RemoteErrorException, InterruptedException {
        // CopyOnWriteArrayList permits null elements, which lets us record a missing MDC verbatim.
        List<String> traceIdsOnSendThread = new CopyOnWriteArrayList<>();

        Mockito.when(streamBridge.send(anyString(), any(Message.class)))
               .thenAnswer(invocation -> {
                   traceIdsOnSendThread.add(MDC.get(TRACE_ID));
                   requestReplyService.onReplyReceived(
                           MessageBuilder.createMessage(
                                   new SensorReading(),
                                   ((Message<?>) invocation.getArgument(1)).getHeaders()
                           )
                   );
                   return true;
               });

        // First request carries an MDC value.
        MDC.put(TRACE_ID, "leaky-value");
        requestReplyService.requestAndAwaitReplyToTopic(new SensorReading(), TOPIC, SensorReading.class, Duration.ofSeconds(10));

        // Let the pooled thread become idle so it is likely reused for the next request.
        await().atMost(Duration.ofSeconds(3)).until(requestReplyService::runningRequests, equalTo(0));
        MDC.clear();

        // Second request carries no MDC: a reused executor thread must not still see "leaky-value".
        requestReplyService.requestAndAwaitReplyToTopic(new SensorReading(), TOPIC, SensorReading.class, Duration.ofSeconds(10));

        // Both requests run synchronously to completion on the caller thread, so the order is stable.
        assertEquals(2, traceIdsOnSendThread.size(), "exactly two requests should have been sent");
        assertEquals("leaky-value", traceIdsOnSendThread.get(0),
                "the first request must observe its own MDC value on the executor thread");
        assertNull(traceIdsOnSendThread.get(1),
                "the second request must see a clean (null) MDC on the executor thread, i.e. no context leak");

        resetMocks();
    }

    @Test
    void callerThreadContext_isUnchangedByARequest() throws TimeoutException, RemoteErrorException, InterruptedException {
        stubSendAndAutoReply(new AtomicReference<>(), new AtomicReference<>(), new SensorReading());

        MDC.put(TRACE_ID, "caller-owned");

        requestReplyService.requestAndAwaitReplyToTopic(new SensorReading(), TOPIC, SensorReading.class, Duration.ofSeconds(10));

        assertEquals("caller-owned", MDC.get(TRACE_ID),
                "the caller thread's MDC must be left intact after the request completes");

        resetMocks();
    }

    @Test
    void runningRequests_tracksInFlightRequest() throws Exception {
        CountDownLatch sendHappened = new CountDownLatch(1);
        AtomicReference<Message<?>> sentMessage = new AtomicReference<>();

        Mockito.when(streamBridge.send(anyString(), any(Message.class)))
               .thenAnswer(invocation -> {
                   sentMessage.set(invocation.getArgument(1));
                   sendHappened.countDown();
                   return true;
               });

        CompletableFuture<SensorReading> future = requestReplyService.requestReplyToTopic(
                new SensorReading(), TOPIC, SensorReading.class, Duration.ofSeconds(10)
        );

        assertTrue(sendHappened.await(5, TimeUnit.SECONDS), "request was never sent");
        await().atMost(Duration.ofSeconds(3))
               .until(requestReplyService::runningRequests, greaterThanOrEqualTo(1));

        Thread replier = new Thread(() -> requestReplyService.onReplyReceived(
                MessageBuilder.createMessage(new SensorReading(), sentMessage.get().getHeaders())
        ), "reply-injector");
        replier.start();
        replier.join();

        future.get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(3)).until(requestReplyService::runningRequests, equalTo(0));

        resetMocks();
    }

    @Test
    void timeout_releasesExecutorThread() {
        Mockito.when(streamBridge.send(anyString(), any(Message.class)))
               .thenReturn(true); // never reply -> force a timeout

        MDC.put(TRACE_ID, "trace-timeout");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                new SensorReading(), TOPIC, SensorReading.class, Duration.ofMillis(200)
        ));

        // The blocked worker must be released by the abort path; otherwise the pool leaks threads.
        await().atMost(Duration.ofSeconds(5)).until(requestReplyService::runningRequests, equalTo(0));

        resetMocks();
    }

    private void stubSendAndAutoReply(
            AtomicReference<String> traceIdOnSendThread,
            AtomicReference<String> sendThreadName,
            SensorReading response
    ) {
        Mockito.when(streamBridge.send(anyString(), any(Message.class)))
               .thenAnswer(invocation -> {
                   traceIdOnSendThread.set(MDC.get(TRACE_ID));
                   sendThreadName.set(Thread.currentThread().getName());
                   requestReplyService.onReplyReceived(
                           MessageBuilder.createMessage(
                                   response,
                                   ((Message<?>) invocation.getArgument(1)).getHeaders()
                           )
                   );
                   return true;
               });
    }
}
