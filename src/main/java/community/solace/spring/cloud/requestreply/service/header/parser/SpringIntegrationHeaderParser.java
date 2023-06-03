package community.solace.spring.cloud.requestreply.service.header.parser;

import java.util.Optional;

import org.springframework.core.annotation.Order;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;

@Service
@Order(20000)
public class SpringIntegrationHeaderParser implements MessageHeaderCorrelationIdParser {

    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return Optional.ofNullable(headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID)).map(Object::toString).orElse(null);
    }
}
