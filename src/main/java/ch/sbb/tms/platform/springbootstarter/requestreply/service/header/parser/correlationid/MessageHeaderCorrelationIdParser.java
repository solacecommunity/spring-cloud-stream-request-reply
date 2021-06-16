package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

/**
 * a CorrelationIdHeaderParser retrieves a correlation id from {@link Message}'s header
 */
@FunctionalInterface
public interface MessageHeaderCorrelationIdParser extends MessageCorrelationIdParser {
    @Override
    default String getCorrelationId(Message<?> message) {
        return message == null ? null : getCorrelationId(message.getHeaders());
    }

    @Nullable
    String getCorrelationId(MessageHeaders headers);
}
