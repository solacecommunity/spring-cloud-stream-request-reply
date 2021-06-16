package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * a CorrelationIdParser is used to get the correlation id from a {@link Message}
 */
@FunctionalInterface
public interface MessageCorrelationIdParser {
    @Nullable
    String getCorrelationId(Message<?> message);
}
