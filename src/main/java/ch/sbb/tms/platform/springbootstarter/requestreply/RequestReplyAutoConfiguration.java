/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.config.BindingBeansRegistrar;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageChannel;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.BinderSpecificRequestReplyConfiguration;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyService;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer.SolaceMessageBuilderConfigurer;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SolaceHeaderParser;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.properties.SolaceConsumerPropertiesConfigurer;

@Configuration
@ConditionalOnClass({ MessageChannel.class })
@Import({ BindingBeansRegistrar.class, BinderFactoryAutoConfiguration.class })
@AutoConfigureAfter({ ContextFunctionCatalogAutoConfiguration.class, PropertySourcesPlaceholderConfigurer.class })
@AutoConfigureBefore({ FunctionConfiguration.class })
@Order(Ordered.LOWEST_PRECEDENCE)
@ComponentScan("ch.sbb.tms.platform.springbootstarter.requestreply.service")
public class RequestReplyAutoConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {
    private static final int SOLACE_CONFIGURERS_PRIORITY = 200;

    @Bean
    @ConditionalOnMissingBean
    public RequestReplyService requestReplyService() {
        return new RequestReplyService();
    }

    @Bean
    @Order(SOLACE_CONFIGURERS_PRIORITY)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = { //
            "com.solace.spring.cloud.stream.binder.SolaceMessageChannelBinder", //
            "com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties" //
    })
    public SolaceConsumerPropertiesConfigurer solaceConsumerPropertiesConfigurer() {
        return new SolaceConsumerPropertiesConfigurer();
    }

    @Bean
    @Order(SOLACE_CONFIGURERS_PRIORITY)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = { //
            "com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders", //
            "com.solacesystems.jcsmp.Destination" //
    })
    public SolaceHeaderParser solaceHeaderParser() {
        return new SolaceHeaderParser();
    }

    @Bean
    @Order(SOLACE_CONFIGURERS_PRIORITY)
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = { //
            "com.solace.spring.cloud.stream.binder.SolaceMessageChannelBinder", //
            "com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders", //
            "com.solacesystems.jcsmp.JCSMPFactory" //
    })
    public SolaceMessageBuilderConfigurer solaceMessageBuilderConfigurer() {
        return new SolaceMessageBuilderConfigurer();
    }

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
        RequestReplyBeanRegistrar registrar = new RequestReplyBeanRegistrar(applicationContext);
        applicationContext.addBeanFactoryPostProcessor(registrar); // note: registrar will also be triggered for binder subcontexts
    }

    private static class RequestReplyBeanRegistrar implements BeanDefinitionRegistryPostProcessor {
        private GenericApplicationContext applicationContext;

        public RequestReplyBeanRegistrar(GenericApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) {
            // nothing to do here
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry arg0) {
            // for global and binder scope configure request reply beans if preconditions are met
            applicationContext.registerBeanDefinition( //
                    "requestReplyBinderSpecificConfiguration", //
                    BeanDefinitionBuilder //
                            .genericBeanDefinition(BinderSpecificRequestReplyConfiguration.class) //
                            .getBeanDefinition() //
            );
        }
    }
}