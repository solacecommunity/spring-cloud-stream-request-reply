package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SpringHeaderParser;
import org.junit.jupiter.api.Test;

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
                167,
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
    void wrapList() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> List.of(m, m));

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
                2
        );
        assertEquals(
                answers.get(0).getHeaders().get("replyIndex"),
                0
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
                2,
                answers.get(1).getHeaders().get("totalReplies")
        );
        assertEquals(
                1,
                answers.get(1).getHeaders().get("replyIndex")
        );
    }

    @Test
    void wrapList_emptyList_shouldCreateMessageWithTotalRepliesNull() {
        Function<Message<String>, List<Message<String>>> supplier = supportService.wrapList(m -> Collections.emptyList());

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
                0,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                0,
                answers.get(0).getHeaders().get("replyIndex")
        );
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
                0,
                answers.get(0).getHeaders().get("totalReplies")
        );
        assertEquals(
                0,
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