/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2026.
 */

package community.solace.spring.cloud.requestreply.service.header.parser.replyindex;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageReplyIndexParser {
    @Nullable
    String getReplyIndex(Message<?> message);
}

