package community.solace.spring.cloud.requestreply.examples.sending.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import community.solace.spring.cloud.requestreply.examples.sending.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.logging.RequestReplyLogger;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import org.springframework.messaging.Message;

@RequiredArgsConstructor
public class CustomizedLogger implements RequestReplyLogger {
    private final ObjectMapper objectMapper;

    @Override
    public <T> void logRequest(Logger logger, Level suggestedLevel, String suggestedLogMessage, Message<T> message) {
        logger.atLevel(Level.DEBUG)
              .log("<<< {} {}", message.getPayload(), message.getHeaders());
    }

    @Override
    @SneakyThrows
    public <T> void logReply(Logger logger, Level suggestedLevel, String suggestedLogMessage, long remainingReplies, Message<T> message) {
        String payloadString = new String((byte[]) message.getPayload());
        SensorReading sensorReading = objectMapper.readValue(payloadString, SensorReading.class);
        logger.atLevel(Level.DEBUG)
              .log(">>> {} {} remaining replies: {}", sensorReading, message.getHeaders(), remainingReplies);
    }

    @Override
    public void log(Logger logger, Level suggestedLevel, String suggestedLogMessage, Object... formatArgs) {
        logger.atLevel(suggestedLevel)
              .log(suggestedLogMessage, formatArgs);
    }
}
