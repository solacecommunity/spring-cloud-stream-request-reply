package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

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

    public @Nullable String getCorrelationId(Message<?> message) {
        return message == null ? null //
                : correlationIdHeaderParsers //
                        .stream() //
                        .map(p -> p.getCorrelationId(message)) //
                        .filter(Objects::nonNull) //
                        .findFirst() //
                        .orElse(null) //
        ;
    }

    public @Nullable String getDestination(Message<?> message) {
        return message == null ? null //
                : destinationHeaderParsers //
                        .stream() //
                        .map(p -> p.getDestination(message)) //
                        .filter(Objects::nonNull) //
                        .findFirst() //
                        .orElse(null) //
        ;
    }

    public @Nullable String getReplyTo(Message<?> message) {
        return message == null ? null //
                : replyToParsers //
                        .stream() //
                        .map(p -> p.getReplyTo(message)) //
                        .filter(Objects::nonNull) //
                        .findFirst() //
                        .orElse(null) //
        ;
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
    public <Q, A> Function<Message<Q>, Message<A>> wrap(Function<Q, A> payloadFunction) {
        return request -> {
            MessageBuilder<A> mb = MessageBuilder.withPayload(payloadFunction.apply(request.getPayload()));
            transferAndAdoptHeaders(request, mb);
            return mb.build();
        };
    }

    private <Q, A> void transferAndAdoptHeaders(Message<Q> request, MessageBuilder<A> mb) {
        String correlationId = getCorrelationId(request);
        if (correlationId != null) mb.setCorrelationId(correlationId);

        String replyToDestination = getReplyTo(request);
        if (replyToDestination != null) mb.setHeader(BinderHeaders.TARGET_DESTINATION, replyToDestination);
    }

    /**
     * set the given message's headers so it is properly forwarded to the designated target channel
     * @param <T> message payload type
     * @param originalMessage the original message to create forward message from
     * @param targetDestination the target message channel name to direct the message at
     * @return a copy of the original message with the target header properly set
     */
    public final <T> Message<T> forwardMessage(Message<T> originalMessage, String targetDestination) {
        return MessageBuilder.fromMessage(originalMessage) //
                .setHeader(BinderHeaders.TARGET_DESTINATION, targetDestination) //
                .build() //
        ;
    }
}
