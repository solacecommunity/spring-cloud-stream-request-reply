/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2026.
 */

package community.solace.spring.cloud.requestreply.service.header.parser.replyindex;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderReplyIndexParser extends MessageReplyIndexParser {
    @Override
    default String getReplyIndex(Message<?> message) {
        return message == null ?
                null :
                getReplyIndex(message.getHeaders());
    }

    @Nullable
    String getReplyIndex(MessageHeaders headers);
}

