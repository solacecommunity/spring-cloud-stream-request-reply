package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.destination.MessageHeaderDestinationParser;

@Service
@Order(30000)
public class BinderHeaderParser implements MessageHeaderDestinationParser {
    @Override
    public String getDestination(MessageHeaders headers) {
        return headers.get(BinderHeaders.TARGET_DESTINATION, String.class);
    }
}
