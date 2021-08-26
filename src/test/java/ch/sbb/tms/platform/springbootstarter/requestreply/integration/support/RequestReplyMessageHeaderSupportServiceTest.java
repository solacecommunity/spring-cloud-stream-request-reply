package ch.sbb.tms.platform.springbootstarter.requestreply.integration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;

class RequestReplyMessageHeaderSupportServiceTest extends AbstractRequestReplyIT {
    private static final UnaryOperator<Object> DUMMY_FUNCTION = o -> o;

    private RequestReplyMessageHeaderSupportService messageHeaderSupportService;
    private Function<Message<Object>, Message<Object>> testFunction;

    @Autowired
    protected void setMessageHeaderSupportService(RequestReplyMessageHeaderSupportService messageHeaderSupportService) {
        this.messageHeaderSupportService = messageHeaderSupportService;
        this.testFunction = messageHeaderSupportService.wrap(DUMMY_FUNCTION);
    }

    @Test
    void retrievingCorrelationIdFromNullMessageShouldReturnNull() {
        assertNull(messageHeaderSupportService.getCorrelationId(null));
    }

    @Test
    void retrievingDestinationFromNullMessageShouldReturnNull() {
        assertNull(messageHeaderSupportService.getDestination(null));
    }

    @Test
    void retrievingReplyToDestinationFromNullMessageShouldReturnNull() {
        assertNull(messageHeaderSupportService.getReplyTo(null));
    }

    @Test
    void existingCorrelationIdShouldBePickedUpFromRequest() {
        Message msg = MessageBuilder.withPayload("existingCorrelationIdShouldBePickedUpFromRequest") //
                .setCorrelationId(UUID.randomUUID()) //
                .build();

        Message result = testFunction.apply(msg);

        assertEquals( //
                msg.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID).toString(), //
                result.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID).toString() //
        );
        assertNotEquals( //
                msg.getHeaders().get(MessageHeaders.ID).toString(), //
                result.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID).toString() //
        );
    }
}
