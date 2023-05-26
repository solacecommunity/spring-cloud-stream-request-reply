package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage.RemoteErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

public class ResponseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);

    private final CountDownLatch countDownLatch;
    private final AtomicLong expectedReplies = new AtomicLong(1);
    private final AtomicLong receivedReplies = new AtomicLong(0);
    private final boolean supportMultipleResponses;

    private final Consumer<Message<?>> responseMessageConsumer;

    private boolean isFirstMessage = true;
    private String errorMessage;

    public ResponseHandler(Consumer<Message<?>> responseMessageConsumer, boolean supportMultipleResponses) {
        this.countDownLatch = new CountDownLatch(1);
        this.responseMessageConsumer = responseMessageConsumer;
        this.supportMultipleResponses = supportMultipleResponses;
    }

    public void receive(Message<?> message) {
        long remainingReplies = expectedReplies.get() - receivedReplies.incrementAndGet();
        if (remainingReplies >= 0) { // In case of unknown replies, the last message has no valid content.
            responseMessageConsumer.accept(message);
        }
        if (remainingReplies <= 0) { // Normally -1
            countDownLatch.countDown();
        }

        LOG.debug("received response(remaining={}) {}", remainingReplies, message);
    }

    public void await() throws RemoteErrorException, InterruptedException {
        countDownLatch.await();
        if (StringUtils.hasText(errorMessage)) {
            throw new RemoteErrorException(errorMessage);
        }
    }

    public void setTotalReplies(Long totalReplies) {
        if (supportMultipleResponses && isFirstMessage && totalReplies > 1) {
            // Set total messages to expect when multi message on first message.
            expectedReplies.set(totalReplies);
            isFirstMessage = false;
        }
    }

    public void setUnknownReplies() {
        if (supportMultipleResponses) {
            expectedReplies.set(Long.MAX_VALUE);
        }
    }

    public void emptyResponse() {
        isFirstMessage = false;

        countDownLatch.countDown();
    }

    public void errorResponse(String errorMessage) {
        isFirstMessage = false;
        this.errorMessage = errorMessage;
        countDownLatch.countDown();
    }
}
