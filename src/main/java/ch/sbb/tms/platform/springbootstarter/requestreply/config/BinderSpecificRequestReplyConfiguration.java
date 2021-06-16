package ch.sbb.tms.platform.springbootstarter.requestreply.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyBinderAdapter;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyBinderAdapterFactory;

/**
 * BinderSpecificRequestReplyConfiguration creates binder specific beans required in a request reply scenario
 */
@Configuration
@ConditionalOnBean(Binder.class)
@ConditionalOnProperty(prefix = "requestReply", name = "topic", havingValue = "", matchIfMissing = false)
@EnableConfigurationProperties(RequestReplyProperties.class)
public class BinderSpecificRequestReplyConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RequestReplyBinderAdapterFactory requestReplyBinderAdapterFactory() {
        return RequestReplyBinderAdapterFactory.getProxy();
    }

    @Bean
    @Autowired
    public RequestReplyBinderAdapter requestReplyBinderAdapter( //
            RequestReplyBinderAdapterFactory binderAdapterFactory, //
            Binder<?, ?, ?> binder, //
            RequestReplyProperties properties //
    ) {
        return binderAdapterFactory.createAdapter(binder, properties);
    }
}
