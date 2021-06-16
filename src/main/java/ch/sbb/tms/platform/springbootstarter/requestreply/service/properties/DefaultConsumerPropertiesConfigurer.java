package ch.sbb.tms.platform.springbootstarter.requestreply.service.properties;

import java.util.Map;

import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(Ordered.LOWEST_PRECEDENCE)
public class DefaultConsumerPropertiesConfigurer implements BinderConsumerPropertiesConfigurer<ConsumerProperties> {

    @Override
    public boolean appliesTo(Binder<?, ?, ?> binder) {
        return true; // fallback implementation for all binders
    }

    @Override
    public ConsumerProperties buildConsumerProperties(Map<String, Object> consumerProperties) {
        ConsumerProperties properties = new ConsumerProperties();
        populateProperties(consumerProperties, properties);
        return properties;
    }
}
