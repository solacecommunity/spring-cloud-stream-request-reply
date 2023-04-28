package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.errormessage;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@FunctionalInterface
public interface MessageHeaderErrorMessageParser extends MessageErrorMessageParser {
    @Override
    default String getErrorMessage(Message<?> message) {
        return message == null ?
                null :
                getErrorMessage(message.getHeaders());
    }

    @Nullable
    String getErrorMessage(MessageHeaders headers);
}
