package community.solace.spring.cloud.requestreply.service.header.parser.destination;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderDestinationParser extends MessageDestinationParser {
    @Override
    default String getDestination(Message<?> message) {
        return message == null ?
                null :
                getDestination(message.getHeaders());
    }

    @Nullable
    String getDestination(MessageHeaders headers);
}
