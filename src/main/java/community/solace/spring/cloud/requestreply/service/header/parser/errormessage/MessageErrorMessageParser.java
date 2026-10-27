package community.solace.spring.cloud.requestreply.service.header.parser.errormessage;

import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageErrorMessageParser {
    String getErrorMessage(Message<?> message);
}
