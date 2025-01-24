package community.solace.spring.cloud.requestreply.sampleapps.simple.configuration;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@Profile(AbstractRequestReplySimpleIT.PROFILE_TEST_SIMPLE)
@Configuration
@Import(TestChannelBinderConfiguration.class)
@EnableIntegration
@IntegrationComponentScan("community.solace.spring.cloud.requestreply.sampleapps.simple.integration")
public class RequestReplyTestConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
    }

}
