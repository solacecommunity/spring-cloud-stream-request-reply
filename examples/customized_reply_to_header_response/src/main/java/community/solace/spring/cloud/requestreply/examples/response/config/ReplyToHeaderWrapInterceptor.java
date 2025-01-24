package community.solace.spring.cloud.requestreply.examples.response.config;

import community.solace.spring.cloud.requestreply.config.messageinterceptor.NoopReplyWrappingInterceptor;
import community.solace.spring.cloud.requestreply.examples.response.service.BindingResolver;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

@Slf4j
@RequiredArgsConstructor
public class ReplyToHeaderWrapInterceptor implements ReplyWrappingInterceptor {

    private final BindingResolver bindingResolver;

    @Override
    public <T> Message<T> interceptReplyWrappingPayloadMessage(Message<T> message, String bindingName) {
        MessageBuilder<T> messageBuilder = MessageBuilder.fromMessage(message);
        messageBuilder.setHeader("customTestHeader", message.getHeaders()
                                                            .get(bindingResolver.resolveCustomHeaderConfigByBindingName(bindingName)
                                                                                .replyHeaderName()));
        log.info("customTestHeader is: " + message.getHeaders()
                                                  .get(bindingResolver.resolveCustomHeaderConfigByBindingName(bindingName)
                                                                      .replyHeaderName()));
        return messageBuilder.build();
    }

    @Override
    public <T> Message<T> interceptReplyWrappingFinishingEmptyMessage(Message<T> message, String bindingName) {
        return new NoopReplyWrappingInterceptor().interceptReplyWrappingFinishingEmptyMessage(message, bindingName);
    }

    @Override
    public <T> Message<T> interceptReplyWrappingErrorMessage(Message<T> message, String bindingName) {
        return new NoopReplyWrappingInterceptor().interceptReplyWrappingErrorMessage(message, bindingName);
    }
}
