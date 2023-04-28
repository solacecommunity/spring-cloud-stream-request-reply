package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.exception.RequestReplyException;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errorMessage.RemoteErrorException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static ch.sbb.tms.platform.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;

/**
 * The RequestReplyService takes care of asynchronous request and reply messages, relating one to the other and allowing to wrap both as a synchronous call.
 */
@Service
public class RequestReplyServiceImpl implements RequestReplyService {
    static final String MISSING_DESTINATION = "not-set";
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyServiceImpl.class);
    private static final ExecutorService REQUEST_REPLY_EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, ResponseHandler> PENDING_RESPONSES = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StreamBridge streamBridge;

    @Autowired
    @Qualifier("integrationArgumentResolverMessageConverter")
    private MessageConverter messageConverter;

    @Autowired
    private RequestReplyMessageHeaderSupportService messageHeaderSupportService;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    private RequestReplyProperties requestReplyProperties;

    @Override
    public <Q, A> A requestAndAwaitReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException, RemoteErrorException {
        return wrapTimeOutException(() ->
                requestReplyToTopic(
                        request,
                        requestDestination,
                        expectedClass,
                        timeoutPeriod
                ).get(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public <Q, A> A requestAndAwaitReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException, RemoteErrorException {
        return wrapTimeOutException(() ->
                requestReplyToBinding(
                        request,
                        bindingName,
                        expectedClass,
                        timeoutPeriod
                ).get(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS)
        );
    }

    @Override
    public <Q, A> CompletableFuture<A> requestReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        final AtomicReference<A> returnValue = new AtomicReference<>();

        return requestReply(
                request,
                bindingName,
                bindingServiceProperties.getBindingDestination(bindingName + "-out-0"),
                msg -> returnValue.set(extractMsgBody(expectedClass, msg)),
                timeoutPeriod,
                false
        ).thenApply(none -> returnValue.get());
    }

    @Override
    public <Q, A> CompletableFuture<A> requestReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        final AtomicReference<A> returnValue = new AtomicReference<>();

        String bindingName = requestReplyProperties
                .findMatchingBinder(requestDestination)
                .orElseThrow(() -> new IllegalArgumentException("Unable to find binding for destination: " + requestDestination + " Please check spring.cloud.stream.requestreply.bindingMapping in your configuration."));

        return requestReply(
                request,
                bindingName,
                requestDestination,
                msg -> returnValue.set(extractMsgBody(expectedClass, msg)),
                timeoutPeriod,
                false
        ).thenApply(none -> returnValue.get());
    }

    @Override
    public <Q, A> Flux<A> requestReplyToBindingReactive(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        return Flux.create(fluxSink -> {
            try {
                wrapTimeOutException(() -> requestReply(
                        request,
                        bindingName,
                        bindingServiceProperties.getBindingDestination(bindingName + "-out-0"),
                        fluxResponseConsumer(expectedClass, fluxSink),
                        timeoutPeriod,
                        true
                ).get(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS));
                fluxSink.complete();
            }
            catch (Exception e) {
                fluxSink.error(e);
            }
        });
    }

    @Override
    public <Q, A> Flux<A> requestReplyToTopicReactive(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        return Flux.create(fluxSink -> {
            try {
                String bindingName = requestReplyProperties
                        .findMatchingBinder(requestDestination)
                        .orElseThrow(() -> new IllegalArgumentException("Unable to find binding for destination: " + requestDestination + " Please check spring.cloud.stream.requestreply.bindingMapping in your configuration."));

                wrapTimeOutException(() -> requestReply(
                        request,
                        bindingName,
                        requestDestination,
                        fluxResponseConsumer(expectedClass, fluxSink),
                        timeoutPeriod,
                        true
                ).get(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS));
                fluxSink.complete();
            }
            catch (Exception e) {
                fluxSink.error(e);
            }
        });
    }

    @NotNull
    private <A> Consumer<Message<?>> fluxResponseConsumer(Class<A> expectedClass, FluxSink<A> fluxSink) {
        return msg -> {
            A payload = extractMsgBody(expectedClass, msg);
            if (payload != null) {
                fluxSink.next(payload);
            }
        };
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <A> A extractMsgBody(Class<A> expectedClass, Message<?> msg) {
        return expectedClass.isAssignableFrom(msg.getPayload().getClass()) ?
                (A) msg.getPayload() :
                (A) messageConverter.fromMessage(msg, expectedClass);
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to direct them at the consumer provided
     *
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param bindingName the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param requestDestination the message channel name to send the request to
     * @param responseConsumer the consumer to handle incoming replies
     * @param multipleResponses indicator if more than one response can be accepted
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    private <Q> CompletableFuture<Void> requestReply(
            @NotNull Q request,
            @NotEmpty String bindingName,
            @NotEmpty String requestDestination,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod,
            boolean multipleResponses
    ) {
        String correlationId = null;
        if (request instanceof Message) {
            correlationId = messageHeaderSupportService.getCorrelationId((Message<?>) request);
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            LOG.debug("generated correlation Id {} for request directed to {} with content {}", correlationId, requestDestination, request);
        }

        final String requestDestinationRaw = requestReplyProperties.replaceVariablesWithWildcard(requestDestination);

        String replyTopic = requestReplyProperties.getBindingMapping(bindingName)
                .orElseThrow(() -> new IllegalArgumentException("Unable to send request reply: Missing binding mapping for: " + bindingName + ". "
                        + "Please check that there is a matching: spring.cloud.stream.requestreply.bindingMapping[].binding"))
                .getReplyTopic();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Using binding:" + bindingName +
                    " , destination:" + requestDestinationRaw +
                    " and replyTopic:" + replyTopic);
        }

        if (!StringUtils.hasText(replyTopic) || Objects.equals(replyTopic, MISSING_DESTINATION)) {
            throw new IllegalArgumentException("Missing configuration option: spring.cloud.stream.requestreply[].replyTopic where binding: " + bindingName + "");
        }

        // Accepted that a client not using this lib but solace,
        // may be confused about not finding it in the correct solace header locations.
        // But so this lib will work if TibRv and Solace binder are in pom.xml of project.
        MessageBuilder<?> messageBuilder;
        if (request instanceof Message) {
            messageBuilder = MessageBuilder.fromMessage((Message<?>) request);
        }
        else {
            messageBuilder = MessageBuilder.withPayload(request);
        }

        messageBuilder
                .setCorrelationId(correlationId)
                .setHeader(BinderHeaders.TARGET_DESTINATION, requestDestinationRaw)
                .setHeader(MessageHeaders.REPLY_CHANNEL, replyTopic);

        return postRequest(bindingName + "-out-0", correlationId, messageBuilder.build(), responseConsumer, timeoutPeriod, multipleResponses);
    }

    private CompletableFuture<Void> postRequest(
            String bindingName,
            String correlationId,
            Message<?> message,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod,
            boolean multipleResponses
    ) {
        Runnable requestRunnable = () -> {
            LOG.trace("Sending message {}", message);
            streamBridge.send(bindingName, message);
        };

        return postRequest(correlationId, requestRunnable, responseConsumer, timeoutPeriod, multipleResponses);
    }

    private CompletableFuture<Void> postRequest(
            @NotEmpty String correlationId,
            @NotNull Runnable requestRunnable,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod,
            boolean multipleResponses
    ) {
        ResponseHandler responseHandler = new ResponseHandler(responseConsumer, multipleResponses);
        ResponseHandler previous = PENDING_RESPONSES.putIfAbsent(correlationId, responseHandler);
        if (previous != null) {
            throw new IllegalArgumentException("response for correlation ID " + correlationId + " is already awaited");
        }

        Runnable runnable = throwingUnchecked(() -> {
            try {
                LOG.trace("Querying correlationId {}", correlationId);
                requestRunnable.run();
                responseHandler.await();
            }
            finally {
                LOG.trace("Disregarding correlationId {}", correlationId);
                PENDING_RESPONSES.remove(correlationId);
            }
        });

        return CompletableFuture.runAsync(runnable, REQUEST_REPLY_EXECUTOR)
                .orTimeout(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS)
                .whenCompleteAsync((reply, error) -> {
                    if (error != null) {
                        LOG.error("Failed to collect response for correlationId {}: {}: {}", correlationId, error.getClass(),
                                error.getMessage());
                    }
                }, REQUEST_REPLY_EXECUTOR);
    }

    private <T> T wrapTimeOutException(TimeoutSupplier<T> businessLogic) throws TimeoutException, RemoteErrorException {
        try {
            return businessLogic.get();
        }
        catch (InterruptedException | TimeoutException | ExecutionException te) {
            if (te instanceof ExecutionException &&
                    te.getCause() instanceof RequestReplyException &&
                    te.getCause().getCause() instanceof RemoteErrorException) {
                throw (RemoteErrorException) te.getCause().getCause();
            }

            throw new TimeoutException(String.format("Failed to collect response: %s: %s",
                    te.getClass(),
                    te.getMessage()));
        }
    }

    private interface TimeoutSupplier<T> {
        T get() throws InterruptedException, TimeoutException, ExecutionException;
    }

    void onReplyReceived(final Message<?> message) {
        String correlationId = messageHeaderSupportService.getCorrelationId(message);

        if (correlationId == null) {
            LOG.error("Received unexpected message, without correlation id: {}", message);
            return;
        }

        Integer totalReplies = messageHeaderSupportService.getTotalReplies(message);
        String errorMessage = messageHeaderSupportService.getErrorMessage(message);

        ResponseHandler handler = PENDING_RESPONSES.get(correlationId);
        if (handler == null) {
            LOG.error("Received unexpected message or maybe too late response: {}", message);
        }
        else {
            if (totalReplies != null) {
                handler.setTotalReplies(totalReplies);

                if (totalReplies == 0) {
                    if (StringUtils.hasText(errorMessage)) {
                        // null will be filtered. An empty flux will be returned.
                        handler.errorResponse(errorMessage);
                    }
                    else {
                        // null will be filtered. An empty flux will be returned.
                        handler.emptyResponse();
                    }
                    return;
                }
            }

            if (StringUtils.hasText(errorMessage)) {
                handler.errorResponse(errorMessage);
            }
            else {
                handler.receive(message);
            }
        }
    }
}