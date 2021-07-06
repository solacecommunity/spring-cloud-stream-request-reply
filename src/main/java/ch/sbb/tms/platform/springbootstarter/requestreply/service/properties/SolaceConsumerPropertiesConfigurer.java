package ch.sbb.tms.platform.springbootstarter.requestreply.service.properties;

import java.util.Map;

import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;

import com.solace.spring.cloud.stream.binder.SolaceMessageChannelBinder;
import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;

public class SolaceConsumerPropertiesConfigurer
        implements BinderConsumerPropertiesConfigurer<ExtendedConsumerProperties<SolaceConsumerProperties>> {

    @Override
    public boolean appliesTo(Binder<?, ?, ?> binder) {
        return binder instanceof SolaceMessageChannelBinder;
    }

    @Override
    public ExtendedConsumerProperties<SolaceConsumerProperties> buildConsumerProperties(Map<String, Object> consumerProperties) {
        SolaceConsumerProperties extension = new SolaceConsumerProperties();
        ExtendedConsumerProperties<SolaceConsumerProperties> properties = new ExtendedConsumerProperties<SolaceConsumerProperties>(
                extension);

        populateProperties(consumerProperties, properties);
        return properties;
    }
}
