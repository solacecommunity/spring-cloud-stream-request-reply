package community.solace.spring.cloud.requestreply.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.stream.requestreply")
public class RequestReplyProperties {
    private final List<String> copyHeadersOnWrap = new ArrayList<>();
    private List<BinderTopicMappings> bindingMapping = new ArrayList<>();
    private Map<String, String> variableReplacements = new HashMap<>();

    public List<String> getCopyHeadersOnWrap() {
        return Collections.unmodifiableList(copyHeadersOnWrap);
    }

    public void setCopyHeadersOnWrap(List<String> copyHeadersOnWrap) {
        this.copyHeadersOnWrap.clear();

        if (copyHeadersOnWrap != null) {
            this.copyHeadersOnWrap.addAll(copyHeadersOnWrap);
        }
    }

    public Set<String> getBindingMappingNames() {
        return bindingMapping.stream()
                .map(BinderTopicMappings::getBinding)
                .collect(Collectors.toSet());
    }

    public void setBindingMapping(List<BinderTopicMappings> bindingMapping) {
        this.bindingMapping = bindingMapping;
    }

    public Optional<BinderTopicMappings> getBindingMapping(String binding) {
        for (BinderTopicMappings mapping : bindingMapping) {
            if (Objects.equals(mapping.getBinding(), binding)) {
                return Optional.of(mapping);
            }
        }
        return Optional.empty();
    }

    public Optional<String> findMatchingBinder(String destination) {
        for (BinderTopicMappings mapping : bindingMapping) {
            for (Pattern topicPattern : mapping.getTopicPatterns()) {
                if (topicPattern.matcher(destination).matches()) {
                    return Optional.of(mapping.getBinding());
                }
            }
        }
        return Optional.empty();
    }

    public String replaceVariables(String topic) {
        if (variableReplacements == null) {
            return topic;
        }
        for (Map.Entry<String, String> varReplacement : variableReplacements.entrySet()) {
            topic = topic.replace("{" + varReplacement.getKey() + "}", varReplacement.getValue());
        }

        return topic;
    }

    public String replaceVariablesWithWildcard(String topic) {
        return topic.replaceAll("(\\{\\w+\\})", "*");
    }


    public void setVariableReplacements(Map<String, String> variableReplacements) {
        this.variableReplacements = variableReplacements;
    }
}
