package ch.sbb.tms.platform.springbootstarter.requestreply.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BinderTopicMappings {
    private String binding;
    private String replyTopic;
    private Set<Pattern> topicPatterns;

    String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public String getReplyTopic() {
        return replyTopic;
    }

    public void setReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
    }

    public void setTopicPatterns(List<String> topicPatterns) {
        this.topicPatterns = topicPatterns.stream()
                .map(Pattern::compile)
                .collect(Collectors.toSet());
    }

    Set<Pattern> getTopicPatterns() {
        return topicPatterns == null ? Collections.emptySet() : topicPatterns;
    }
}
