package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageReplyToParser {
    @Nullable
    String getReplyTo(Message<?> message);
}
