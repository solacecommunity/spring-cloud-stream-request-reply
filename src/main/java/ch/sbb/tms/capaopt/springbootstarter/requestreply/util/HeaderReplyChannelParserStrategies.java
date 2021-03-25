package ch.sbb.tms.capaopt.springbootstarter.requestreply.util;

import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solacesystems.jcsmp.Destination;

public enum HeaderReplyChannelParserStrategies {
    SOLACE(headers -> Optional.ofNullable(headers.get(SolaceHeaders.REPLY_TO, Destination.class)).map(Destination::getName)
            .orElseGet(() -> null)), //
    CHANNEL(headers -> headers.get(MessageHeaders.REPLY_CHANNEL, String.class)) //
    ;

    static final Logger LOG = LoggerFactory.getLogger(HeaderReplyChannelParserStrategies.class);

    private Function<MessageHeaders, String> extractor;

    HeaderReplyChannelParserStrategies(Function<MessageHeaders, String> extractor) {
        this.extractor = extractor;
    }

    public static final String retrieve(MessageHeaders messageHeaders) {
        if (messageHeaders != null) {
            for (HeaderReplyChannelParserStrategies strategy : HeaderReplyChannelParserStrategies.values()) {
                String replyTo = strategy.extractor.apply(messageHeaders);
                if (replyTo != null) {
                    LOG.trace("Retrieved reply to channel '{}' from {}", replyTo, strategy);
                    return replyTo;
                }
            }
        }

        return null;
    }
}