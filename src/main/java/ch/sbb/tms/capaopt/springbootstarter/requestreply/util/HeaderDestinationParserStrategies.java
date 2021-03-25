package ch.sbb.tms.capaopt.springbootstarter.requestreply.util;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solacesystems.jcsmp.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.messaging.MessageHeaders;

import java.util.Optional;
import java.util.function.Function;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.util.MessagingUtil.SCS_SEND_TO_DESTINATION;

public enum HeaderDestinationParserStrategies {
    SOLACE(headers -> Optional.ofNullable(headers.get(SolaceHeaders.DESTINATION, Destination.class)).map(Destination::getName)
            .orElse(null)), //
    SCS(headers -> headers.get(SCS_SEND_TO_DESTINATION,
            String.class)),
    BINDER(headers -> headers.get(BinderHeaders.TARGET_DESTINATION, String.class)), //
    ;

    static final Logger LOG = LoggerFactory.getLogger(HeaderCorrelationIdParserStrategies.class);

    private Function<MessageHeaders, String> extractor;

    HeaderDestinationParserStrategies(Function<MessageHeaders, String> extractor) {
        this.extractor = extractor;
    }

    public static final String retrieve(MessageHeaders messageHeaders) {
        if (messageHeaders != null) {
            for (HeaderDestinationParserStrategies strategy : HeaderDestinationParserStrategies.values()) {
                String destination = strategy.extractor.apply(messageHeaders);
                if (destination != null) {
                    LOG.trace("Retrieved reply to channel '{}' from {}", destination, strategy);
                    return destination;
                }
            }
        }

        return null;
    }
}