package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpHeaderParser implements MessageHeaderCorrelationIdParser {
    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return headers.get("X-Correlation-ID", String.class);
    }
}
