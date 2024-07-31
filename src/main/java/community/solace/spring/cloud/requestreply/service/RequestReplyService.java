package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public interface RequestReplyService {

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to.
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    default <Q, A> A requestAndAwaitReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws InterruptedException, TimeoutException, RemoteErrorException {
        return requestAndAwaitReplyToTopic(request, requestDestination, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to.
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @param additionalHeaders  additional headers to be added to the request message
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> A requestAndAwaitReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    ) throws InterruptedException, TimeoutException, RemoteErrorException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q>           question/request type
     * @param <A>           answer/response type
     * @param request       the request to be sent
     * @param bindingName   the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    default <Q, A> A requestAndAwaitReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws InterruptedException, TimeoutException, RemoteErrorException {
        return requestAndAwaitReplyToBinding(request, bindingName, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q>               question/request type
     * @param <A>               answer/response type
     * @param request           the request to be sent
     * @param bindingName       the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass     the class the response shall be mapped to
     * @param timeoutPeriod     the timeout when to give up waiting for a response
     * @param additionalHeaders additional headers to be added to the request message
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> A requestAndAwaitReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    ) throws InterruptedException, TimeoutException, RemoteErrorException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>           question/request type
     * @param <A>           answer/response type
     * @param request       the request to be sent
     * @param bindingName   the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    default <Q, A> CompletableFuture<A> requestReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException {
        return requestReplyToBinding(request, bindingName, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>               question/request type
     * @param <A>               answer/response type
     * @param request           the request to be sent
     * @param bindingName       the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass     the class the response shall be mapped to
     * @param timeoutPeriod     the timeout when to give up waiting for a response
     * @param additionalHeaders additional headers to be added to the request message
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> CompletableFuture<A> requestReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    ) throws TimeoutException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    default <Q, A> CompletableFuture<A> requestReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException {
        return requestReplyToTopic(request, requestDestination, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @param additionalHeaders  additional headers to be added to the request message
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> CompletableFuture<A> requestReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    ) throws TimeoutException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>           question/request type
     * @param <A>           answer/response type
     * @param request       the request to be sent
     * @param bindingName   the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link Flux} which can be used to receive responses
     */
    default <Q, A> Flux<A> requestReplyToBindingReactive(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        return requestReplyToBindingReactive(request, bindingName, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>               question/request type
     * @param <A>               answer/response type
     * @param request           the request to be sent
     * @param bindingName       the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass     the class the response shall be mapped to
     * @param timeoutPeriod     the timeout when to give up waiting for a response
     * @param additionalHeaders additional headers to be added to the request message
     * @return a {@link Flux} which can be used to receive responses
     */
    <Q, A> Flux<A> requestReplyToBindingReactive(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    );


    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @return a {@link Flux} which can be used to receive responses
     */
    default <Q, A> Flux<A> requestReplyToTopicReactive(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) {
        return requestReplyToTopicReactive(request, requestDestination, expectedClass, timeoutPeriod, null);
    }

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q>                question/request type
     * @param <A>                answer/response type
     * @param request            the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass      the class the response shall be mapped to
     * @param timeoutPeriod      the timeout when to give up waiting for a response
     * @param additionalHeaders  additional headers to be added to the request message
     * @return a {@link Flux} which can be used to receive responses
     */
    <Q, A> Flux<A> requestReplyToTopicReactive(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod,
            Map<String, Object> additionalHeaders
    );
}
