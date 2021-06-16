package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderReplyToParser extends MessageReplyToParser {
    @Override
    default String getReplyTo(Message<?> message) {
        return getReplyTo(message.getHeaders());
    }

    @Nullable
    String getReplyTo(MessageHeaders headers);
}
