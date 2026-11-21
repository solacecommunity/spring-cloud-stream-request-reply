package community.solace.spring.cloud.requestreply.service.header;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.service.header.parser.SolaceHeaderParser;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MutableMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A responder that copies the request's time-to-live onto its replies with {@code copyHeadersOnWrap} chose that
 * value itself, so the library must not replace it with what is left of the request's time-to-live.
 */
@TestPropertySource(properties = "spring.cloud.stream.requestreply.copyHeadersOnWrap=" + SolaceHeaderParser.TIME_TO_LIVE)
class RequestReplyCopiedTimeToLiveTests extends AbstractRequestReplySimpleIT {
    private static final Duration REQUEST_TIME_TO_LIVE = Duration.ofSeconds(10);
    private static final Duration TIME_SINCE_ARRIVAL = Duration.ofSeconds(3);

    @Autowired
    RequestReplyMessageHeaderSupportService supportService;

    @Test
    void wrap_expectCopiedTimeToLive_whenCopyHeadersOnWrapNamesIt() {
        Function<Message<String>, Message<String>> supplier = supportService.wrap(m -> m, (Class<Throwable>) null);

        Message<String> answerM = supplier.apply(requestArrivedAgo());

        assertEquals(
                REQUEST_TIME_TO_LIVE.toMillis(),
                answerM.getHeaders().get(SolaceHeaderParser.TIME_TO_LIVE, Long.class)
        );
    }

    private static Message<String> requestArrivedAgo() {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MessageHeaders.TIMESTAMP, Instant.now().minus(TIME_SINCE_ARRIVAL).toEpochMilli());
        headers.put("correlationId", "my-correlationId-my");
        headers.put(MessageHeaders.REPLY_CHANNEL, "my-dest-my/{StagePlaceholder}/the-event-after");
        headers.put(SolaceHeaderParser.TIME_TO_LIVE, REQUEST_TIME_TO_LIVE.toMillis());
        return new MutableMessage<>("demo", headers);
    }
}