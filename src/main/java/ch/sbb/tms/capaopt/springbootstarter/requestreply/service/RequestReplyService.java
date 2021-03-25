package ch.sbb.tms.capaopt.springbootstarter.requestreply.service;

import ch.sbb.tms.capaopt.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.capaopt.springbootstarter.requestreply.util.HeaderCorrelationIdParserStrategies;
import ch.sbb.tms.capaopt.springbootstarter.requestreply.util.HeaderDestinationParserStrategies;
import ch.sbb.tms.capaopt.springbootstarter.requestreply.util.MessagingUtil;
import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;
import com.solacesystems.jcsmp.JCSMPFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.scheduling.annotation.Async;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;

/**
 * The RequestReplyService takes care of asynchroneous request and reply messages, relating one to the other and allowing to wrap both as a synchroneous call.
 *
 */
public class RequestReplyService implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyService.class);
    private static final ExecutorService REQUEST_REPLY_EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, ResponseHandler> PENDING_RESPONSES = new ConcurrentHashMap<>();

    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private RequestReplyProperties properties;

    @Autowired
    private BinderFactory binderFactory;

    @Autowired
    @Qualifier("integrationArgumentResolverMessageConverter")
    private MessageConverter messageConverter;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void afterPropertiesSet() throws Exception {
        // create reply topic configured in properties
        SolaceConsumerProperties solaceConsumerProperties = new SolaceConsumerProperties();
        solaceConsumerProperties.setProvisionDurableQueue(true);
        solaceConsumerProperties.setQueueRespectsMsgTtl(true);

		LOG.debug("Fetching binder {}", properties.getBinderName());
        Binder binder = binderFactory.getBinder(properties.getBinderName(), PublishSubscribeChannel.class);
        MessageChannel mc = (message, timeout) -> {
			LOG.trace("received message {}", message);
            onReplyReceived(message);
            return true;
        };
		LOG.debug("binding consumer to {}.{}", properties.getReplyToQueueName(), properties.getRequestReplyGroupName());
		binder.bindConsumer( //
				properties.getReplyToQueueName(), // topic to consume from
				properties.getRequestReplyGroupName(), // consumer group for this app
				mc, // handle incoming messages
				new ExtendedConsumerProperties<>(solaceConsumerProperties) //
		);
		LOG.debug("done initializing RequestReplyService");
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * 
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param timeOutInMs the number of milliseconds after which to wait for the respective response
     * @param expectedClass the class the response shall be mapped to
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    public <Q, A> CompletableFuture<A> requestAndAwaitReply(Q request, @NotEmpty String requestDestination, @Min(1) long timeOutInMs,
            Class<A> expectedClass) {
        final AtomicReference<A> returnValue = new AtomicReference<>();

        @SuppressWarnings("unchecked")
        Consumer<Message<?>> responseConsumer = msg -> {
            returnValue.set((A) messageConverter.fromMessage(msg, expectedClass));
        };
        return requestReply(request, requestDestination, responseConsumer, timeOutInMs).thenApply(none -> returnValue.get());
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to send them back via the provided reply destination
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param replyToDestination the message channel to send any replies to
     * @param timeOutInMs the number of milliseconds after which to wait for the respective response
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    public <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull String replyToDestination, //
            @Min(1) long timeOutInMs //
    ) {
        return requestReply(request, requestDestination, msg -> forwardMessage(msg, replyToDestination), timeOutInMs);
    }

    /**
     * sends the given request to the given message channel and prepares the framework to await the response within the given timeframe to direct them at the consumer provided
     * 
     * @param <Q> question/request type
     * @param request the request to be sent
     * @param requestDestination the message channel name to send the request to
     * @param responseConsumer the consumer to handle incoming replies
     * @param timeOutInMs the number of milliseconds after which to wait for the respective response
     * @return a {@link CompletableFuture} spanning the request and response await time
     */
    public <Q> CompletableFuture<Void> requestReply( //
            @NotNull Q request, //
            @NotEmpty String requestDestination, //
            @NotNull Consumer<Message<?>> responseConsumer, //
            @Min(1) long timeOutInMs //
    ) {
        String correlationId = UUID.randomUUID().toString();
        LOG.debug("generated correlation Id {} for request directed to {} with content {}", correlationId, requestDestination, request);

        Message<Q> message = MessageBuilder.withPayload(request) //
                .setCorrelationId(correlationId) //
                .setHeader(SolaceHeaders.CORRELATION_ID, correlationId) //
                //.setHeader(MessageHeaders.REPLY_CHANNEL, properties.getReplyToQueueName()) //
                .setHeader(SolaceHeaders.REPLY_TO, JCSMPFactory.onlyInstance().createTopic(properties.getReplyToQueueName())) //
                .setHeader(BinderHeaders.TARGET_DESTINATION, requestDestination).build() //
        ;
        return postRequest(correlationId, message, responseConsumer, timeOutInMs);
    }

    private CompletableFuture<Void> postRequest( //
            String correlationId, //
            Message<?> message, //
            @NotNull Consumer<Message<?>> responseConsumer, //
            @Min(1) long timeOutInMs //
    ) {
        Runnable requestRunnable = () -> {
			LOG.trace("Sending message {}", message);
            streamBridge.send(HeaderDestinationParserStrategies.retrieve(message.getHeaders()), message);
        };

        return postRequest(correlationId, requestRunnable, responseConsumer, timeOutInMs);
    }

    private CompletableFuture<Void> postRequest( //
            @NotEmpty String correlationId, //
            @NotNull Runnable requestRunnable, //
            @NotNull Consumer<Message<?>> responseConsumer, //
            @Min(1) long timeOutInMs //
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
                .orTimeout(timeOutInMs, TimeUnit.MILLISECONDS) //
                .whenCompleteAsync((reply, error) -> {
                    if (error != null) {
                        LOG.error("Failed to collect response for correlationId {}: {}: {}", correlationId, error.getClass(),
                                error.getMessage());
                    }
                }, REQUEST_REPLY_EXECUTOR);
    }

    private void onReplyReceived(final Message<?> message) {
        final MessageHeaders headers = message.getHeaders();
        String correlationId = HeaderCorrelationIdParserStrategies.retrieve(headers);

        if (correlationId == null) {
            LOG.error("Received unexpected message, without correlation id: {}", message);
            return;
        }

        ResponseHandler handler = PENDING_RESPONSES.get(correlationId);
        if (handler == null) {
            LOG.error("Received unexpected message or maybe to late response: {}", message);
        }
        else {
            handler.receive(message);
        }
    }

	@Async
    private void forwardMessage(final Message<?> originalMessage, String destination) {
        Message<?> forwardMessage = MessagingUtil.forwardMessage(originalMessage, destination);
		LOG.debug("forwarding message {} to {}", forwardMessage, destination);
        streamBridge.send(destination, forwardMessage);
    }
}
