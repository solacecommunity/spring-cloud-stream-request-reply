package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage.RemoteErrorException;
import reactor.core.publisher.Flux;

public interface RequestReplyService {

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass the class the response shall be mapped to.
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> A requestAndAwaitReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws InterruptedException, TimeoutException, RemoteErrorException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param bindingName the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> A requestAndAwaitReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws InterruptedException, TimeoutException, RemoteErrorException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param bindingName the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> CompletableFuture<A> requestReplyToBinding(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return value
     * this is non-blocking.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link CompletableFuture} which can be used to await and retrieve the response
     */
    <Q, A> CompletableFuture<A> requestReplyToTopic(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    ) throws TimeoutException;

    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param bindingName the message channel name to send the request to. Example: requestReplyRepliesDemoTibrv
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link Flux} which can be used to receive responses
     */
    <Q, A> Flux<A> requestReplyToBindingReactive(
            Q request,
            @NotEmpty String bindingName,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    );


    /**
     * sends the given request to the given message channel, awaits the response and maps it to the provided class as a return
     * as stream of values this is non-blocking.
     * This can be used for request/multy reply.
     * Attention: use this only for the special edge case that you need parallel requests in the same thread.
     *
     * @param <Q> question/request type
     * @param <A> answer/response type
     * @param request the request to be sent
     * @param requestDestination the topic name to send the request to. Example: my.supper.topic
     * @param expectedClass the class the response shall be mapped to
     * @param timeoutPeriod the timeout when to give up waiting for a response
     * @return a {@link Flux} which can be used to receive responses
     */
    <Q, A> Flux<A> requestReplyToTopicReactive(
            Q request,
            @NotEmpty String requestDestination,
            Class<A> expectedClass,
            @NotNull @Valid Duration timeoutPeriod
    );
}
