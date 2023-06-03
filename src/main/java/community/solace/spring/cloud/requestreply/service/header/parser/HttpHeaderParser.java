package community.solace.spring.cloud.requestreply.service.header.parser;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpHeaderParser implements MessageHeaderCorrelationIdParser {
    public static final String HTTP_HEADER_CORRELATION_ID = "X-Correlation-ID";

    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return headers.get(HTTP_HEADER_CORRELATION_ID, String.class);
    }
}
