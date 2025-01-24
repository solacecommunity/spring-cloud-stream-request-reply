package community.solace.spring.cloud.requestreply.service.logging;

import org.slf4j.event.Level;
import org.springframework.messaging.Message;

import org.slf4j.Logger;

public class DefaultRequestReplyLogger implements RequestReplyLogger {
    @Override
    public <T> void logRequest(Logger logger, Level suggestedLevel, String suggestedLogMessage, Message<T> message) {
        logger.atLevel(suggestedLevel).log(suggestedLogMessage, message);
    }

    @Override
    public <T> void logReply(Logger logger, Level suggestedLevel, String suggestedLogMessage, long remainingReplies, Message<T> message) {
        logger.atLevel(suggestedLevel).log(suggestedLogMessage, remainingReplies, message);
    }

    @Override
    public void log(Logger logger, Level suggestedLevel, String suggestedLogMessage, Object... formatArgs) {
        logger.atLevel(suggestedLevel).log(suggestedLogMessage, formatArgs);
    }
}
