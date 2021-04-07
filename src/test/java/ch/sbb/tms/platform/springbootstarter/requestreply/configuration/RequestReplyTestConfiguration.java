/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.configuration;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;

import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.config.EnableIntegration;

@Profile(AbstractRequestReplyIT.PROFILE_TEST)
@Configuration
@Import(TestChannelBinderConfiguration.class)
@EnableIntegration
@IntegrationComponentScan("ch.sbb.tms.platform.springbootstarter.requestreply.integration")
public class RequestReplyTestConfiguration {

}
