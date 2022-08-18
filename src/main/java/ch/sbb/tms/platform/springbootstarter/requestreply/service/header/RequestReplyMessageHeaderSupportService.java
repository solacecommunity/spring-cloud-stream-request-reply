package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid.MessageCorrelationIdParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.destination.MessageDestinationParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto.MessageReplyToParser;

@Service
public class RequestReplyMessageHeaderSupportService {
    @Autowired
    private List<MessageCorrelationIdParser> correlationIdHeaderParsers;

    @Autowired
    private List<MessageDestinationParser> destinationHeaderParsers;

    @Autowired
    private List<MessageReplyToParser> replyToParsers;

    @Autowired
    private RequestReplyProperties requestReplyProperties;

    public @Nullable
    String getCorrelationId(Message<?> message) {
        return message == null ? null
                : correlationIdHeaderParsers
                .stream()
                .map(p -> p.getCorrelationId(message))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public @Nullable
    String getDestination(Message<?> message) {
        return message == null ? null
                : destinationHeaderParsers
                .stream()
                .map(p -> p.getDestination(message))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public @Nullable
    String getReplyTo(Message<?> message) {
        return message == null ? null
                : replyToParsers
                .stream()
                .map(p -> p.getReplyTo(message))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q> incoming message payload type
     * @param <A> outgoing message payload type
     * @param payloadFunction mapping function from incoming to outgoing payload
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    public <Q, A, T extends Function<Q, A>> Function<Message<Q>, Message<A>> wrap(T payloadFunction) {
        return request -> {
            MessageBuilder<A> mb = MessageBuilder.withPayload(payloadFunction.apply(request.getPayload()));
            transferAndAdoptHeaders(request, mb);
            return mb.build();
        };
    }

    private <Q, A> void transferAndAdoptHeaders(Message<Q> request, MessageBuilder<A> mb) {
        String correlationId = getCorrelationId(request);
        if (correlationId != null) {
            mb.setCorrelationId(correlationId);
        }

        String replyToDestination = getReplyTo(request);
        if (replyToDestination != null) {
            mb.setHeader(BinderHeaders.TARGET_DESTINATION, requestReplyProperties.replaceVariables(replyToDestination));
        }

        final List<String> headersToCopy = requestReplyProperties.getCopyHeadersOnWrap();
        mb.copyHeadersIfAbsent(request.getHeaders().entrySet().stream().filter(e -> headersToCopy.contains(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));
    }
}
