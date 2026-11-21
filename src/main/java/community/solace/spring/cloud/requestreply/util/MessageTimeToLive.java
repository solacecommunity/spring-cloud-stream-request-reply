package community.solace.spring.cloud.requestreply.util;

import java.time.Duration;
import java.util.Map;

import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;

import org.springframework.integration.support.MessageBuilder;

/**
 * Gives a request or a reply the Solace time-to-live after which the broker discards it, so that it does not wait on
 * a queue long after the requester stopped waiting for it.
 */
public final class MessageTimeToLive {
    /**
     * Solace reads a time-to-live of 0 as "never expire", so a lifetime below a millisecond is written as this.
     */
    private static final Duration SHORTEST_TIME_TO_LIVE = Duration.ofMillis(1);
    /**
     * JCSMP rejects a longer time-to-live with an {@link IllegalArgumentException} when the message is sent.
     */
    private static final Duration LONGEST_TIME_TO_LIVE = Duration.ofDays(3650);

    private MessageTimeToLive() {
    }

    /**
     * Sets the time-to-live of the message, unless it already carries a lifetime of its own: a time-to-live, or an
     * expiration. A Solace message must never carry both.
     *
     * @param messageBuilder the message to set the time-to-live on
     * @param timeToLive     how long the message stays useful; written as at least one millisecond and at most ten
     *                       years, the bounds Solace accepts
     */
    public static void setTimeToLiveIfAbsent(MessageBuilder<?> messageBuilder, Duration timeToLive) {
        Map<String, Object> headers = messageBuilder.getHeaders();
        boolean carriesLifetime = headers.containsKey(SolaceHeaderParser.TIME_TO_LIVE) ||
                headers.containsKey(SolaceHeaderParser.EXPIRATION);
        if (carriesLifetime) {
            return;
        }

        messageBuilder.setHeader(SolaceHeaderParser.TIME_TO_LIVE, withinTheBoundsSolaceAccepts(timeToLive).toMillis());
    }

    private static Duration withinTheBoundsSolaceAccepts(Duration timeToLive) {
        if (timeToLive.compareTo(SHORTEST_TIME_TO_LIVE) < 0) {
            return SHORTEST_TIME_TO_LIVE;
        }
        if (timeToLive.compareTo(LONGEST_TIME_TO_LIVE) > 0) {
            return LONGEST_TIME_TO_LIVE;
        }
        return timeToLive;
    }
}