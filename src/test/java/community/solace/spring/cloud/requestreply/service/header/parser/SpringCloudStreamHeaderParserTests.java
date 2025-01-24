package community.solace.spring.cloud.requestreply.service.header.parser;

import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringCloudStreamHeaderParserTests {

    @Test
    void getDestination() {
        SpringCloudStreamHeaderParser p = new SpringCloudStreamHeaderParser();

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader("spring.cloud.stream.sendto.destination", "1337/foo")
                .build();

        assertEquals(
                "1337/foo",
                p.getDestination(m.getHeaders())
        );
    }
}