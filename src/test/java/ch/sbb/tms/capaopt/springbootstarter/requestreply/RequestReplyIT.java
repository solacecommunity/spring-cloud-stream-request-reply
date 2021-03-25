package ch.sbb.tms.capaopt.springbootstarter.requestreply;

import ch.sbb.tms.capaopt.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.capaopt.springbootstarter.requestreply.service.RequestReplyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.integration.RequestReplyTestEndpoint.MC_REVERSE_IN;
import static ch.sbb.tms.capaopt.springbootstarter.requestreply.integration.RequestReplyTestEndpoint.MC_SUCCESS_CHANNEL;
import static ch.sbb.tms.capaopt.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;
import static org.springframework.util.StringUtils.hasText;

class RequestReplyIT extends AbstractRequestReplyIT {
    private static final String MESSAGE = "Hier könnte Ihre Werbung stehen!";
    private static final String MESSAGE_REVERSED = "!nehets gnubreW erhI etnnök reiH";

    @Autowired
    private RequestReplyProperties requestReplyConfig;

    @Autowired
    private RequestReplyService requestReplyService;

    @Test
    void contextLoadsAndConfigurationShouldBeInitializedProperly() {
        assertNotNull(requestReplyConfig);
        assertTrue(hasText(requestReplyConfig.getRequestReplyGroupName()));
    }

    @Test
    void submitRequestAndRelateReplyInTimeShouldSucceed() throws Exception {
        long timeoutInMs = 500;
        
        UUID uuid = UUID.randomUUID();
        String correlationId = uuid.toString();
        
        AtomicReference<UUID> ref = new AtomicReference<>();
        Runnable requestRunnable = throwingUnchecked(() -> {
            TimeUnit.MILLISECONDS.sleep(timeoutInMs);
            Message<UUID> msg = MessageBuilder.withPayload(uuid).setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId)
                    .build();
            invokeMethod(requestReplyService, "onReplyReceived", msg);
        });
        Consumer<Message<?>> responseConsumer = msg -> {
            ref.set(uuid);
        };
        
        CompletableFuture<Void> future = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutInMs * 2);

        future.get(); // await completion
        assertEquals(uuid, ref.get());
    }

    @Test
    void resubmitRequestShouldThrowExceptionWhilstCompletingFirstRequest() throws Exception {
        long timeoutInMs = 500;

        UUID uuid = UUID.randomUUID();
        String correlationId = uuid.toString();

        AtomicReference<UUID> ref = new AtomicReference<>();
        Runnable requestRunnable = throwingUnchecked(() -> {
            TimeUnit.MILLISECONDS.sleep(timeoutInMs);
            Message<UUID> msg = MessageBuilder.withPayload(uuid).setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId)
                    .build();
            invokeMethod(requestReplyService, "onReplyReceived", msg);
        });
        Consumer<Message<?>> responseConsumer = msg -> {
            ref.set(uuid);
        };
        Consumer<Message<?>> exceptionalConsumer = msg -> {
            throw new IllegalStateException();
        };

        CompletableFuture<Void> future1 = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutInMs * 2);
        assertThrows(IllegalArgumentException.class, () -> invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, exceptionalConsumer, timeoutInMs * 2));

        future1.get(); // await completion
        assertEquals(uuid, ref.get());
    }

    @Test
    void submitRequestAndRelateReplyOutOfTimeShouldReturnNull() throws Exception {
        long timeoutInMs = 500;

        UUID uuid = UUID.randomUUID();
        String correlationId = uuid.toString();

        AtomicReference<UUID> ref = new AtomicReference<>();
        Runnable requestRunnable = throwingUnchecked(() -> {
            TimeUnit.MILLISECONDS.sleep(timeoutInMs);
            Message<UUID> msg = MessageBuilder.withPayload(uuid).setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId)
                    .build();
            invokeMethod(requestReplyService, "onReplyReceived", msg);
        });
        Consumer<Message<?>> responseConsumer = msg -> {
            ref.set(uuid);
        };

        CompletableFuture<Void> future = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutInMs / 2);

        try {
            future.get();
        }
        catch (Exception e) {
            assertTrue(e instanceof ExecutionException);
            assertTrue(e.getCause() instanceof TimeoutException);
        }
    }

    @Test
    void onReplyReceivedForUnknownMessageShouldSilentlyFail() {
        UUID uuid = UUID.randomUUID();
        Message<UUID> msg = MessageBuilder.withPayload(uuid).setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, uuid.toString())
                .build();
        invokeMethod(requestReplyService, "onReplyReceived", msg);
    }

    @Test
    void requestAndAwaitReplyShouldSucceed() throws Exception {
        CompletableFuture<String> future = requestReplyService.requestAndAwaitReply(MESSAGE, MC_REVERSE_IN, 500, String.class);
        assertEquals(MESSAGE_REVERSED, future.get());
    }

    @Test
    void requestAndForwardReplyShouldSucceed() throws Exception {
        testEndpoint.prepareForMessages(1);

        CompletableFuture<Void> future = requestReplyService.requestReply(MESSAGE, MC_REVERSE_IN, MC_SUCCESS_CHANNEL, 500);
        future.get(); // await request completion

        // check result
        testEndpoint.await();

        List<Message<?>> responses = testEndpoint.getReceivedMessages();
        assertEquals(1, responses.size());
        Message<?> msg = responses.get(0);
        assertEquals(MESSAGE_REVERSED, msg.getPayload());
    }
}
