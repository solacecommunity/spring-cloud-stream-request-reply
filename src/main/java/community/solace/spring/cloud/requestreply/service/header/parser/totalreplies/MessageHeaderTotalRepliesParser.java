package community.solace.spring.cloud.requestreply.service.header.parser.totalreplies;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderTotalRepliesParser extends MessageTotalRepliesParser {
    @Override
    default Long getTotalReplies(Message<?> message) {
        return message == null ?
                null :
                getTotalReplies(message.getHeaders());
    }

    @Nullable
    Long getTotalReplies(MessageHeaders headers);
}
