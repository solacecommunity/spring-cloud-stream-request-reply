package ch.sbb.tms.platform.springbootstarter.requestreply.service.properties;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.binder.ConsumerProperties;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.BinderSpecificConfigurer;

public interface BinderConsumerPropertiesConfigurer<T extends ConsumerProperties> extends BinderSpecificConfigurer {
    static final Logger LOG = LoggerFactory.getLogger(BinderConsumerPropertiesConfigurer.class);

    T buildConsumerProperties(Map<String, Object> consumerProperties);

    default void populateProperties(Map<String, Object> consumerProperties, T target) {
        if (consumerProperties == null || target == null) {
            return;
        }

        consumerProperties.entrySet().stream().forEach(e -> mapToTarget(e.getKey(), e.getValue(), target));
    }

    @SuppressWarnings("unchecked")
    private void mapToTarget(String propertyName, Object propertyValue, Object target) {
        try {
            if (propertyValue instanceof Map) {
                if (propertyValue != null) {
                    ((Map<String, Object>) propertyValue).entrySet().stream() //
                            .forEach(e -> mapToTarget(propertyName + "." + e.getKey(), e.getValue(), target)) //
                    ;
                }
            }
            else {
                BeanUtils.setProperty(target, propertyName, propertyValue);
            }
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            LOG.warn("Failed to set property {} to {} on target {}: {}", propertyName, propertyValue, target, e.getMessage());
        }
    }
}
