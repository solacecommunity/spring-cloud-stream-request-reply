package ch.sbb.tms.platform.springbootstarter.requestreply.service.header;

import java.util.function.Function;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                () -> "Random UUID4 should be provided"
        );
    }

}