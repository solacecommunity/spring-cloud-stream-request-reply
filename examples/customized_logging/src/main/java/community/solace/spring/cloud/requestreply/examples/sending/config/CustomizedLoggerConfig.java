package community.solace.spring.cloud.requestreply.examples.sending.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import community.solace.spring.cloud.requestreply.service.logging.RequestReplyLogger;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class CustomizedLoggerConfig {
    private final ObjectMapper objectMapper;

    @Bean
    public RequestReplyLogger requestReplyLogger() {
        return new CustomizedLogger(objectMapper);
    }
}
