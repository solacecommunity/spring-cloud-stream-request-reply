package ch.sbb.tms.platform.sampleapp.springbootstarter.requestreply.configuration;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.BinderSpecificRequestReplyConfiguration;
import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.ReplyMessageChannel;

/**
 * <p>SCS TestChannelBinder does not populate RequestReplyProperties configured through environment
 * at main_session binder, hence the underlying {@link BinderSpecificRequestReplyConfiguration} is not
 * triggered due to {@link ConditionalOnProperty} constraint.</p>
 * <p>The <code>RequestReplyTestBinderConfiguration</code> extends that configuration and provides
 * mock properties to have it properly configured.</p>
 */
@Configuration
@ConditionalOnBean(Binder.class)
public class RequestReplyTestBinderConfiguration extends BinderSpecificRequestReplyConfiguration {
    @Bean
    @Primary
    @ConditionalOnMissingBean
    public RequestReplyProperties requestReplyProperties() {
        RequestReplyProperties properties = new RequestReplyProperties();

        properties.setTopic(ReplyMessageChannel.CHANNEL_NAME);
        properties.setGroup("requestreply-integrationtest");

        // allow some time for debugging
        properties.getTimeout().setDuration(5l);
        properties.getTimeout().setUnit(TimeUnit.MINUTES);

        return properties;
    }
}
