package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer;

import org.springframework.integration.support.MessageBuilder;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.BinderSpecificConfigurer;

public interface MessageBuilderConfigurer extends BinderSpecificConfigurer {
    RequestMessageBuilderAdapter adoptTo(MessageBuilder<?> messageBuilder);
}
