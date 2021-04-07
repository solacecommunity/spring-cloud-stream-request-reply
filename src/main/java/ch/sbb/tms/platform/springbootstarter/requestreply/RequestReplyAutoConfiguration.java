/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnClass({ MessageChannel.class })
@EnableConfigurationProperties({ RequestReplyProperties.class })
public class RequestReplyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.cloud.stream.requestreply.binderName", matchIfMissing = false, havingValue = "")
    public RequestReplyService requestReplyService() {
        return new RequestReplyService();
    }
}