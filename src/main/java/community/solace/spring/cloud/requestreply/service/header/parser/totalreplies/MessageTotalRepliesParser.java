package community.solace.spring.cloud.requestreply.service.header.parser.totalreplies;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageTotalRepliesParser {
    @Nullable
    Long getTotalReplies(Message<?> message);
}
