package community.solace.spring.cloud.requestreply.examples.sending.config;

import java.util.List;
import java.util.Optional;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("example")
public class CustomizedHeaderProperties {
    private List<CustomHeaderMapping> customHeaderMappings;

    public Optional<CustomHeaderMapping> getCustomHeaderMapping(String bindingName) {
        if (null == customHeaderMappings) {
            return Optional.empty();
        }
        for (CustomHeaderMapping mapping : customHeaderMappings) {
            if (mapping.getBinding()
                       .equals(bindingName)) {
                return Optional.of(mapping);
            }
        }
        return Optional.empty();
    }
}
