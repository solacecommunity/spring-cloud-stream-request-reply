package community.solace.spring.cloud.requestreply.sampleapps.logging.configuration;

import community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@Profile(AbstractRequestReplyLoggingIT.PROFILE_TEST_LOGGING)
@Configuration
@Import(TestChannelBinderConfiguration.class)
@EnableIntegration
@IntegrationComponentScan("community.solace.spring.cloud.requestreply.sampleapps.logging.integration")
public class RequestReplyTestConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {

    @Override
    public void initialize(GenericApplicationContext applicationContext) {
    }

}
