package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
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
public class RequestReplyService {
    static final String MISSING_DESTINATION = "not-set";
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyService.class);
    private static final ExecutorService REQUEST_REPLY_EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, ResponseHandler> PENDING_RESPONSES = new ConcurrentHashMap<>();
    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    @Qualifier("integrationArgumentResolverMessageConverter")
    private MessageConverter messageConverter;

    @Autowired
    private RequestReplyMessageHeaderSupportService messageHeaderSupportService;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Bean
    public Consumer<Message<?>> requestReplyReplies() {
        return this::onReplyReceived;
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    public <Q, A> A requestAndAwaitReply(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws ExecutionException, InterruptedException, TimeoutException {
        final AtomicReference<A> returnValue = new AtomicReference<>();

        @SuppressWarnings("unchecked")
        Consumer<Message<?>> responseConsumer = msg -> returnValue.set((A) messageConverter.fromMessage(msg, expectedClass));

        try {
            return requestReply(
                    request,
                    requestDestination,
                    responseConsumer,
                    timeoutPeriod
            ).thenApply(none -> returnValue.get()).get(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException te) {
            throw new TimeoutException(String.format("Failed to collect response: %s: %s",
                    te.getClass(),
                    te.getMessage()));
        }
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to direct them at the consumer provided
     *
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param responseConsumer the consumer to handle incoming replies
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    private <Q> CompletableFuture<Void> requestReply(
            @NotNull Q request,
            @NotEmpty String requestDestination,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        String correlationId = null;
        if (request instanceof Message) {
            correlationId = messageHeaderSupportService.getCorrelationId((Message<?>) request);
        }
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
            LOG.debug("generated correlation Id {} for request directed to {} with content {}", correlationId, requestDestination, request);
        }

        String replyTopic = bindingServiceProperties.getBindingDestination("requestReplyReplies-in-0");

        if (!StringUtils.hasText(replyTopic) || Objects.equals(replyTopic, MISSING_DESTINATION)) {
            throw new IllegalArgumentException("Missing configuration option: spring.cloud.stream.bindings.requestReplyReplies-in-0.destination");
        }

        // Accepted that a client not using this lib but solace,
        // may be confused about not finding it in the correct solace header locations.
        // But so this lib will work if TibRv and Solace binder are in pom.xml of project.
        MessageBuilder<?> messageBuilder;
        if (request instanceof Message) {
            messageBuilder = MessageBuilder.fromMessage((Message<?>) request);
        } else {
            messageBuilder = MessageBuilder.withPayload(request);
        }

        messageBuilder
                .setCorrelationId(correlationId)
                .setHeader(MessageHeaders.REPLY_CHANNEL, replyTopic);

        return postRequest(requestDestination, correlationId, messageBuilder.build(), responseConsumer, timeoutPeriod);
    }

    private CompletableFuture<Void> postRequest(
            String requestDestination,
            String correlationId,
            Message<?> message,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        Runnable requestRunnable = () -> {
            LOG.trace("Sending message {}", message);
            streamBridge.send(requestDestination, message);
        };

        return postRequest(correlationId, requestRunnable, responseConsumer, timeoutPeriod);
    }

    private CompletableFuture<Void> postRequest(
            @NotEmpty String correlationId,
            @NotNull Runnable requestRunnable,
            @NotNull Consumer<Message<?>> responseConsumer,
            @NotNull @Valid Duration timeoutPeriod
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

        return CompletableFuture.runAsync(runnable, REQUEST_REPLY_EXECUTOR)
                .orTimeout(timeoutPeriod.toMillis(), TimeUnit.MILLISECONDS)
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
}
