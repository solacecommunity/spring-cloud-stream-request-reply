package ch.sbb.tms.capaopt.springbootstarter.requestreply.configuration;

import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.AbstractRequestReplyIT.PROFILE_TEST;

@Profile(PROFILE_TEST)
@Configuration
@Import(TestChannelBinderConfiguration.class)
@EnableIntegration
@IntegrationComponentScan("ch.sbb.tms.capaopt.springbootstarter.requestreply.integration")
public class RequestReplyTestConfiguration {

}
