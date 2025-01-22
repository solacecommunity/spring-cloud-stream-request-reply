package community.solace.spring.cloud.requestreply.examples.sending.config;

import community.solace.spring.cloud.requestreply.examples.sending.service.BindingResolver;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.RequestSendingInterceptor;
import lombok.RequiredArgsConstructor;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

@RequiredArgsConstructor
public class CustomizedReplyToHeaderInterceptor implements RequestSendingInterceptor {
    private final BindingResolver bindingResolver;

    @Override
    public <T> Message<T> interceptRequestSendingMessage(Message<T> message, String bindingName) {
        MessageBuilder<T> messageBuilder = MessageBuilder.fromMessage(message);
        CustomHeaderConfig customHeaderConfig = bindingResolver.resolveCustomHeaderConfigByBindingName(bindingName);
        if (null != customHeaderConfig && null != customHeaderConfig.replyHeaderName()) {
            messageBuilder.setHeader(customHeaderConfig.replyHeaderName(),
                                     message.getHeaders()
                                            .get(MessageHeaders.REPLY_CHANNEL));
        }
        return messageBuilder.build();
    }
}
