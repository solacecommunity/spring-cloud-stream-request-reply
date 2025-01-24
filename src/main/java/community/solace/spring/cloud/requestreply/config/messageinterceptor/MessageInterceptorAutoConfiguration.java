package community.solace.spring.cloud.requestreply.config.messageinterceptor;

import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.RequestSendingInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageInterceptorAutoConfiguration {
    @ConditionalOnMissingBean(RequestSendingInterceptor.class)
    @Bean
    public RequestSendingInterceptor requestSendingInterceptor() {
        return new NoopRequestSendingInterceptor();
    }

    @ConditionalOnMissingBean(ReplyWrappingInterceptor.class)
    @Bean
    public ReplyWrappingInterceptor replySendingInterceptor() {
        return new NoopReplyWrappingInterceptor();
    }
}
