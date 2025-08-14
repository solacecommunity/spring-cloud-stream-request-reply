package community.solace.spring.cloud.requestreply.examples.response.config;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DefaultMeterRegistry {
    // in case you decide to remove the actuator dependency, and if you have no meter registry, you can use for example the following meter registry:
    @Bean
    public MeterRegistry registry(Environment environments) {
        LoggingRegistryConfig config =
                key -> {
                    String property = switch (key) {
                        case "logging.enabled" -> "management.metrics.export.logging.enabled";
                        case "logging.step" -> "management.metrics.export.logging.step";
                        default -> null;
                    };

                    return
                            (null != property)
                            ? environments.getProperty(property)
                            : null;
                };

        return new LoggingMeterRegistry(config, Clock.SYSTEM);
    }
}
