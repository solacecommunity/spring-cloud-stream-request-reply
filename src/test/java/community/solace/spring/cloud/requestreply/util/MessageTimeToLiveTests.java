package community.solace.spring.cloud.requestreply.util;

import java.time.Duration;

import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Solace reads a time-to-live of 0 as "never expire", so a timeout below a millisecond must not round down to it.
 */
class MessageTimeToLiveTests {

    @Test
    void setTimeToLiveIfAbsent_expectOneMillisecond_whenTimeToLiveRoundsDownToZero() {
        MessageBuilder<String> messageBuilder = MessageBuilder.withPayload("demo");

        MessageTimeToLive.setTimeToLiveIfAbsent(messageBuilder, Duration.ofNanos(500_000));

        assertEquals(
                1L,
                messageBuilder.build().getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE, Long.class),
                "A time-to-live of 0 would let the message never expire"
        );
    }
}