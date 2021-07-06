package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer;

import javax.validation.constraints.NotEmpty;

import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultMessageBuilderConfigurer implements MessageBuilderConfigurer {
    @Override
    public RequestMessageBuilderAdapter adoptTo(MessageBuilder<?> messageBuilder) {
        return new DefaultRequestMessageBuilderAdapter(messageBuilder);
    }

    @Override
    public boolean appliesTo(Binder<?, ?, ?> binder) {
        return true; // fallback implementation for all binders
    }
}

class DefaultRequestMessageBuilderAdapter implements RequestMessageBuilderAdapter {
    private MessageBuilder<?> messageBuilder;

    public DefaultRequestMessageBuilderAdapter(MessageBuilder<?> messageBuilder) {
        this.messageBuilder = messageBuilder;
    }

    @Override
    public RequestMessageBuilderAdapter setCorrelationId(String correlationId) {
        messageBuilder.setCorrelationId(correlationId);
        return this;
    }

    @Override
    public RequestMessageBuilderAdapter setReplyToTopic(String topic) {
        messageBuilder.setHeader(MessageHeaders.REPLY_CHANNEL, topic);
        return this;
    }

    @Override
    public RequestMessageBuilderAdapter setDestination(@NotEmpty String requestDestination) {
        messageBuilder.setHeader(BinderHeaders.TARGET_DESTINATION, requestDestination);
        return this;
    }
}