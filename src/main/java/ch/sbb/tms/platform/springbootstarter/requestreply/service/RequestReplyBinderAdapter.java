package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.cloud.stream.binder.ConsumerProperties;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties.Period;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer.MessageBuilderConfigurer;

/**
 * The RequestReplyBinderAdapter provides binder specific functionality and configuration to the {@link RequestReplyServiceImpl}
 *
 */
public interface RequestReplyBinderAdapter {
    @NotNull
    MessageBuilderConfigurer getMessagebuilderConfigurer();

    @NotEmpty
    default String getReplyTopic() {
        return getProperties().getTopic();
    }

    @NotNull
    @Valid
    default Period getTimeoutPeriod() {
        return getProperties().getTimeout();
    }

    RequestReplyProperties getProperties();

    ConsumerProperties buildConsumerProperties(Map<String, Object> consumerProperties);
}
