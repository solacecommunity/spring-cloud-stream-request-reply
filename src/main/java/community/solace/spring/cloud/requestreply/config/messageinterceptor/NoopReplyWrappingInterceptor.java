package community.solace.spring.cloud.requestreply.config.messageinterceptor;

import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import org.springframework.messaging.Message;

public class NoopReplyWrappingInterceptor implements ReplyWrappingInterceptor {
    @Override
    public <T> Message<T> interceptReplyWrappingPayloadMessage(Message<T> message, String bindingName) {
        return message;
    }

    @Override
    public <T> Message<T> interceptReplyWrappingFinishingEmptyMessage(Message<T> message, String bindingName) {
        return message;
    }

    @Override
    public <T> Message<T> interceptReplyWrappingErrorMessage(Message<T> message, String bindingName) {
        return message;
    }
}
