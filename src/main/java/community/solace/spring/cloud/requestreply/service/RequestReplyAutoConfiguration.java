package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.config.RequestReplyProperties;
import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnClass({MessageChannel.class})
@ComponentScan("community.solace.spring.cloud.requestreply.service")
@Import(RequestReplyFunctionRegistrar.class)
@AutoConfigureAfter({
        ContextFunctionCatalogAutoConfiguration.class,
        PropertySourcesPlaceholderConfigurer.class,
        BinderFactoryAutoConfiguration.class
})
@AutoConfigureBefore({
        FunctionConfiguration.class
})
@Order()
@EnableConfigurationProperties(RequestReplyProperties.class)
public class RequestReplyAutoConfiguration {
    private static final int SOLACE_CONFIGURERS_PRIORITY = 200;

    @Bean
    @Order(SOLACE_CONFIGURERS_PRIORITY)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = {
            "com.solacesystems.jcsmp.Destination"
    })
    public SolaceHeaderParser solaceHeaderParser() {
        return new SolaceHeaderParser();
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestReplyServiceImpl requestReplyService() {
        return new RequestReplyServiceImpl();
    }
}