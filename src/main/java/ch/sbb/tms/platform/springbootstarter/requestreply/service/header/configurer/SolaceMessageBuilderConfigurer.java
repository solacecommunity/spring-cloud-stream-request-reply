package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;

import com.solace.spring.cloud.stream.binder.SolaceMessageChannelBinder;
import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solacesystems.jcsmp.JCSMPFactory;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyBinderAdapterFactory;

public class SolaceMessageBuilderConfigurer implements MessageBuilderConfigurer {
    @Autowired
    private RequestReplyBinderAdapterFactory adapterFactory;
    
    Map<String, Optional<String>> destinationLookupCache = new HashMap<>();

    @Override
    public RequestMessageBuilderAdapter adoptTo(MessageBuilder<?> messageBuilder) {
        return new SolaceRequestMessageBuilderAdapter(messageBuilder);
    }

    @Override
    public boolean appliesTo(Binder<?, ?, ?> binder) {
        return binder instanceof SolaceMessageChannelBinder;
    }

    class SolaceRequestMessageBuilderAdapter implements RequestMessageBuilderAdapter {
        private MessageBuilder<?> messageBuilder;

        public SolaceRequestMessageBuilderAdapter(MessageBuilder<?> messageBuilder) {
            this.messageBuilder = messageBuilder;
        }

        @Override
        public RequestMessageBuilderAdapter setCorrelationId(String correlationId) {
            messageBuilder.setHeader(SolaceHeaders.CORRELATION_ID, correlationId);
            return this;
        }

        @Override
        public RequestMessageBuilderAdapter setReplyToTopic(String topic) {
            messageBuilder.setHeader(SolaceHeaders.REPLY_TO, JCSMPFactory.onlyInstance().createTopic(topic));
            return this;
        }

        @Override
        public SolaceRequestMessageBuilderAdapter setDestination(@NotEmpty String requestDestination) {
            messageBuilder.setHeader(BinderHeaders.TARGET_DESTINATION, getDestination(requestDestination));
            return this;
        }

        private String getDestination(@NotEmpty String requestDestination) {
            return destinationLookupCache.computeIfAbsent(requestDestination,
                    d -> adapterFactory.getBindingDestination(requestDestination)).orElse(requestDestination);
        }
    }
}
