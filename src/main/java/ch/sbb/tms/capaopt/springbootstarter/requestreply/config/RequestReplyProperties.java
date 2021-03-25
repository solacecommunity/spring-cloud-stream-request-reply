package ch.sbb.tms.capaopt.springbootstarter.requestreply.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("spring.cloud.stream.requestreply")
public class RequestReplyProperties {
    @NotEmpty
    private String binderName;

    @NotEmpty
    private String replyToQueueName;

    @NotEmpty
    private String requestReplyGroupName;

    public String getBinderName() {
        return binderName;
    }

    public void setBinderName(String binder) {
        this.binderName = binder;
    }

    public String getReplyToQueueName() {
        return replyToQueueName;
    }

    public void setReplyToQueueName(String replyToQueueName) {
        this.replyToQueueName = replyToQueueName;
    }

    public String getRequestReplyGroupName() {
        return requestReplyGroupName;
    }

    public void setRequestReplyGroupName(String requestReplyGroupName) {
        this.requestReplyGroupName = requestReplyGroupName;
    }
}
