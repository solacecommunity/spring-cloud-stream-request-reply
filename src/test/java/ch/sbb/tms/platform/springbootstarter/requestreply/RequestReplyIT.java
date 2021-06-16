/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply;

import static ch.sbb.tms.platform.springbootstarter.requestreply.integration.RequestReplyTestEndpoint.MC_REVERSE_IN;
import static ch.sbb.tms.platform.springbootstarter.requestreply.integration.RequestReplyTestEndpoint.MC_SUCCESS_CHANNEL;
import static ch.sbb.tms.platform.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties.Period;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;

class RequestReplyIT extends AbstractRequestReplyIT {
    private static final String MESSAGE = "Hier könnte Ihre Werbung stehen!";
    private static final String MESSAGE_REVERSED = "!nehets gnubreW erhI etnnök reiH";

    @Autowired
    private RequestReplyService requestReplyService;

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
        
        Period timeoutPeriod = new Period(timeoutInMs * 2, TimeUnit.MILLISECONDS);
        CompletableFuture<Void> future = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutPeriod);

        future.get(); // await completion
        assertEquals(uuid, ref.get());
    }

    @Test
    void resubmitRequestShouldThrowExceptionWhilstCompletingFirstRequest() throws Exception {
        Long timeoutInMs = 500l;

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

        Period timeoutPeriod = new Period(timeoutInMs * 2, TimeUnit.MILLISECONDS);
        CompletableFuture<Void> future1 = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutPeriod);
        assertThrows(IllegalArgumentException.class, () -> invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, exceptionalConsumer, timeoutPeriod));

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

        Period timeoutPeriod = new Period(timeoutInMs / 2, TimeUnit.MILLISECONDS);
        CompletableFuture<Void> future = invokeMethod(requestReplyService, "postRequest", //
                correlationId, requestRunnable, responseConsumer, timeoutPeriod);

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
        assertDoesNotThrow(() -> invokeMethod(requestReplyService, "onReplyReceived", msg));
    }

    @Test
    void requestAndAwaitReplyShouldSucceed() throws Exception {
        CompletableFuture<String> future = requestReplyService.requestAndAwaitReply(MESSAGE, MC_REVERSE_IN, String.class);
        assertEquals(MESSAGE_REVERSED, future.get());
    }

    @Test
    void requestAndForwardReplyShouldSucceed() throws Exception {
        testEndpoint.prepareForMessages(1);

        CompletableFuture<Void> future = requestReplyService.requestReply(MESSAGE, MC_REVERSE_IN, MC_SUCCESS_CHANNEL);
        future.get(); // await request completion

        // check result
        testEndpoint.await();

        List<Message<?>> responses = testEndpoint.getReceivedMessages();
        assertEquals(1, responses.size());
        Message<?> msg = responses.get(0);
        assertEquals(MESSAGE_REVERSED, msg.getPayload());
    }
}
