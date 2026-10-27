package community.solace.spring.cloud.requestreply.service.header.parser.destination;

import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageDestinationParser {
    String getDestination(Message<?> message);
}
