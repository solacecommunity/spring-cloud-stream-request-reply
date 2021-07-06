package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.destination;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageDestinationParser {
    @Nullable
    String getDestination(Message<?> message);
}
