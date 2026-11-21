package community.solace.spring.cloud.requestreply.service.header.parser.timetolive;

import java.time.Duration;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * Reads the time-to-live of a message from its headers.
 */
@FunctionalInterface
public interface MessageHeaderTimeToLiveParser extends MessageTimeToLiveParser {
    @Override
    default Duration getTimeToLive(Message<?> message) {
        return message == null ?
                null :
                getTimeToLive(message.getHeaders());
    }

    /**
     * Reads the time-to-live from the headers of a message.
     *
     * @param headers the headers to read
     * @return the time-to-live of the message, or {@code null} if it never expires or the parser does not know
     */
    Duration getTimeToLive(MessageHeaders headers);
}