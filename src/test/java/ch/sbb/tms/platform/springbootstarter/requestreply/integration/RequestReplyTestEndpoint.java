/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.integration;


import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@MessageEndpoint
public class RequestReplyTestEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyTestEndpoint.class);
    private static final long TEST_TIMEOUT_SECONDS = 5;

    /**
     * message channel name for delayer
     */
    public static final String MC_DELAY_MESSAGE = "delayChannel";

    /**
     * error channel name
     */
    public static final String MC_ERROR_CHANNEL = "errorChannel";

    /**
     * success channel name
     */
    public static final String MC_SUCCESS_CHANNEL = "successChannel";
    public static final String MC_ROUTER = "routerChannel";
    public static final String MC_REVERSE_IN = "reverseInChannel";
    public static final String MC_REVERSE_OUT = "reverseOutChannel";
    public static final String MC_REPLIES = "replies";

    private int numExpectedMessages;

    private CountDownLatch messageCountDownLatch;

    private List<Message<?>> receivedMessages = new CopyOnWriteArrayList<>();
    private List<Message<Exception>> receivedErrorMessages = new CopyOnWriteArrayList<>();

    @Autowired
    @Qualifier("reverse")
    private Function<Message<String>, Message<String>> reverseFunction;

    @Autowired
    private RequestReplyService requestReplyService;

    @ServiceActivator(inputChannel = MC_ERROR_CHANNEL)
    public void receiveError(Message<Exception> message) {
        LOG.error("received error message", message.getPayload());
        if (messageCountDownLatch == null) throw new IllegalStateException("Countdown latch not initialized");
        receivedErrorMessages.add(message);
        messageCountDownLatch.countDown();
    }

    @ServiceActivator(inputChannel = MC_SUCCESS_CHANNEL)
    public void receiveMessage(Message<?> message) {
        LOG.info("received message", message.getPayload());
        if (messageCountDownLatch == null) throw new IllegalStateException("Countdown latch not initialized");
        receivedMessages.add(message);
        messageCountDownLatch.countDown();
    }

    @ServiceActivator(inputChannel = MC_REPLIES)
    public void reply(Message<?> message) {
        invokeMethod(requestReplyService, "onReplyReceived", message);
    }

    @Router(inputChannel = MC_ROUTER)
    public String route(Message<?> message) {
        return (String) message.getHeaders().getOrDefault(BinderHeaders.TARGET_DESTINATION, MC_ERROR_CHANNEL);
    }

    @Transformer(inputChannel = MC_REVERSE_IN, outputChannel = MC_ROUTER)
    public Message<String> reverse(Message<String> input) {
        return reverseFunction.apply(input);
    }

    public synchronized void prepareForMessages(int numMessages) {
        Assert.isTrue(numMessages > 0, "number of messages must be positive");
        if (this.messageCountDownLatch != null) throw new IllegalStateException("Countdown latch already initialized");
        this.messageCountDownLatch = new CountDownLatch(numMessages);
    }

    public synchronized void await() throws InterruptedException {
        try {
            messageCountDownLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (messageCountDownLatch.getCount() > 0) throw new AssertionError(String.format("Received %s out of %s expected messages",
                    numExpectedMessages - messageCountDownLatch.getCount(), numExpectedMessages));
        }
        catch (InterruptedException iex) {
            reset();
            throw iex;
        }
    }

    public synchronized void reset() {
        messageCountDownLatch = null;
        numExpectedMessages = 0;
        receivedErrorMessages.clear();
        receivedMessages.clear();
    }

    public boolean hasExceptions() {
        return !receivedErrorMessages.isEmpty();
    }

    public List<Message<Exception>> getReceivedErrorMessages() {
        return Collections.unmodifiableList(receivedErrorMessages);
    }

    public List<Message<?>> getReceivedMessages() {
        return Collections.unmodifiableList(receivedMessages);
    }
}
