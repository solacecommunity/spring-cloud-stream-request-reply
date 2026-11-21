package community.solace.spring.cloud.requestreply.service.header.parser;

import java.time.Duration;
import java.util.Optional;

import com.solacesystems.jcsmp.Destination;
import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageHeaderCorrelationIdParser;
import community.solace.spring.cloud.requestreply.service.header.parser.destination.MessageHeaderDestinationParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;
import community.solace.spring.cloud.requestreply.service.header.parser.timetolive.MessageHeaderTimeToLiveParser;

import org.springframework.messaging.MessageHeaders;

public class SolaceHeaderParser implements MessageHeaderCorrelationIdParser, MessageHeaderDestinationParser, MessageHeaderReplyToParser, MessageHeaderTimeToLiveParser {
    public static final String TIME_TO_LIVE = "solace_timeToLive"/*SolaceHeaders.TIME_TO_LIVE*/;
    public static final String EXPIRATION = "solace_expiration"/*SolaceHeaders.EXPIRATION*/;

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

    /**
     * Reads {@value #TIME_TO_LIVE}, which the Solace binder sets on every inbound message.
     *
     * @param headers the headers to read
     * @return the time-to-live of the message, or {@code null} if it has none or its time-to-live is 0, which
     * Solace reads as "never expire"
     */
    @Override
    public Duration getTimeToLive(MessageHeaders headers) {
        Object timeToLive = headers.get(TIME_TO_LIVE);
        if (!(timeToLive instanceof Number millis) || millis.longValue() <= 0) {
            return null;
        }
        return Duration.ofMillis(millis.longValue());
    }
}
