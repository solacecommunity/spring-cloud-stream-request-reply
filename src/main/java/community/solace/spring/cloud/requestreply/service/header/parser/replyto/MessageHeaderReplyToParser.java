package community.solace.spring.cloud.requestreply.service.header.parser.replyto;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderReplyToParser extends MessageReplyToParser {
    @Override
    default String getReplyTo(Message<?> message) {
        return message == null ?
                null :
                getReplyTo(message.getHeaders());
    }

    String getReplyTo(MessageHeaders headers);
}
