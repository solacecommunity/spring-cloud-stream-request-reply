package community.solace.spring.cloud.requestreply.examples.response.config;

import community.solace.spring.cloud.requestreply.examples.response.service.BindingResolver;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ReplyToHeaderWrapInterceptorConfig {
    private final BindingResolver bindingResolver;

    @Bean
    public ReplyWrappingInterceptor replyWrappingInterceptor() {
        return new ReplyToHeaderWrapInterceptor(bindingResolver);
    }
}
