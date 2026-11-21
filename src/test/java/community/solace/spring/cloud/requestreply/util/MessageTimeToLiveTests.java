package community.solace.spring.cloud.requestreply.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A timeout becomes the Solace time-to-live of a request, so it has to fit the bounds Solace accepts: 0 means
 * "never expire", and JCSMP refuses to send a message whose time-to-live is longer than ten years.
 */
class MessageTimeToLiveTests {
    private static final long TEN_YEARS_IN_MILLIS = 315_360_000_000L;

    @Test
    void setTimeToLiveIfAbsent_expectOneMillisecond_whenTimeToLiveRoundsDownToZero() {
        MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("demo");

        MessageTimeToLive.setTimeToLiveIfAbsent(messageBuilder, Duration.ofNanos(500_000));

        assertEquals(
                1L,
                messageBuilder.build().getHeaders().get(MessageTimeToLive.TIME_TO_LIVE, Long.class),
                "A time-to-live of 0 would let the message never expire"
        );
    }

    @Test
    void setTimeToLiveIfAbsent_expectTenYears_whenTheCallerWaitsForever() {
        MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("demo");

        MessageTimeToLive.setTimeToLiveIfAbsent(messageBuilder, ChronoUnit.FOREVER.getDuration());

        assertEquals(
                TEN_YEARS_IN_MILLIS,
                messageBuilder.build().getHeaders().get(MessageTimeToLive.TIME_TO_LIVE, Long.class),
                "JCSMP throws an IllegalArgumentException for a longer time-to-live, so the request would not be sent"
        );
    }
}