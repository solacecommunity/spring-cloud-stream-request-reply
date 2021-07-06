package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.core.annotation.Order;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;

@Service
@Order(20000)
public class SpringIntegrationHeaderParser implements MessageHeaderCorrelationIdParser {

    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID, String.class);
    }

}
