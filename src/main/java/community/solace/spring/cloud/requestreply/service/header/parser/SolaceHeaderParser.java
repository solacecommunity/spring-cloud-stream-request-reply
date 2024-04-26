package community.solace.spring.cloud.requestreply.service.header.parser;

import com.solacesystems.jcsmp.Destination;
import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;
import community.solace.spring.cloud.requestreply.service.header.parser.destination.MessageHeaderDestinationParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;
import org.springframework.messaging.MessageHeaders;

import java.util.Optional;

public class SolaceHeaderParser implements MessageHeaderCorrelationIdParser, MessageHeaderDestinationParser, MessageHeaderReplyToParser {
    @Override
    public String getCorrelationId(MessageHeaders headers) {
        return headers.get("solace_correlationId"/*SolaceHeaders.CORRELATION_ID*/, String.class);
    }

    @Override
    public String getDestination(MessageHeaders headers) {
        return Optional.ofNullable(headers.get("solace_destination"/*SolaceHeaders.DESTINATION*/, Destination.class)).map(Destination::getName).orElse(null);
    }

    @Override
    public String getReplyTo(MessageHeaders headers) {
        return Optional.ofNullable(headers.get("solace_replyTo"/*SolaceHeaders.REPLY_TO*/, Destination.class)).map(Destination::getName).orElse(null);
    }
}
