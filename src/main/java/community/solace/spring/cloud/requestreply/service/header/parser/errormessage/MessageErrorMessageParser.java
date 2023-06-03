package community.solace.spring.cloud.requestreply.service.header.parser.errormessage;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

@FunctionalInterface
public interface MessageErrorMessageParser {
    @Nullable
    String getErrorMessage(Message<?> message);
}
