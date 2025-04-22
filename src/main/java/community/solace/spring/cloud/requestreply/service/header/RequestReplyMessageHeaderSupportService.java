package community.solace.spring.cloud.requestreply.service.header;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import community.solace.spring.cloud.requestreply.config.RequestReplyProperties;
import community.solace.spring.cloud.requestreply.service.MessageConverter;
import community.solace.spring.cloud.requestreply.service.header.parser.SpringHeaderParser;
import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageCorrelationIdParser;
import community.solace.spring.cloud.requestreply.service.header.parser.destination.MessageDestinationParser;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.MessageErrorMessageParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageReplyToParser;
import community.solace.spring.cloud.requestreply.service.header.parser.totalreplies.MessageTotalRepliesParser;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import community.solace.spring.cloud.requestreply.util.MessageChunker;
import org.apache.commons.lang3.tuple.Pair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;

@Service
public class RequestReplyMessageHeaderSupportService {

    private final static int MAX_MSG_PER_CHUNK = 10_000;
    private final static int ONE_MB = 1_000_000;

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
    @Autowired
    private MessageConverter messageConverter;
    @Autowired
    private BindingServiceProperties bindingServiceProperties;
    @Autowired
    private ReplyWrappingInterceptor replyWrappingInterceptor;

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
     * @param <Q>                   incoming message payload type
     * @param <A>                   outgoing message payload type
     * @param payloadFunction       mapping function from incoming to outgoing payload
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    public final <Q, A, T extends ThrowingFunction<Q, A>> Function<Message<Q>, Message<A>> wrap(T payloadFunction,
                                                                                                             Class<? extends Throwable>... applicationExceptions) {
        return wrapWithBindingName(payloadFunction, (String) null, new HashMap<>(), applicationExceptions);
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q>                   incoming message payload type
     * @param <A>                   outgoing message payload type
     * @param payloadFunction       mapping function from incoming to outgoing payload
     * @param additionalHeaders     additional headers to be added to the response message
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    public final <Q, A, T extends ThrowingFunction<Q, A>> Function<Message<Q>, Message<A>> wrap(
            T payloadFunction,
            Map<String, Object> additionalHeaders,
            Class<? extends Throwable>... applicationExceptions) {
        return wrapWithBindingName(payloadFunction, null, additionalHeaders, applicationExceptions);
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q>                   incoming message payload type
     * @param <A>                   outgoing message payload type
     * @param payloadFunction       mapping function from incoming to outgoing payload
     * @param bindingName           the name of the output binding. Required to have reply header configurable in binding settings, otherwise you may pass null.
     * @param additionalHeaders     additional headers to be added to the response message
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    public final <Q, A, T extends ThrowingFunction<Q, A>> Function<Message<Q>, Message<A>> wrapWithBindingName(T payloadFunction,
                                                                                                                            String bindingName,
                                                                                                                            Map<String, Object> additionalHeaders,
                                                                                                                            Class<? extends Throwable>... applicationExceptions) {
        return request -> {
            try {
                MessageBuilder<A> mb = MessageBuilder.withPayload(payloadFunction.apply(request.getPayload()));
                transferAndAdoptHeaders(request, mb);
                if (additionalHeaders != null) {
                    for (var header : additionalHeaders.entrySet()) {
                        mb.setHeader(header.getKey(), header.getValue());
                    }
                }
                return this.replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(mb.build(), bindingName);
            }
            catch (Exception e) {
                if (applicationExceptions != null) {
                    for (Class<? extends Throwable> applicationException : applicationExceptions) {
                        if (applicationException.isInstance(e)) {
                            return errorResponse(request, e, bindingName);
                        }
                    }
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q>                   incoming message payload type
     * @param <A>                   outgoing message payload type
     * @param payloadFunction       mapping function from incoming to outgoing payload
     * @param bindingName           the name of the output binding. Required to get configured content type, to encode message.
     * @param applicationExceptions A list of exceptions that will return the error to the requestor
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    @SafeVarargs
    public final <Q, A, T extends Function<Q, List<A>>, E extends Throwable> Function<Message<Q>, List<Message<A>>> wrapList(T payloadFunction, String bindingName, Class<E>... applicationExceptions) {
        return request -> {
            try {
                List<A> rawResponses = payloadFunction.apply(request.getPayload());

                if (CollectionUtils.isEmpty(rawResponses)) {
                    return interceptResponses(bindingName, List.of(emptyMsg(request, 0, 0, bindingName)));
                } else {

                    if (Boolean.TRUE.equals(request.getHeaders()
                                                   .get(SpringHeaderParser.GROUPED_MESSAGES)) && StringUtils.hasText(bindingName)) {
                        return interceptResponses(bindingName, wrapListGroupedResponses(request, rawResponses, getContentType(bindingName), bindingName));
                    } else {
                        return wrapListSingleResponses(request, rawResponses, bindingName);
                    }
                }
            }
            catch (Exception e) {
                if (applicationExceptions != null) {
                    for (Class<E> applicationException : applicationExceptions) {
                        if (applicationException.isInstance(e)) {
                            return List.of(errorResponse(request, e, bindingName));
                        }
                    }
                }
                throw e;
            }
        };
    }

    private <A> List<Message<A>> interceptResponses(String bindingName, List<Message<A>> messages) {
        return messages.stream()
                       .map(message -> replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(message, bindingName))
                       .toList();
    }

    private <A, Q> List<Message<A>> wrapListSingleResponses(Message<Q> request, List<A> rawResponses, String bindingName) {
        List<Message<A>> response = new ArrayList<>();
        for (int i = 0; i < rawResponses.size(); i++) {
            MessageBuilder<A> mb = MessageBuilder.withPayload(rawResponses.get(i));
            transferAndAdoptHeaders(request, mb, rawResponses.size(), "" + i);
            response.add(replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(mb.build(), bindingName));
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private <A, Q> List<Message<A>> wrapListGroupedResponses(Message<Q> request, List<A> rawResponses, MimeType outputContentType, String bindingName) {
        List<Message<byte[]>> byteMessages = new ArrayList<>();
        for (int i = 0; i < rawResponses.size(); i++) {
            Message<?> responseAsByteMsg = messageConverter.convertMessageToBytesIfNecessary(
                    MessageBuilder
                            .withPayload(rawResponses.get(i))
                            .build(),
                    outputContentType.toString()
            );

            if (!(responseAsByteMsg.getPayload() instanceof byte[])) {
                // A message could not be converted to byte[]:
                return wrapListSingleResponses(request, rawResponses, bindingName);
            }

            byteMessages.add((Message<byte[]>) responseAsByteMsg);
        }

        AtomicLong index = new AtomicLong(0);
        List<Message<A>> response = new ArrayList<>();
        for (Pair<Message<byte[]>, Integer> oneMbChunk : MessageChunker.mapChunked(byteMessages, ONE_MB)) {
            MessageBuilder<A> mb = (MessageBuilder<A>) MessageBuilder.fromMessage(oneMbChunk.getKey());

            String indexRange = index.get() + "-" + (index.addAndGet(oneMbChunk.getValue()) - 1);
            transferAndAdoptHeaders(request, mb, byteMessages.size(), indexRange);
            response.add(this.replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(mb.build(), bindingName));
        }

        return response;
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q>             incoming message payload type
     * @param <A>             outgoing message payload type
     * @param payloadFunction mapping function from incoming to outgoing payload
     * @param bindingName     the name of the output binding. Required to get configured content type, to encode message.
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    public final <Q, A> Function<Flux<Message<Q>>, Flux<Message<A>>> wrapFlux(BiConsumer<Q, FluxSink<A>> payloadFunction, String bindingName) {
        return wrapFlux(payloadFunction, bindingName, Duration.ofMillis(200));
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     *
     * @param <Q>             incoming message payload type
     * @param <A>             outgoing message payload type
     * @param payloadFunction mapping function from incoming to outgoing payload
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    public final <Q, A> Function<Flux<Message<Q>>, Flux<Message<A>>> wrapFlux(BiConsumer<Q, FluxSink<A>> payloadFunction, String bindingName, Duration groupTimeout) {
        return inFlux -> inFlux
                .flatMap(request -> {
                    try {
                        Flux<A> responses = Flux.create(fluxSink -> payloadFunction.accept(request.getPayload(), fluxSink));

                        if (Boolean.TRUE.equals(request.getHeaders()
                                                       .get(SpringHeaderParser.GROUPED_MESSAGES)) && StringUtils.hasText(bindingName)) {
                            return wrapFluxGroupedResponses(request, responses, getContentType(bindingName), groupTimeout, bindingName);
                        } else {
                            return wrapFluxSingleResponses(request, responses, bindingName);
                        }
                    }
                    catch (Exception e) {
                        return Flux.error(e);
                    }
                });
    }

    private <Q, A> Flux<Message<A>> wrapFluxSingleResponses(Message<Q> request, Flux<A> responses, String bindingName) {
        AtomicLong index = new AtomicLong(0);
        return responses
                .map(payload -> {
                    MessageBuilder<A> mb = MessageBuilder.withPayload(payload);
                    transferAndAdoptHeaders(request, mb, -1, "" + index.getAndIncrement());
                    return this.replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(mb.build(), bindingName);
                })
                .concatWith(Mono.fromSupplier(() -> emptyMsg(request, index.get(), index.get(), bindingName))) // Append the finish message
                .onErrorResume(err -> Mono.just(errorResponse(request, err, bindingName)));
    }

    @SuppressWarnings("unchecked")
    private <Q, A> Flux<Message<A>> wrapFluxGroupedResponses(Message<Q> request, Flux<A> responses, MimeType outputContentType, Duration groupTimeout, String bindingName) {
        AtomicLong index = new AtomicLong(0);
        return responses
                .map(payload -> messageConverter.convertMessageToBytesIfNecessary(
                        MessageBuilder
                                .withPayload(payload)
                                .build(),
                        outputContentType.toString()
                ))
                .bufferTimeout(MAX_MSG_PER_CHUNK, groupTimeout)
                .flatMapIterable(msgs -> MessageChunker.mapChunked(msgs, ONE_MB))
                .map(oneMbChunk -> {
                    MessageBuilder<A> mb = (MessageBuilder<A>) MessageBuilder.fromMessage(oneMbChunk.getKey());

                    String indexRange = index.get() + "-" + (index.addAndGet(oneMbChunk.getValue()) - 1);
                    transferAndAdoptHeaders(request, mb, -1, indexRange);
                    return replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(mb.build(), bindingName);
                })
                .concatWith(Mono.fromSupplier(() -> emptyMsg(request, index.get(), index.get(), bindingName))) // Append the finish message
                .onErrorResume(err -> Mono.just(errorResponse(request, err, bindingName)));
    }

    private MimeType getContentType(String bindingName) {
        BindingProperties bindingProperties = this.bindingServiceProperties.getBindingProperties(bindingName);
        return StringUtils.hasText(bindingProperties.getContentType()) ? MimeType.valueOf(bindingProperties.getContentType()) : MimeTypeUtils.APPLICATION_JSON;
    }

    @SuppressWarnings("unchecked")
    private <Q, A> Message<A> emptyMsg(Message<Q> request, long totalReplies, long replyIndex, String bindingName) {
        MessageBuilder<String> mb = MessageBuilder.withPayload("");
        transferAndAdoptHeaders(request, mb, totalReplies, "" + replyIndex);
        return (Message<A>) this.replyWrappingInterceptor.interceptReplyWrappingFinishingEmptyMessage(mb.build(), bindingName);
    }

    @SuppressWarnings("unchecked")
    private <Q, A> Message<A> errorResponse(Message<Q> request, Throwable e, String bindingName) {
        MessageBuilder<String> mb = MessageBuilder.withPayload("");
        transferAndAdoptHeaders(request, mb, 0, "0");
        mb.setHeader(SpringHeaderParser.ERROR_MESSAGE, e.getMessage());
        return (Message<A>) this.replyWrappingInterceptor.interceptReplyWrappingErrorMessage(mb.build(), bindingName);
    }

    private <Q, A> void transferAndAdoptHeaders(Message<Q> request, MessageBuilder<A> mb, long totalReplies, String replyIndex) {
        transferAndAdoptHeaders(request, mb);

        mb.setHeader(SpringHeaderParser.MULTI_TOTAL_REPLIES, totalReplies);
        mb.setHeader(SpringHeaderParser.MULTI_REPLY_INDEX, replyIndex);
    }

    private <Q, A> void transferAndAdoptHeaders(Message<Q> request,
                                                MessageBuilder<A> mb) {
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

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         */
        R apply(T t) throws Exception;
    }
}
