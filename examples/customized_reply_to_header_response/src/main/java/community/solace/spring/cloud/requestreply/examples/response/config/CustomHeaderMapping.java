package community.solace.spring.cloud.requestreply.examples.response.config;

import lombok.Data;

@Data
public class CustomHeaderMapping {
    private String binding;
    private String replyHeaderName;
}
