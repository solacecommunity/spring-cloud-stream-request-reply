package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser;

import org.springframework.core.annotation.Order;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.destination.MessageHeaderDestinationParser;

@Service
@Order(10000)
public class SpringCloudStreamHeaderParser implements MessageHeaderDestinationParser {
    /**
     * @see <a href="https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_spring_cloud_stream_sendto_destination">SCS Doku</a>
     */
    private static final String SCS_SEND_TO_DESTINATION = "spring.cloud.stream.sendto.destination";

    @Override
    public String getDestination(MessageHeaders headers) {
        return headers.get(SCS_SEND_TO_DESTINATION, String.class);
    }
}
