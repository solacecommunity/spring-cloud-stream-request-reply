/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply;

import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;

@ActiveProfiles(AbstractRequestReplyIT.PROFILE_LOCAL_APP)
@SpringBootApplication
public class RequestReplyTestApplication {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyTestApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RequestReplyTestApplication.class, args);
    }

    @Bean
    @Autowired
    public Function<Message<String>, Message<String>> reverse(RequestReplyMessageHeaderSupportService headerSupport) {
        return headerSupport.wrap((value) -> new StringBuilder(value).reverse().toString());
    }

    @Bean
    public Consumer<Message<String>> logger() {
        return (msg) -> {
            LOG.info(String.format("Received message: %s", msg.getPayload()));
        };
    }
}
