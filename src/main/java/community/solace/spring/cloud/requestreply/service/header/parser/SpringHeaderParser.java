package community.solace.spring.cloud.requestreply.service.header.parser;

import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.MessageHeaderErrorMessageParser;
import community.solace.spring.cloud.requestreply.service.header.parser.replyto.MessageHeaderReplyToParser;
import community.solace.spring.cloud.requestreply.service.header.parser.totalreplies.MessageHeaderTotalRepliesParser;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

@Service
@Order(40000)
public class SpringHeaderParser implements MessageHeaderReplyToParser, MessageHeaderTotalRepliesParser, MessageHeaderErrorMessageParser {
    public final static String MULTI_TOTAL_REPLIES = "totalReplies";
    public final static String MULTI_REPLY_INDEX = "replyIndex";
    public final static String GROUPED_MESSAGES = "groupedMessages";
    public final static String GROUPED_CONTENT_TYPE = "groupedContentType";
    public final static String ERROR_MESSAGE = "errorMessage";

    @Override
    public String getReplyTo(MessageHeaders headers) {
        return headers.get(MessageHeaders.REPLY_CHANNEL, String.class);
    }

    @Override
    public Long getTotalReplies(MessageHeaders headers) {
        Object replies = headers.get(MULTI_TOTAL_REPLIES);
        if (replies == null) {
            return null;
        }
        if (replies instanceof Integer) {
            return ((Integer) replies).longValue();
        }
        if (replies instanceof Long) {
            return (Long) replies;
        }
        if (replies instanceof String) {
            return Long.parseLong((String) replies);
        }
        throw new IllegalArgumentException("Invalid datatype for " + MULTI_TOTAL_REPLIES + ": " + replies.getClass()
                .getName() + " == " + replies);
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
