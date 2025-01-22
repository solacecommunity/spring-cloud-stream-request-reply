package community.solace.spring.cloud.requestreply.config.logging;

import community.solace.spring.cloud.requestreply.service.logging.DefaultRequestReplyLogger;
import community.solace.spring.cloud.requestreply.service.logging.RequestReplyLogger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggerAutoConfiguration {

    @ConditionalOnMissingBean(RequestReplyLogger.class)
    @Bean
    public RequestReplyLogger requestReplyLogger() {
        return new DefaultRequestReplyLogger();
    }
}
