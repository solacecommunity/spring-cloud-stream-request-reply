package community.solace.spring.cloud.requestreply.examples.response.service;

import community.solace.spring.cloud.requestreply.examples.response.config.CustomHeaderConfig;
import community.solace.spring.cloud.requestreply.examples.response.config.CustomizedHeaderProperties;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BindingResolver {
    private final CustomizedHeaderProperties customizedHeaderProperties;

    public CustomHeaderConfig resolveCustomHeaderConfigByBindingName(String bindingName) {
        return this.customizedHeaderProperties.getCustomHeaderMapping(bindingName)
                                              .map(customHeaderMapping -> new CustomHeaderConfig(
                                                      customHeaderMapping.getReplyHeaderName()))
                                              .orElse(null);
    }
}
