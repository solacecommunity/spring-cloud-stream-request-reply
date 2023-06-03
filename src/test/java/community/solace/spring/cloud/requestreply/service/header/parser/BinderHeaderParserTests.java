package community.solace.spring.cloud.requestreply.service.header.parser;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.junit.jupiter.api.Assertions.*;

class BinderHeaderParserTests {

    @Test
    void getDestination() {
        BinderHeaderParser p = new BinderHeaderParser();

        Message<String> m = MessageBuilder.withPayload("demo")
                .setHeader(BinderHeaders.TARGET_DESTINATION, "1337/foo")
                .build();

        assertEquals(
                "1337/foo",
                p.getDestination(m.getHeaders())
        );
    }
}