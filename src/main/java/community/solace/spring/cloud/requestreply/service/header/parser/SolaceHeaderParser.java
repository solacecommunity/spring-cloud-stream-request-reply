package community.solace.spring.cloud.requestreply.service.header.parser;

import java.util.Optional;

import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;
import community.solace.spring.cloud.requestreply.service.header.parser.destination.MessageHeaderDestinationParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;
import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solacesystems.jcsmp.Destination;

import org.springframework.messaging.MessageHeaders;

public class SolaceHeaderParser implements MessageHeaderCorrelationIdParser, MessageHeaderDestinationParser, MessageHeaderReplyToParser {
    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return headers.get(SolaceHeaders.CORRELATION_ID, String.class);
    }

    @Override
    public String getDestination(MessageHeaders headers) {
        return Optional.ofNullable(headers.get(SolaceHeaders.DESTINATION, Destination.class)).map(Destination::getName)
                .orElse(null);
    }

    @Override
    public String getReplyTo(MessageHeaders headers) {
        return Optional.ofNullable(headers.get(SolaceHeaders.REPLY_TO, Destination.class)).map(Destination::getName)
                .orElse(null);
    }
}
