/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SolaceHeaderParser;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnClass({MessageChannel.class})
@ComponentScan("ch.sbb.tms.platform.springbootstarter.requestreply.service")
public class RequestReplyAutoConfiguration {
    private static final int SOLACE_CONFIGURERS_PRIORITY = 200;

    @Bean
    @ConditionalOnMissingBean
    public RequestReplyService requestReplyService() {
        return new RequestReplyService();
    }

    @Bean
    @Order(SOLACE_CONFIGURERS_PRIORITY)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = {
            "com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders",
            "com.solacesystems.jcsmp.Destination"
    })
    public SolaceHeaderParser solaceHeaderParser() {
        return new SolaceHeaderParser();
    }
}