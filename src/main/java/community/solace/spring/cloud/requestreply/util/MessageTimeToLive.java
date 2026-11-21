package community.solace.spring.cloud.requestreply.util;

import java.time.Duration;
import java.util.Map;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

/**
 * Gives a request or a reply the Solace time-to-live after which the broker discards it, so that it does not wait on
 * a queue long after the requester stopped waiting for it. The only place that reads and writes the Solace lifetime
 * headers.
 */
public final class MessageTimeToLive {
    /**
     * The Solace header with the milliseconds after which the broker discards the message; 0 means never.
     */
    public static final String TIME_TO_LIVE = "solace_timeToLive"/*SolaceHeaders.TIME_TO_LIVE*/;
    /**
     * The Solace header with the time at which the message expires; never set together with {@link #TIME_TO_LIVE}.
     */
    public static final String EXPIRATION = "solace_expiration"/*SolaceHeaders.EXPIRATION*/;

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
        boolean carriesLifetime = headers.containsKey(TIME_TO_LIVE) ||
                headers.containsKey(EXPIRATION);
        if (carriesLifetime) {
            return;
        }

        messageBuilder.setHeader(TIME_TO_LIVE, withinTheBoundsSolaceAccepts(timeToLive).toMillis());
    }

    /**
     * Gives a reply the time-to-live of its request, unless the reply already carries a lifetime of its own. Call it
     * after every other header of the reply is set, so that it sees a lifetime the caller set. A request that never
     * expires gets a reply without a time-to-live, which never expires either.
     *
     * @param request the request the reply answers
     * @param reply   the reply to set the time-to-live on
     */
    public static void copyTimeToLiveIfAbsent(Message<?> request, MessageBuilder<?> reply) {
        Object requestTimeToLive = request.getHeaders().get(TIME_TO_LIVE);
        if (!(requestTimeToLive instanceof Number millis) || millis.longValue() <= 0) {
            return;
        }

        setTimeToLiveIfAbsent(reply, Duration.ofMillis(millis.longValue()));
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