package community.solace.spring.cloud.requestreply.examples.sending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RequestReplyCustomLoggingApplication {
    public static void main(String[] args) {
        SpringApplication.run(RequestReplyCustomLoggingApplication.class, args);
    }
}
