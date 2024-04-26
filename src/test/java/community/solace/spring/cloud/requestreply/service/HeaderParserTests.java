package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.AbstractRequestReplyIT;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import community.solace.spring.cloud.requestreply.service.header.parser.correlationid.MessageCorrelationIdParser;
import community.solace.spring.cloud.requestreply.service.header.parser.destination.MessageDestinationParser;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.MessageErrorMessageParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageReplyToParser;
import community.solace.spring.cloud.requestreply.service.header.parser.totalreplies.MessageTotalRepliesParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.List;

import static community.solace.spring.cloud.requestreply.service.header.parser.HttpHeaderParser.HTTP_HEADER_CORRELATION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeaderParserTests extends AbstractRequestReplyIT {
    @Autowired
    RequestReplyMessageHeaderSupportService headerSupport;

    @Autowired
    private List<MessageCorrelationIdParser> messageCorrelationIdParsers;

    @Autowired
    private List<MessageDestinationParser> messageDestinationParsers;

    @Autowired
    private List<MessageReplyToParser> messageReplyToParsers;

    @Autowired
    private List<MessageTotalRepliesParser> messageTotalRepliesParsers;

    @Autowired
    private List<MessageErrorMessageParser> messageErrorMessageParsers;

    @Test
    void correlationIdParsersShouldNotThrowExceptionWhenGivenNull() {
        for (MessageCorrelationIdParser parser : messageCorrelationIdParsers) {
            assertNull(parser.getCorrelationId(null), String.format("%s can not handle null values", parser));
        }
    }

    @Test
    void destinationParsersShouldNotThrowExceptionWhenGivenNull() {
        for (MessageDestinationParser parser : messageDestinationParsers) {
            assertNull(parser.getDestination(null), String.format("%s can not handle null values", parser));
        }
    }

    @Test
    void replyToParsersShouldNotThrowExceptionWhenGivenNull() {
        for (MessageReplyToParser parser : messageReplyToParsers) {
            assertNull((parser).getReplyTo(null), String.format("%s can not handle null values", parser));
        }
    }

    @Test
    void totalRepliesParsersShouldNotThrowExceptionWhenGivenNull() {
        for (MessageTotalRepliesParser parser : messageTotalRepliesParsers) {
            assertNull((parser).getTotalReplies(null), String.format("%s can not handle null values", parser));
        }
    }

    @Test
    void errorMessageParsersShouldNotThrowExceptionWhenGivenNull() {
        for (MessageErrorMessageParser parser : messageErrorMessageParsers) {
            assertNull((parser).getErrorMessage(null), String.format("%s can not handle null values", parser));
        }
    }

    @Test
    void headerParserPrecedenceIsObeyed() {
        MessageBuilder<String> mb = MessageBuilder.withPayload("headerParserPrecedenceIsObeyed");

        String correlationId1 = "93205362-d3ea-4fb7-9fa9-correlationId1";
        mb.setHeader(HTTP_HEADER_CORRELATION_ID, correlationId1);
        Message<String> message = mb.build();
        assertEquals(correlationId1, headerSupport.getCorrelationId(message));

        String correlationId2 = "93205362-d3ea-4fb7-9fa9-correlationId2";
        mb.setCorrelationId(correlationId2);
        message = mb.build();
        assertEquals(correlationId2, headerSupport.getCorrelationId(message));

        String correlationId3 = "93205362-d3ea-4fb7-9fa9-correlationId3";
        mb.setHeader("solace_correlationId"/*SolaceHeaders.CORRELATION_ID*/, correlationId3);
        message = mb.build();
        assertEquals(correlationId3, headerSupport.getCorrelationId(message));

        assertEquals(correlationId2, message.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID));
        assertEquals(correlationId1, message.getHeaders().get(HTTP_HEADER_CORRELATION_ID));
    }
}
