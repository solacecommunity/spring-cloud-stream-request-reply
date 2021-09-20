/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.sampleapp.springbootstarter.requestreply.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;

@Profile(AbstractRequestReplyIT.PROFILE_TEST)
@Configuration
@Import(TestChannelBinderConfiguration.class)
@EnableIntegration
@IntegrationComponentScan("ch.sbb.tms.platform.sampleapp.springbootstarter.requestreply.integration")
public class RequestReplyTestConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {

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
        public void postProcessBeanFactory(ConfigurableListableBeanFactory arg0) throws BeansException {
            // nothing to do here
        }

        @Override
        public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry arg0) throws BeansException {
            // for global and binder scope configure request reply beans if preconditions are met
            applicationContext.registerBeanDefinition( //
                    "requestReplyBinderSpecificTestConfiguration", //
                    BeanDefinitionBuilder //
                            .genericBeanDefinition(RequestReplyTestBinderConfiguration.class) //
                            .getBeanDefinition() //
            );
        }
    }
}
