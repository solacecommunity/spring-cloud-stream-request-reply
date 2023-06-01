package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SpringHeaderParser;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestReplyMessageHeaderSupportServiceTests extends AbstractRequestReplyIT {

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
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m);

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
                "my-dest-my/p-arcs/the-event-after",
                answerM.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answerM.getHeaders().get("custom")
        );
    }

    @Test
    void wrapList_singleResponses() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m), "requestReplyRepliesDemo-out-0");

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
                "my-dest-my/p-arcs/the-event-after",
                answers.get(0).getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );
        assertNull(
                answers.get(0).getHeaders().get("custom")
        );
        assertEquals(
                answers.get(0).getHeaders().get("totalReplies"),
                2L
        );
        assertEquals(
                answers.get(0).getHeaders().get("replyIndex"),
                "0"
        );

        assertEquals(
                "my-correlationId-my",
                answers.get(1).getHeaders().get("correlationId")
        );
        assertEquals(
                "my-dest-my/p-arcs/the-event-after",
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
    void wrapList_groupedResponses() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m), "requestReplyRepliesDemo-out-0");

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
                "my-dest-my/p-arcs/the-event-after",
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
            outSink.next("second msg");
            outSink.complete();
        },"requestReplyRepliesDemo-out-0");

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
                            "my-dest-my/p-arcs/the-event-after",
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
                            "second msg",
                            aMsg.getPayload()
                    );
                    assertEquals(
                            "my-correlationId-my",
                            aMsg.getHeaders().get("correlationId")
                    );
                    assertEquals(
                            "my-dest-my/p-arcs/the-event-after",
                            aMsg.getHeaders().get(BinderHeaders.TARGET_DESTINATION)
                    );
                    assertEquals(
                            -1L,
                            aMsg.getHeaders().get("totalReplies")
                    );
                    assertEquals(
                            "1",
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
                            "my-dest-my/p-arcs/the-event-after",
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
    void wrapFlux_groupedResponses() {
        Function<Flux<Message<String>>, Flux<Message<String>>> supplier = supportService.wrapFlux((payloadIn, outSink) -> {
            outSink.next("first msg");
            outSink.next("second msg");
            outSink.complete();
        },"requestReplyRepliesDemo-out-0");

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
                            "my-dest-my/p-arcs/the-event-after",
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
                            "my-dest-my/p-arcs/the-event-after",
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
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> Collections.emptyList(), "requestReplyRepliesDemo-out-0");

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
                "my-dest-my/p-arcs/the-event-after",
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
                            "my-dest-my/p-arcs/the-event-after",
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
                "my-dest-my/p-arcs/the-event-after",
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
                "my-dest-my/p-arcs/the-event-after",
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
        },"requestReplyRepliesDemo-out-0");

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
                            "my-dest-my/p-arcs/the-event-after",
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
                            "my-dest-my/p-arcs/the-event-after",
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