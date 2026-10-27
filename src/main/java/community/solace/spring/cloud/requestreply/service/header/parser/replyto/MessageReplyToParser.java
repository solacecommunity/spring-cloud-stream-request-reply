package community.solace.spring.cloud.requestreply.service.header.parser.replyto;

import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageReplyToParser {
    String getReplyTo(Message<?> message);
}
