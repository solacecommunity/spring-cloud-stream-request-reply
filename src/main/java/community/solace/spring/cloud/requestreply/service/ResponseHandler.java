package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import community.solace.spring.cloud.requestreply.service.logging.RequestReplyLogger;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.BitSet;
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

    private final RequestReplyLogger requestReplyLogger;

    /**
     * Fast-path dedup store for replyIndex values when totalReplies is known.
     *
     * <p>We store numeric indices (e.g. "12") and numeric ranges (e.g. "0-15") inside a BitSet.
     * This is designed for very large reply counts without excessive allocation pressure.</p>
     *
     * <p>Protected by synchronizing on {@code numericReplyIndexBitSetLock} because BitSet is not thread-safe.</p>
     */
    private volatile BitSet numericReplyIndexBitSet;
    private volatile int numericReplyIndexBitSetSize = -1;
    private final Object numericReplyIndexBitSetLock = new Object();

    // Grow limit for unknown-size / streaming cases to avoid unbounded memory use on malformed indices.
    // For known totalReplies, numericReplyIndexBitSetSize will cap growth.
    private static final int MAX_DEDUP_BITS_WHEN_UNKNOWN = Integer.getInteger(
            "spring.cloud.stream.requestreply.dedup.maxBitsWhenUnknown",
            100_000
    );

    public ResponseHandler(Consumer<Message<?>> responseMessageConsumer, boolean supportMultipleResponses, Timer timer, RequestReplyLogger requestReplyLogger) {
        this.countDownLatch = new CountDownLatch(1);
        this.responseMessageConsumer = responseMessageConsumer;
        this.supportMultipleResponses = supportMultipleResponses;

        this.requestTime = Instant.now();
        this.timer = timer;
        this.requestReplyLogger = requestReplyLogger;
    }

    public void receive(Message<?> message) {
        long remainingReplies = expectedReplies.get() - receivedReplies.incrementAndGet();
        if (remainingReplies >= 0) { // In case of unknown replies, the last message has no valid content.
            responseMessageConsumer.accept(message);
        }
        if (remainingReplies <= 0) { // Normally -1
            finished();
        }

        requestReplyLogger.logReply(LOG, Level.DEBUG, "received response(remaining={}) {}", remainingReplies, message);
    }


    public boolean checkDuplicate(String replyIndex) {
        if (replyIndex == null) {
            return false;
        }

        // Support both "n" and "start-end" (range dedup is by start only).
        Integer start = tryParseNonNegativeReplyIndexStart(replyIndex);
        if (start == null) {
            // Keep runtime fast: ignore non-numeric indices rather than allocating fallback structures.
            requestReplyLogger.log(LOG, Level.DEBUG, "replyIndex '{}' is not numeric; skipping dedup", replyIndex);
            return false;
        }

        synchronized (numericReplyIndexBitSetLock) {
            // Lazily allocate & grow BitSet.
            if (numericReplyIndexBitSet == null) {
                numericReplyIndexBitSet = new BitSet(Math.min(1024, Math.max(start + 1, 0)));
            }

            // If totalReplies is known, clamp to [0, total-1].
            if (numericReplyIndexBitSetSize > 0) {
                if (start >= numericReplyIndexBitSetSize) {
                    return false;
                }
            } else {
                // Unknown totalReplies (streaming): cap growth to avoid unbounded memory.
                if (start >= MAX_DEDUP_BITS_WHEN_UNKNOWN) {
                    return false;
                }
            }

            // Dedup by start index only.
            if (numericReplyIndexBitSet.get(start)) {
                requestReplyLogger.log(LOG, Level.WARN, "received duplicate response(index={})", replyIndex);
                return true;
            }

            numericReplyIndexBitSet.set(start);
            return false;
        }
    }

    public void await() throws RemoteErrorException, InterruptedException {
        countDownLatch.await();
        if (StringUtils.hasText(errorMessage)) {
            throw new RemoteErrorException(errorMessage);
        }
    }

    public void setTotalReplies(Long totalReplies) {
        if (supportMultipleResponses && isFirstMessage && totalReplies >= 1) {
            // Set total messages to expect when a multi message on a first message.
            expectedReplies.set(totalReplies);
            isFirstMessage = false;

            // If totalReplies is known and within Integer range, enable bounded numeric dedup.
            if (totalReplies <= Integer.MAX_VALUE) {
                numericReplyIndexBitSetSize = totalReplies.intValue();
                // Don't eagerly allocate; it might never be needed if replyIndex isn't present.
            }
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


    private static Integer tryParseNonNegativeReplyIndexStart(String text) {
        // Accept:
        // - "123" -> 123
        // - "12-34" -> 12 (start)
        // Reject:
        // - "-1", "12-", "-12", "12-34-56", "", non-digits
        int len = text.length();
        if (len == 0) {
            return null;
        }

        int value = 0;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if (c == '-') {
                // must not be leading or trailing
                return (i == 0 || i == len - 1) ? null : value;
            }

            if (c < '0' || c > '9') {
                return null;
            }

            value = value * 10 + (c - '0');
            if (value < 0) {
                return null;
            }
        }

        return value;
    }

    private void finished() {
        // Clear per-request dedup bookkeeping to avoid retaining replyIndex values
        // longer than necessary (success, error, or timeout/abort paths all call finished()).
        numericReplyIndexBitSet = null;
        numericReplyIndexBitSetSize = -1;

        if (timer != null) {
            timer.record(Duration.between(requestTime, Instant.now()));
        }
        countDownLatch.countDown();
    }

    public void abort() {
        finished();
    }
}
