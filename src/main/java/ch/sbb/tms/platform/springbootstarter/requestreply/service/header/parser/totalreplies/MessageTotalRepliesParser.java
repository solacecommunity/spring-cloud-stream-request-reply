package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.totalreplies;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageTotalRepliesParser {
    @Nullable
    Integer getTotalReplies(Message<?> message);
}