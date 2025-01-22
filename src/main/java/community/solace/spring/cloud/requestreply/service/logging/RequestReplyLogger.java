package community.solace.spring.cloud.requestreply.service.logging;

import org.springframework.messaging.Message;

import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * If you want to make use of custom Logging functionality, create a bean implementing this interface.
 */
public interface RequestReplyLogger {
    /**
     * Is executed when a request message is sent. The suggested info parameters help to create your own custom log entry.
     * @param logger You can use this logger to have the surrounding class info available in the log.
     * @param suggestedLevel This is what the developer intended as log severity for an outgoing request.
     * @param suggestedLogMessage Formated message with prosa-text
     * @param message The actual message that is being sent, you can use this for logging some or all of the message content, headers etc.
     */
    <T> void logRequest(Logger logger, Level suggestedLevel, String suggestedLogMessage, Message<T> message);
    /**
     * Is executed when a reply message is received. The suggested info parameters help to create your own custom log entry.
     * @param logger You can use this logger to have the surrounding class info available in the log.
     * @param suggestedLevel This is what the developer intended as log severity for the incoming reply.
     * @param suggestedLogMessage Formated message with prosa-text
     * @param remainingReplies How many replies that are expected to follow this message
     * @param message The actual message that is being sent, you can use this for logging some or all of the message content, headers etc.
     */
    <T> void logReply(Logger logger, Level suggestedLevel, String suggestedLogMessage, long remainingReplies, Message<T> message);

    /**
     * For all log entries other than the request or reply standard behavior, this method is called.
     * Note: This is also called, if an exception is raised before a request could be sent out.
     * @param logger You can use this logger to have the surrounding class info available in the log.
     * @param suggestedLevel This is what the developer intended as log severity for the log.
     * @param suggestedLogMessage This is the raw message without replacements and contain formal placeholders i.e. {}
     * @param formatArgs The contents for the placeholders, with vararg.
     */
    void log(Logger logger, Level suggestedLevel, String suggestedLogMessage, Object... formatArgs);
}
