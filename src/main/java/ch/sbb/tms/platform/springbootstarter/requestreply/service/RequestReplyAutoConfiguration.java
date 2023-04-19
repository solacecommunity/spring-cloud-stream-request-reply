/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.util.function.Consumer;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SolaceHeaderParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionType;
import org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration;
import org.springframework.cloud.stream.config.BinderFactoryAutoConfiguration;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnClass({MessageChannel.class})
@ComponentScan("ch.sbb.tms.platform.springbootstarter.requestreply.service")
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
public class RequestReplyAutoConfiguration implements ApplicationContextInitializer<GenericApplicationContext> {
	private static final int SOLACE_CONFIGURERS_PRIORITY = 200;

	private static final Logger LOG = LoggerFactory.getLogger(RequestReplyAutoConfiguration.class);

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

	@Bean
	@ConditionalOnMissingBean
	public RequestReplyServiceImpl requestReplyService() {
		return new RequestReplyServiceImpl();
	}

	@Override
	public void initialize(final GenericApplicationContext context) {
		final BindResult<RequestReplyProperties> bindResult = Binder.get(context.getEnvironment())
				.bind("spring.cloud.stream.requestreply",
						RequestReplyProperties.class);

		if (!bindResult.isBound()) {
			return;
		}

		final RequestReplyProperties requestReplyProperties = bindResult.get();

		for (final String bindingName : requestReplyProperties.getBindingMappingNames()) {
			context.registerBean(
					bindingName,
					FunctionRegistration.class,
					() -> new FunctionRegistration<Consumer<Message<?>>>(msg ->
						((RequestReplyServiceImpl) context.getBean("requestReplyService")).onReplyReceived(msg)
					)
							.type(new FunctionType(
									ResolvableType.forClassWithGenerics(
											Consumer.class,
											ResolvableType.forClassWithGenerics(
													Message.class,
													Object.class
											)
									).getType()
							))
			);
			LOG.info("Register binding: " + bindingName + " for receiving replies");
		}
	}
}