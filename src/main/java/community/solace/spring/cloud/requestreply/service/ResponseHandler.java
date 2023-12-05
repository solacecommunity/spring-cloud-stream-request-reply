package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ResponseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);

    private final CountDownLatch countDownLatch;
    private final AtomicLong expectedReplies = new AtomicLong(1);
    private final AtomicLong receivedReplies = new AtomicLong(0);
    private final boolean supportMultipleResponses;
    private final Instant requestTime;
    private final Timer timer;

    private final Consumer<Message<?>> responseMessageConsumer;

    private boolean isFirstMessage = true;
    private String errorMessage;

    public ResponseHandler(Consumer<Message<?>> responseMessageConsumer, boolean supportMultipleResponses, Timer timer) {
        this.countDownLatch = new CountDownLatch(1);
        this.responseMessageConsumer = responseMessageConsumer;
        this.supportMultipleResponses = supportMultipleResponses;

        this.requestTime = Instant.now();
        this.timer = timer;
    }

    public void receive(Message<?> message) {
        long remainingReplies = expectedReplies.get() - receivedReplies.incrementAndGet();
        if (remainingReplies >= 0) { // In case of unknown replies, the last message has no valid content.
            responseMessageConsumer.accept(message);
        }
        if (remainingReplies <= 0) { // Normally -1
            finished();
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
            // Set total messages to expect when a multi message on a first message.
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

        finished();
    }

    public void errorResponse(String errorMessage) {
        isFirstMessage = false;
        this.errorMessage = errorMessage;
        finished();
    }

    private void finished() {
        if (timer != null) {
            timer.record(Duration.between(requestTime, Instant.now()));
        }
        countDownLatch.countDown();
    }
}
