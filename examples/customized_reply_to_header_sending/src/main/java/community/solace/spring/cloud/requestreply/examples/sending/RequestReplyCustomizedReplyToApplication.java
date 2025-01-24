package community.solace.spring.cloud.requestreply.examples.sending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan("community.solace.spring.cloud.requestreply.examples.sending.config")
@SpringBootApplication
public class RequestReplyCustomizedReplyToApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestReplyCustomizedReplyToApplication.class, args);
    }
}
