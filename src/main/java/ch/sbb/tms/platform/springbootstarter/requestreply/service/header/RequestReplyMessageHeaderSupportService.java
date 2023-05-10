package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SpringHeaderParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.correlationid.MessageCorrelationIdParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.destination.MessageDestinationParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage.MessageErrorMessageParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto.MessageReplyToParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.totalreplies.MessageTotalRepliesParser;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class RequestReplyMessageHeaderSupportService {
    @Autowired
    private List<MessageCorrelationIdParser> correlationIdHeaderParsers;

    @Autowired
    private List<MessageDestinationParser> destinationHeaderParsers;

    @Autowired
    private List<MessageReplyToParser> replyToParsers;

    @Autowired
    private List<MessageTotalRepliesParser> totalRepliesParsers;

    @Autowired
    private List<MessageErrorMessageParser> errorMessageParsers;

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

    public @Nullable
    Long getTotalReplies(Message<?> message) {
        return message == null ? null
                : totalRepliesParsers
                .stream()
                .map(p -> p.getTotalReplies(message))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public @Nullable
    String getErrorMessage(Message<?> message) {
        return message == null ? null
                : errorMessageParsers
                .stream()
                .map(p -> p.getErrorMessage(message))
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
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    public final <Q, A, T extends Function<Q, A>, E extends Throwable> Function<Message<Q>, Message<A>> wrap(T payloadFunction, Class<E>... applicationExceptions) {
        return request -> {
            try {
                MessageBuilder<A> mb = MessageBuilder.withPayload(payloadFunction.apply(request.getPayload()));
                transferAndAdoptHeaders(request, mb);
                return mb.build();
            }
            catch (Exception e) {
                if (applicationExceptions != null) {
                    for (Class<E> applicationException : applicationExceptions) {
                        if (applicationException.isInstance(e)) {
                            return errorResponse(request, e);
                        }
                    }
                }
                throw e;
            }
        };
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q> incoming message payload type
     * @param <A> outgoing message payload type
     * @param payloadFunction mapping function from incoming to outgoing payload
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    @SuppressWarnings("unchecked" )
    public final <Q, A, T extends Function<Q, List<A>>, E extends Throwable> Function<Message<Q>, List<Message<A>>> wrapList(T payloadFunction, Class<E>... applicationExceptions) {
        return request -> {
            try {
                List<A> rawResponses = payloadFunction.apply(request.getPayload());

                List<Message<A>> response = new ArrayList<>();

                if (CollectionUtils.isEmpty(rawResponses)) {
                    response.add(emptyMsg(request, 0, 0));
                }
                else {
                    for (int i = 0; i < rawResponses.size(); i++) {
                        MessageBuilder<A> mb = MessageBuilder.withPayload(rawResponses.get(i));
                        transferAndAdoptHeaders(request, mb, rawResponses.size(), i);
                        response.add(mb.build());
                    }
                }

                return response;
            }
            catch (Exception e) {
                if (applicationExceptions != null) {
                    for (Class<E> applicationException : applicationExceptions) {
                        if (applicationException.isInstance(e)) {
                            return List.of(errorResponse(request, e));
                        }
                    }
                }
                throw e;
            }
        };
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
    @SuppressWarnings("unchecked" )
    public final <Q, A> Function<Flux<Message<Q>>, Flux<Message<A>>> wrapFlux(BiConsumer<Q, FluxSink<A>> payloadFunction) {
        return inFlux -> inFlux
                .flatMap(request -> {
                    try {
                        AtomicLong index = new AtomicLong(0);
                        return Flux
                                .create(fluxSink -> payloadFunction.accept(request.getPayload(), (FluxSink<A>) fluxSink))
                                .map(payload -> {
                                    MessageBuilder<A> mb = (MessageBuilder<A>) MessageBuilder.withPayload(payload);
                                    transferAndAdoptHeaders(request, mb, -1, index.getAndIncrement());
                                    return mb.build();
                                })
                                .concatWith(Mono.fromSupplier(() -> emptyMsg(request, index.get(), index.get()))) // Append the finish message
                                .onErrorResume(err -> Mono.just(errorResponse(request, err)));
                    }
                    catch (Exception e) {
                        return Flux.error(e);
                    }
                });
    }

    private <Q, A> Message<A> emptyMsg(Message<Q> request, long totalReplies, long replyIndex) {
        MessageBuilder<String> mb = MessageBuilder.withPayload("" );
        transferAndAdoptHeaders(request, mb, totalReplies, replyIndex);
        return (Message<A>) mb.build();
    }

    @NotNull
    @SuppressWarnings("unchecked" )
    private <Q, A> Message<A> errorResponse(Message<Q> request, Throwable e) {
        MessageBuilder<String> mb = MessageBuilder.withPayload("" );
        transferAndAdoptHeaders(request, mb, 0, 0);
        mb.setHeader(SpringHeaderParser.ERROR_MESSAGE, e.getMessage());
        return (Message<A>) mb.build();
    }

    private <Q, A> void transferAndAdoptHeaders(Message<Q> request, MessageBuilder<A> mb, long totalReplies, long replyIndex) {
        transferAndAdoptHeaders(request, mb);

        mb.setHeader(SpringHeaderParser.MULTI_TOTAL_REPLIES, totalReplies);
        mb.setHeader(SpringHeaderParser.MULTI_REPLY_INDEX, replyIndex);
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

        MessageHeaders requestHeaders = request.getHeaders();
        for (String headerToCopy : requestReplyProperties.getCopyHeadersOnWrap()) {
            Object val = requestHeaders.get(headerToCopy);

            if (val != null) {
                mb.setHeaderIfAbsent(headerToCopy, val);
            }
        }
    }
}
