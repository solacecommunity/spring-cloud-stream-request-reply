package community.solace.spring.cloud.requestreply.examples.sending.config;

import community.solace.spring.cloud.requestreply.examples.sending.service.BindingResolver;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.RequestSendingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class CustomizedReplyToHeaderInterceptorConfig {
    private final BindingResolver bindingResolver;

    @Bean
    public RequestSendingInterceptor requestSendingInterceptor() {
        return new CustomizedReplyToHeaderInterceptor(bindingResolver);
    }
}
