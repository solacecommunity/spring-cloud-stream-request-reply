package community.solace.spring.cloud.requestreply.util;

import java.time.Duration;
import java.util.Map;

import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;

import org.springframework.integration.support.MessageBuilder;

/**
 * Gives a request or a reply the Solace time-to-live after which the broker discards it, so that it does not
 * outlive the requester's wait for it.
 */
public final class MessageTimeToLive {
    /**
     * Solace reads a time-to-live of 0 as "never expire", so a lifetime that is already over is written as this.
     */
    private static final Duration SHORTEST_TIME_TO_LIVE = Duration.ofMillis(1);

    private MessageTimeToLive() {
    }

    /**
     * Sets the time-to-live of the message, unless it already carries a lifetime of its own: a time-to-live, or an
     * expiration, which the Solace binder rejects next to a time-to-live of a different value.
     *
     * @param messageBuilder the message to set the time-to-live on
     * @param timeToLive     how long the message stays useful; zero or less is written as the shortest time-to-live
     */
    public static void setTimeToLiveIfAbsent(MessageBuilder<?> messageBuilder, Duration timeToLive) {
        Map<String, Object> headers = messageBuilder.getHeaders();
        boolean carriesLifetime = headers.containsKey(SolaceHeaderParser.TIME_TO_LIVE) ||
                headers.containsKey(SolaceHeaderParser.EXPIRATION);
        if (carriesLifetime) {
            return;
        }

        Duration atLeastTheShortest = timeToLive.compareTo(SHORTEST_TIME_TO_LIVE) < 0 ? SHORTEST_TIME_TO_LIVE : timeToLive;
        messageBuilder.setHeader(SolaceHeaderParser.TIME_TO_LIVE, atLeastTheShortest.toMillis());
    }
}