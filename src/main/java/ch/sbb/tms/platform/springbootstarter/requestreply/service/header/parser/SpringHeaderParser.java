package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;

@Service
@Order(40000)
public class SpringHeaderParser implements MessageHeaderReplyToParser {
    @Override
    public String getReplyTo(MessageHeaders headers) {
        return headers.get(MessageHeaders.REPLY_CHANNEL, String.class);
    }

}
