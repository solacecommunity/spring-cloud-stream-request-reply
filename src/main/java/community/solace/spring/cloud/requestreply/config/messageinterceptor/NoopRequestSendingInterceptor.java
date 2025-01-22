package community.solace.spring.cloud.requestreply.config.messageinterceptor;

import community.solace.spring.cloud.requestreply.service.messageinterceptor.RequestSendingInterceptor;
import org.springframework.messaging.Message;

public class NoopRequestSendingInterceptor implements RequestSendingInterceptor {
    @Override
    public <T> Message<T> interceptRequestSendingMessage(Message<T> message, String bindingName) {
        return message;
    }
}
