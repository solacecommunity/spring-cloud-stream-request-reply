package community.solace.spring.cloud.requestreply.service.header.parser.timetolive;

import java.time.Duration;

import org.springframework.messaging.Message;

/**
 * Reads how long a message may live before the broker discards it.
 */
@FunctionalInterface
public interface MessageTimeToLiveParser {
    /**
     * Reads the time-to-live of a message.
     *
     * @param message the message to read
     * @return the time-to-live of the message, or {@code null} if it never expires or the parser does not know
     */
    Duration getTimeToLive(Message<?> message);
}