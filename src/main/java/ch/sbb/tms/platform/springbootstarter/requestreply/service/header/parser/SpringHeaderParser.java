package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errorMessage.MessageErrorMessageParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errorMessage.MessageHeaderErrorMessageParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.totalReplies.MessageHeaderTotalRepliesParser;

@Service
@Order(40000)
public class SpringHeaderParser implements MessageHeaderReplyToParser, MessageHeaderTotalRepliesParser, MessageHeaderErrorMessageParser {

    public  static String MULTI_TOTAL_REPLIES = "totalReplies";
    public  static String MULTI_REPLY_INDEX = "replyIndex";
    public  static String ERROR_MESSAGE = "errorMessage";

    @Override
    public String getReplyTo(MessageHeaders headers) {
        return headers.get(MessageHeaders.REPLY_CHANNEL, String.class);
    }

    @Override
    public Integer getTotalReplies(MessageHeaders headers) {
        Object replies = headers.get(MULTI_TOTAL_REPLIES);
        if (replies == null) {
            return null;
        }
        if (replies instanceof Integer) {
            return (Integer) replies;
        }
        if (replies instanceof Long) {
            return Math.toIntExact((Long) replies);
        }
        if (replies instanceof String) {
            return Integer.parseInt((String) replies);
        }
        throw new IllegalArgumentException("Invalid datatype for " + MULTI_TOTAL_REPLIES + ": "+ replies.getClass().getName() + " == " + replies);
    }

    @Override
    public String getErrorMessage(MessageHeaders headers) {
        Object errorMessage = headers.get(ERROR_MESSAGE);
        if (errorMessage instanceof String) {
            return (String) errorMessage;
        }

        return null;
    }
}
