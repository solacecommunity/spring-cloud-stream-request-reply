package community.solace.spring.cloud.requestreply.service.header.parser.replyto;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageReplyToParser {
    @Nullable
    String getReplyTo(Message<?> message);
}
