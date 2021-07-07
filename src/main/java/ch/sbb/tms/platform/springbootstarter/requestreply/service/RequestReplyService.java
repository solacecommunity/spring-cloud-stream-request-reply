/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import static ch.sbb.tms.platform.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.scheduling.annotation.Async;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties.Period;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;

/**
 * The RequestReplyService takes care of asynchroneous request and reply messages, relating one to the other and allowing to wrap both as a synchroneous call.
 */
public class RequestReplyService implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyService.class);
    private static final ExecutorService REQUEST_REPLY_EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, ResponseHandler> PENDING_RESPONSES = new ConcurrentHashMap<>();
    static final AtomicReference<RequestReplyService> REQUEST_REPLY_SERVICE_REFERENCE = new AtomicReference<>(null);

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    @Qualifier("integrationArgumentResolverMessageConverter")
    private MessageConverter messageConverter;

    @Autowired
    private RequestReplyMessageHeaderSupportService messageHeaderSupportService;

    @Autowired
    private RequestReplyBinderAdapterFactory binderAdapterFactory;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * 
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param expectedClass the class the response shall be mapped to
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    public <Q, A> CompletableFuture<A> requestAndAwaitReply( //
            Q request, //
            @NotEmpty String requestDestination, //
            Class<A> expectedClass //
    ) {
        return requestAndAwaitReply(request, requestDestination, binderAdapterFactory.getCachedAdapterForDefaultBinder(), expectedClass);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * 
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param expectedClass the class the response shall be mapped to
     * @param binder the binder to prepare messages for
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    public <Q, A> CompletableFuture<A> requestAndAwaitReply( //
            Q request, //
            @NotEmpty String requestDestination, //
            Class<A> expectedClass, //
            @NotNull Binder<?, ?, ?> binder //
    ) {
        return requestAndAwaitReply(request, requestDestination, binderAdapterFactory.getCachedAdapterForBinder(binder), expectedClass);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * 
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param expectedClass the class the response shall be mapped to
     * @param binderName the binder to prepare messages for
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    public <Q, A> CompletableFuture<A> requestAndAwaitReply( //
            Q request, //
            @NotEmpty String requestDestination, //
            Class<A> expectedClass, //
            @NotEmpty String binderName //
    ) {
        return requestAndAwaitReply(request, requestDestination, binderAdapterFactory.getCachedAdapterForBinder(binderName), expectedClass);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * 
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param requestReplyBinderAdapter binder specific strategy
     * @param expectedClass the class the response shall be mapped to
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    private <Q, A> CompletableFuture<A> requestAndAwaitReply( //
            Q request, //
            @NotEmpty String requestDestination, //
            @NotNull RequestReplyBinderAdapter requestReplyBinderAdapter, //
            Class<A> expectedClass //
    ) {
        final AtomicReference<A> returnValue = new AtomicReference<>();

        @SuppressWarnings("unchecked")
        Consumer<Message<?>> responseConsumer = msg -> returnValue.set((A) messageConverter.fromMessage(msg, expectedClass));

        return requestReply( //
                request, //
                requestDestination, //
                requestReplyBinderAdapter, //
                responseConsumer //
        ).thenApply(none -> returnValue.get());
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to send them back via the provided reply destination
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param replyToDestination the message channel to send any replies to
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    public <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull String replyToDestination //
    ) {
        return requestReply( //
                request, //
                requestDestination, //
                binderAdapterFactory.getCachedAdapterForDefaultBinder(), //
                msg -> forwardMessage(msg, replyToDestination) //
        );
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to send them back via the provided reply destination
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param replyToDestination the message channel to send any replies to
     * @param binder the binder to prepare messages for
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    public <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull String replyToDestination, //
            @NotNull Binder<?, ?, ?> binder //
    ) {
        return requestReply( //
                request, //
                requestDestination, //
                binderAdapterFactory.getCachedAdapterForBinder(binder), //
                msg -> forwardMessage(msg, replyToDestination) //
        );
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to send them back via the provided reply destination
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param replyToDestination the message channel to send any replies to
     * @param binderName the binder to prepare messages for
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    public <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull String replyToDestination, //
            @NotEmpty String binderName //
    ) {
        return requestReply( //
                request, //
                requestDestination, //
                binderAdapterFactory.getCachedAdapterForBinder(binderName), //
                msg -> forwardMessage(msg, replyToDestination) //
        );
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to direct them at the consumer provided
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param requestReplyBinderAdapter binder specific strategy
     * @param responseConsumer the consumer to handle incoming replies
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    private <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull RequestReplyBinderAdapter requestReplyBinderAdapter, //
            @NotNull Consumer<Message<?>> responseConsumer //
    ) {
        String correlationId = UUID.randomUUID().toString();
        LOG.debug("generated correlation Id {} for request directed to {} with content {}", correlationId, requestDestination, request);

        MessageBuilder<@NotNull Q> messageBuilder = MessageBuilder.withPayload(request);
        requestReplyBinderAdapter.getMessagebuilderConfigurer() //
                .adoptTo(messageBuilder) //
                .setCorrelationId(correlationId) //
                .setReplyToTopic(requestReplyBinderAdapter.getReplyTopic()) //
                .setDestination(requestDestination) //
        ;

        return postRequest(correlationId, messageBuilder.build(), responseConsumer, requestReplyBinderAdapter.getTimeoutPeriod());
    }

    private CompletableFuture<Void> postRequest( //
            String correlationId, //
            Message<?> message, //
            @NotNull Consumer<Message<?>> responseConsumer, //
            @NotNull @Valid Period timeoutPeriod //
    ) {
        Runnable requestRunnable = () -> {
			LOG.trace("Sending message {}", message);
            String target = messageHeaderSupportService.getDestination(message);
            streamBridge.send(target, message);
        };

        return postRequest(correlationId, requestRunnable, responseConsumer, timeoutPeriod);
    }

    private CompletableFuture<Void> postRequest( //
            @NotEmpty String correlationId, //
            @NotNull Runnable requestRunnable, //
            @NotNull Consumer<Message<?>> responseConsumer, //
            @NotNull @Valid Period timeoutPeriod //
    ) {
        ResponseHandler responseHandler = new ResponseHandler(responseConsumer);
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

        return CompletableFuture.runAsync(runnable, REQUEST_REPLY_EXECUTOR) //
                .orTimeout(timeoutPeriod.getDuration(), timeoutPeriod.getUnit()) //
                .whenCompleteAsync((reply, error) -> {
                    if (error != null) {
                        LOG.error("Failed to collect response for correlationId {}: {}: {}", correlationId, error.getClass(),
                                error.getMessage());
                    }
                }, REQUEST_REPLY_EXECUTOR);
    }

    void onReplyReceived(final Message<?> message) {
        String correlationId = messageHeaderSupportService.getCorrelationId(message);

        if (correlationId == null) {
            LOG.error("Received unexpected message, without correlation id: {}", message);
            return;
        }

        ResponseHandler handler = PENDING_RESPONSES.get(correlationId);
        if (handler == null) {
            LOG.error("Received unexpected message or maybe too late response: {}", message);
        }
        else {
            handler.receive(message);
        }
    }

	@Async
    private void forwardMessage(final Message<?> originalMessage, String destination) {
        Message<?> forwardMessage = messageHeaderSupportService.forwardMessage(originalMessage, destination);
		LOG.debug("forwarding message {} to {}", forwardMessage, destination);
        streamBridge.send(destination, forwardMessage);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        synchronized (REQUEST_REPLY_SERVICE_REFERENCE) {
            if (REQUEST_REPLY_SERVICE_REFERENCE.get() == null) {
                REQUEST_REPLY_SERVICE_REFERENCE.set(applicationContext.getBean(RequestReplyService.class));
            }
        }
    }
}
