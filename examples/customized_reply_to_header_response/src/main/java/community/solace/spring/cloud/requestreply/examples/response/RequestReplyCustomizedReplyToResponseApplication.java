package community.solace.spring.cloud.requestreply.examples.response;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan("community.solace.spring.cloud.requestreply.examples.response.config")
@SpringBootApplication
public class RequestReplyCustomizedReplyToResponseApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestReplyCustomizedReplyToResponseApplication.class, args);
    }
}
