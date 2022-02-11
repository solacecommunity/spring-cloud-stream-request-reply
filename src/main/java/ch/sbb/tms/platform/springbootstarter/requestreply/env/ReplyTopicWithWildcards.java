package ch.sbb.tms.platform.springbootstarter.requestreply.env;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.BinderTopicMappings;
import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;

public class ReplyTopicWithWildcards {
    String replaceWithWildcards(String bindingName, String wildcard, ConfigurableEnvironment environment) {
        RequestReplyProperties requestReplyProperties = Binder.get(environment)
                .bind("spring.cloud.stream.requestreply", RequestReplyProperties.class)
                .get();
        BinderTopicMappings bindingMapping = requestReplyProperties.getBindingMapping(bindingName)
                .orElseThrow(() -> new IllegalArgumentException("replyTopicWithWildcards: Missing binding mapping for: " + bindingName + ". "
                        + "Please check that there is a matching: spring.cloud.stream.requestreply.bindingMapping[].binding"));

        return bindingMapping.getReplyTopic().replaceAll("\\{\\w+\\}", wildcard);
    }
}
