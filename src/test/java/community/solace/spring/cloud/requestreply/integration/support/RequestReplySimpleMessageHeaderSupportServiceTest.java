package community.solace.spring.cloud.requestreply.integration.support;

import java.util.UUID;
import java.util.function.Function;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService.ThrowingFunction;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestReplySimpleMessageHeaderSupportServiceTest extends AbstractRequestReplySimpleIT {
    private static final ThrowingFunction<String, String> DUMMY_FUNCTION = o -> o;

    private RequestReplyMessageHeaderSupportService messageHeaderSupportService;
    private Function<Message<String>, Message<String>> testFunction;

    @Autowired
    protected void setMessageHeaderSupportService(RequestReplyMessageHeaderSupportService messageHeaderSupportService) {
        this.messageHeaderSupportService = messageHeaderSupportService;
        this.testFunction = messageHeaderSupportService.wrap(DUMMY_FUNCTION, (Class<Throwable>) null);
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
        Message<String> msg = MessageBuilder.withPayload("existingCorrelationIdShouldBePickedUpFromRequest")
                                            .setCorrelationId(UUID.randomUUID())
                                            .build();

        Message<String> result = testFunction.apply(msg);

        assertEquals(
                msg.getHeaders()
                   .get(IntegrationMessageHeaderAccessor.CORRELATION_ID)
                   .toString(),
                result.getHeaders()
                      .get(IntegrationMessageHeaderAccessor.CORRELATION_ID)
                      .toString()
        );
        assertNotEquals(
                msg.getHeaders()
                   .get(MessageHeaders.ID),
                result.getHeaders()
                      .get(IntegrationMessageHeaderAccessor.CORRELATION_ID)
        );
    }

    @Test
    void additionalHeadersDefinedShouldBeCopied() {
        String encodingHeaderKey = "encoding";
        String encodingHeaderValue = "ProtoBuf";
        String dummyHeaderKey = "dummy";
        Object dummyHeaderValue = UUID.randomUUID();
        String unknownHeaderKey = "unknownHeader";

        Message<String> msg = MessageBuilder.withPayload("existingCorrelationIdShouldBePickedUpFromRequest")
                                            .setHeader(encodingHeaderKey, encodingHeaderValue)
                                            .setHeader(dummyHeaderKey, dummyHeaderValue)
                                            .setHeader(unknownHeaderKey, UUID.randomUUID())
                                            .build();

        Message<String> result = testFunction.apply(msg);

        MessageHeaders messageHeaders = msg.getHeaders();
        MessageHeaders resultHeaders = result.getHeaders();

        assertEquals(messageHeaders.get(encodingHeaderKey), resultHeaders.get(encodingHeaderKey));
        assertEquals(messageHeaders.get(dummyHeaderKey), resultHeaders.get(dummyHeaderKey));
        assertFalse(resultHeaders.containsKey(unknownHeaderKey));
    }
}
