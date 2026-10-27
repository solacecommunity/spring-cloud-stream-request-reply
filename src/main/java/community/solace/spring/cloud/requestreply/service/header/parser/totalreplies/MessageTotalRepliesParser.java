package community.solace.spring.cloud.requestreply.service.header.parser.totalreplies;

import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageTotalRepliesParser {
    Long getTotalReplies(Message<?> message);
}
