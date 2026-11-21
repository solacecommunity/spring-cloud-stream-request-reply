package community.solace.spring.cloud.requestreply.service.header;

import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;
import community.solace.spring.cloud.requestreply.service.header.parser.SpringHeaderParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RequestReplySimpleMessageHeaderSupportServiceTests extends AbstractRequestReplySimpleIT {
    private static final Duration REQUEST_TIME_TO_LIVE = Duration.ofSeconds(10);

    @Autowired
    RequestReplyMessageHeaderSupportService supportService;

    @Autowired
    BindingServiceProperties bindingServiceProperties;

    @Test
    void getCorrelationId() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("solace_correlationId", "rt54756iuj")
                .build();

        assertEquals(
                "rt54756iuj",
                supportService.getCorrelationId(m)
        );
    }

    @Test
    void getDestination() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("spring.cloud.stream.sendto.destination", "1337/foo")
                .build();

        assertEquals(
                "1337/foo",
                supportService.getDestination(m)
        );
    }

    @Test
    void getReplyTo() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "to/me")
                .build();

        assertEquals(
                "to/me",
                supportService.getReplyTo(m)
        );
    }

    @Test
    void getTotalReplies() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(SpringHeaderParser.MULTI_TOTAL_REPLIES, 167)
                .build();

        assertEquals(
                167L,
                supportService.getTotalReplies(m)
        );
    }

    @Test
    void getErrorMessage() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(SpringHeaderParser.ERROR_MESSAGE, "Something went wrong")
                .build();

        assertEquals(
                "Something went wrong",
                supportService.getErrorMessage(m)
        );
    }

    @Test
    void wrap() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m, (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        Message<String> answerM = supplier.apply(m);

        assertEquals(
                "my-correlationId-my",
                answerM.getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answerM.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answerM.getHeaders().get("custom")
        );
    }

    @Test
    void wrap_noResponse() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> null, (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                                          .setHeader("correlationId", "my-correlationId-my")
                                          .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                                          .setHeader("custom", "my-custom-my")
                                          .build();

        Message<String> answerM = supplier.apply(m);

        assertNull(
                answerM
        );
    }

    @Test
    void wrapList_singleResponses() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m), "requestReplyRepliesDemo-out-0", (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        List<Message<String>> answers = supplier.apply(m);

        assertEquals(
                2,
                answers.size()
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(0).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.get(0).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answers.get(0).getHeaders().get("custom")
        );
        assertEquals(
                2L,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                "0",
                answers.get(0).getHeaders().get("replyIndex")
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(1).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.get(1).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answers.get(1).getHeaders().get("custom")
        );
        assertEquals(
                2L,
                answers.get(1).getHeaders().get("totalReplies")
        );
        assertEquals(
                "1",
                answers.get(1).getHeaders().get("replyIndex")
        );
    }

    @Test
    void wrapList_noResponses() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> null, "requestReplyRepliesDemo-out-0", (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                                          .setHeader("correlationId", "my-correlationId-my")
                                          .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                                          .setHeader("custom", "my-custom-my")
                                          .build();

        List<Message<String>> answers = supplier.apply(m);

        assertNull(
                answers
        );
    }


    @Test
    void wrapList_groupedResponses() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m), "requestReplyRepliesDemo-out-0", (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("groupedMessages", true)
                .setHeader("custom", "my-custom-my")
                .build();

        List<Message<String>> answers = supplier.apply(m);

        SDTStream expectedBody = new StreamImpl();
        expectedBody.writeString("BytesMessage");
        expectedBody.writeBytes("demo".getBytes(StandardCharsets.UTF_8));
        expectedBody.writeString("BytesMessage");
        expectedBody.writeBytes("demo".getBytes(StandardCharsets.UTF_8));

        assertEquals(
                1,
                answers.size()
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(0).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.get(0).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answers.get(0).getHeaders().get("custom")
        );
        assertEquals(
                2L,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                "0-1",
                answers.get(0).getHeaders().get("replyIndex")
        );
        assertEquals(
                expectedBody,
                answers.get(0).getPayload()
        );
    }

    @Test
    void wrapFlux_singleResponses() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux((payloadIn, outSink) -> {
            outSink.next("first msg");
            outSink.complete();
        }, "requestReplyRepliesDemo-out-0");

        Message<String> inMsg = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        Flux<Message<String>> answers = supplier.apply(Flux.just(inMsg));

        StepVerifier
                .create(answers)
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "first msg",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            -1L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "0",
                            aMsg.getHeaders().get("replyIndex")
                    );
                })
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            1L,
                            aMsg.getHeaders().get("totalReplies"),
                            "Total replies should be set at last msg"
                    );
                    assertEquals(
                            "1",
                            aMsg.getHeaders().get("replyIndex"),
                            "replyIndex should be equal to total replies at last msg"
                    );
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void wrapFlux_groupedResponses() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux((payloadIn, outSink) -> {
            outSink.next("first msg");
            outSink.next("second msg");
            outSink.complete();
        }, "requestReplyRepliesDemo-out-0");

        Message<String> inMsg = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .setHeader("groupedMessages", true)
                .build();

        Flux<Message<String>> answers = supplier.apply(Flux.just(inMsg));

        SDTStream expectedBody = new StreamImpl();
        expectedBody.writeString("BytesMessage");
        expectedBody.writeBytes("first msg".getBytes(StandardCharsets.UTF_8));
        expectedBody.writeString("BytesMessage");
        expectedBody.writeBytes("second msg".getBytes(StandardCharsets.UTF_8));

        StepVerifier
                .create(answers)
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            expectedBody,
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            -1L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "0-1",
                            aMsg.getHeaders().get("replyIndex")
                    );
                })
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            2L,
                            aMsg.getHeaders().get("totalReplies"),
                            "Total replies should be set at last msg"
                    );
                    assertEquals(
                            "2",
                            aMsg.getHeaders().get("replyIndex"),
                            "replyIndex should be equal to total replies at last msg"
                    );
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }


    @Test
    void wrapList_emptyList_shouldCreateMessageWithTotalRepliesNull() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> Collections.emptyList(), "requestReplyRepliesDemo-out-0", (Class<Throwable>) null);

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        List<Message<String>> answers = supplier.apply(m);

        assertEquals(
                1,
                answers.size()
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(0).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.get(0).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertEquals(
                0L,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                "0",
                answers.get(0).getHeaders().get("replyIndex")
        );
    }

    @Test
    void wrapFlux_emptyList_shouldCreateMessageWithTotalRepliesNull() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux(
                (payloadIn, outSink) -> outSink.complete(),
                "requestReplyRepliesDemo-out-0"
        );

        Message<String> inMsg = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        Flux<Message<String>> answers = supplier.apply(Flux.just(inMsg));

        StepVerifier
                .create(answers)
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            0L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "0",
                            aMsg.getHeaders().get("replyIndex")
                    );
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    @Test
    void wrap_exception_shouldReturnErrorWhenMatch() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(
                m -> {
                    throw new IllegalArgumentException("The error message");
                },
                IllegalArgumentException.class
        );

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        Message<String> answers = supplier.apply(m);

        assertEquals(
                "my-correlationId-my",
                answers.getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertEquals(
                "The error message",
                answers.getHeaders().get("errorMessage")
        );
    }

    @Test
    void wrap_exception_shouldThrowWhenNotMatch() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(
                m -> {
                    throw new IllegalArgumentException("The error message");
                },
                NullPointerException.class
        );

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> supplier.apply(m));

        assertEquals(
                "The error message",
                e.getMessage()
        );
    }

    @Test
    void wrapList_exception_shouldReturnErrorWhenMatch() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(
                m -> {
                    throw new IllegalArgumentException("The error message");
                },
                "requestReplyRepliesDemo-out-0",
                IllegalArgumentException.class
        );

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        List<Message<String>> answers = supplier.apply(m);

        assertEquals(
                1,
                answers.size()
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(0).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-pineapple/the-event-after",
                answers.get(0).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertEquals(
                0L,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                "0",
                answers.get(0).getHeaders().get("replyIndex")
        );
        assertEquals(
                "The error message",
                answers.get(0).getHeaders().get("errorMessage")
        );
    }

    @Test
    void wrapList_exception_shouldThrowWhenNotMatch() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(
                m -> {
                    throw new IllegalArgumentException("The error message");
                },
                "requestReplyRepliesDemo-out-0",
                NullPointerException.class
        );

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> supplier.apply(m));

        assertEquals(
                "The error message",
                e.getMessage()
        );
    }

    @Test
    void wrapFlux_exception() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux((payloadIn, outSink) -> {
            outSink.next("first msg");
            outSink.error(new RuntimeException("The error message"));
            outSink.complete();
        }, "requestReplyRepliesDemo-out-0");

        Message<String> inMsg = MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader("custom", "my-custom-my")
                .build();

        Flux<Message<String>> answers = supplier.apply(Flux.just(inMsg));

        StepVerifier
                .create(answers)
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "first msg",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            -1L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "0",
                            aMsg.getHeaders().get("replyIndex")
                    );
                })
                .consumeNextWith(aMsg -> {
                    assertEquals(
                            "",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-pineapple/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            0L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "0",
                            aMsg.getHeaders().get("replyIndex")
                    );
                    assertEquals(
                            "The error message",
                            aMsg.getHeaders().get("errorMessage")
                    );
                })
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }


    @Test
    void getTimeToLive() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(SolaceHeaderParser.TIME_TO_LIVE, 5_000L)
                .build();

        assertEquals(
                Duration.ofSeconds(5),
                supportService.getTimeToLive(m)
        );
    }

    @Test
    void getTimeToLive_ofZero_meansTheMessageNeverExpires() {
        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(SolaceHeaderParser.TIME_TO_LIVE, 0L)
                .build();

        assertNull(
                supportService.getTimeToLive(m)
        );
    }

    @Test
    void wrap_expectTheRequestsTimeToLive_whenRequestCarriesOne() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m, (Class<Throwable>) null);

        Message<String> answerM = supplier.apply(requestWithTimeToLive(REQUEST_TIME_TO_LIVE));

        assertTheRequestsTimeToLive(answerM);
    }

    @Test
    void wrap_expectNoTimeToLive_whenRequestCarriesNone() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m, (Class<Throwable>) null);

        Message<String> answerM = supplier.apply(MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .build());

        assertNull(
                answerM.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE)
        );
    }

    @Test
    void wrap_expectNoTimeToLive_whenRequestNeverExpires() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m, (Class<Throwable>) null);

        Message<String> answerM = supplier.apply(requestWithTimeToLive(Duration.ZERO));

        assertNull(
                answerM.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE)
        );
    }

    @Test
    void wrap_expectCallersTimeToLive_whenGivenInAdditionalHeaders() {
        long neverExpire = 0L;
        Function<Message<String>, Message<String>> supplier = supportService.wrap(
                m -> m,
                Map.of(SolaceHeaderParser.TIME_TO_LIVE, neverExpire),
                (Class<Throwable>) null
        );

        Message<String> answerM = supplier.apply(requestWithTimeToLive(REQUEST_TIME_TO_LIVE));

        assertEquals(
                neverExpire,
                answerM.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE, Long.class)
        );
    }

    /**
     * A Solace message must never carry a time-to-live and an expiration together.
     */
    @Test
    void wrap_expectNoTimeToLive_whenAdditionalHeadersGiveAnExpiration() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(
                m -> m,
                Map.of(SolaceHeaderParser.EXPIRATION, 1_682_928_000_000L),
                (Class<Throwable>) null
        );

        Message<String> answerM = supplier.apply(requestWithTimeToLive(REQUEST_TIME_TO_LIVE));

        assertNull(
                answerM.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE)
        );
    }

    @Test
    void wrap_exception_expectTheRequestsTimeToLiveOnTheErrorReply() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(
                m -> {
                    throw new IllegalArgumentException("The error message");
                },
                IllegalArgumentException.class
        );

        Message<String> answerM = supplier.apply(requestWithTimeToLive(REQUEST_TIME_TO_LIVE));

        assertTheRequestsTimeToLive(answerM);
    }

    @Test
    void wrapList_groupedResponses_expectTheRequestsTimeToLive() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m), "requestReplyRepliesDemo-out-0", (Class<Throwable>) null);

        Message<String> request = MessageBuilder.fromMessage(requestWithTimeToLive(REQUEST_TIME_TO_LIVE))
                .setHeader(SpringHeaderParser.GROUPED_MESSAGES, true)
                .build();
        List<Message<String>> answers = supplier.apply(request);

        assertEquals(
                1,
                answers.size()
        );
        assertTheRequestsTimeToLive(answers.get(0));
    }

    @Test
    void wrapFlux_expectTheRequestsTimeToLiveOnEveryReply_includingTheFinishingOne() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux((payloadIn, outSink) -> {
            outSink.next("first msg");
            outSink.complete();
        }, "requestReplyRepliesDemo-out-0");

        Flux<Message<String>> answers = supplier.apply(Flux.just(requestWithTimeToLive(REQUEST_TIME_TO_LIVE)));

        StepVerifier
                .create(answers)
                .consumeNextWith(this::assertTheRequestsTimeToLive)
                .consumeNextWith(this::assertTheRequestsTimeToLive)
                .expectComplete()
                .verify(Duration.ofSeconds(10));
    }

    private static Message<String> requestWithTimeToLive(Duration requestTimeToLive) {
        return MessageBuilder.withPayload("demo")
                .setHeader("correlationId", "my-correlationId-my")
                .setHeader(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after")
                .setHeader(SolaceHeaderParser.TIME_TO_LIVE, requestTimeToLive.toMillis())
                .build();
    }

    private void assertTheRequestsTimeToLive(Message<String> reply) {
        assertEquals(
                REQUEST_TIME_TO_LIVE.toMillis(),
                reply.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE, Long.class),
                "The reply must expire like the request it answers"
        );
    }

    @Test
    void replyTopicWithWildcards() {
        assertEquals(
                "requestReply/response/*/itTests",
                bindingServiceProperties.getBindingDestination("requestReplyRepliesDemo-in-0")
        );


        String uuidDemoA = bindingServiceProperties.getBindingDestination("uuidDemoA-in-0");
        String uuidDemoB = bindingServiceProperties.getBindingDestination("uuidDemoB-in-0");

        assertEquals(
                uuidDemoA,
                uuidDemoB
        );

        assertTrue(
                uuidDemoA.matches("uuidDemo/[a-f0-9]{8}-?[a-f0-9]{4}-?4[a-f0-9]{3}-?[89ab][a-f0-9]{3}-?[a-f0-9]{12}"),
                "Random UUID4 should be provided"
        );
    }

}
