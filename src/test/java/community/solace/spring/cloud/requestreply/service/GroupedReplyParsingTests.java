package community.solace.spring.cloud.requestreply.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.SDTStream;
import community.solace.spring.cloud.requestreply.service.header.parser.SpringHeaderParser;
import org.junit.jupiter.api.Test;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how the requester unpacks a grouped (SDTStream) reply into its items: which headers every item
 * carries, and that the items of a group share one header instance. A group can hold thousands of items,
 * so a header copy per item turns unpacking a large reply into the dominant CPU cost.
 */
class GroupedReplyParsingTests {

    @Test
    void groupedReply_whenContentTypeWasMovedAside_restoresItOnEveryItem() {
        Message<SDTStream> group = MessageBuilder.withPayload(TestSdtStreamSupport.createSdtStream(List.of("a", "b")))
                .setHeader(SpringHeaderParser.GROUPED_CONTENT_TYPE, "application/json")
                .build();

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(group);

        assertThat(items)
                .extracting(item -> item.getHeaders().get(MessageHeaders.CONTENT_TYPE))
                .containsExactly("application/json", "application/json");
    }

    @Test
    void groupedReply_whenContentTypeWasMovedAside_keepsTheOtherHeadersOfTheGroup() {
        Message<SDTStream> group = MessageBuilder.withPayload(TestSdtStreamSupport.createSdtStream(List.of("a", "b")))
                .setHeader(SpringHeaderParser.GROUPED_CONTENT_TYPE, "application/json")
                .setHeader("correlationId", "correlation-1")
                .setHeader("solace_senderId", "responder-1")
                .build();

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(group);

        assertThat(items).allSatisfy(item -> assertThat(item.getHeaders())
                .containsEntry("correlationId", "correlation-1")
                .containsEntry("solace_senderId", "responder-1"));
    }

    @Test
    void groupedReply_whenContentTypeIsPresent_keepsIt() {
        Message<SDTStream> group = MessageBuilder.withPayload(TestSdtStreamSupport.createSdtStream(List.of("a")))
                .setHeader(MessageHeaders.CONTENT_TYPE, "text/plain")
                .setHeader(SpringHeaderParser.GROUPED_CONTENT_TYPE, "application/json")
                .build();

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(group);

        assertThat(items.get(0).getHeaders()).containsEntry(MessageHeaders.CONTENT_TYPE, "text/plain");
    }

    @Test
    void groupedReply_givesEveryItemTheHeadersOfTheGroup() {
        Message<SDTStream> group = MessageBuilder.withPayload(TestSdtStreamSupport.createSdtStream(List.of("a", "b")))
                .setHeader("correlationId", "correlation-1")
                .setHeader("solace_senderId", "responder-1")
                .build();

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(group);

        assertThat(items).allSatisfy(item -> assertThat(item.getHeaders())
                .containsEntry("correlationId", "correlation-1")
                .containsEntry("solace_senderId", "responder-1"));
    }

    @Test
    void groupedReply_sharesOneHeaderInstanceAcrossItsItems() {
        Message<SDTStream> group = MessageBuilder.withPayload(TestSdtStreamSupport.createSdtStream(List.of("a", "b", "c")))
                .setHeader(SpringHeaderParser.GROUPED_CONTENT_TYPE, "application/json")
                .build();

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(group);

        MessageHeaders firstItemHeaders = items.get(0).getHeaders();
        assertThat(items)
                .as("every item must reuse the header instance of the first one: "
                        + "a copy per item multiplies the cost of a group by its size")
                .allSatisfy(item -> assertThat(item.getHeaders()).isSameAs(firstItemHeaders));
    }

    @Test
    void groupedReply_decodesBytesItemsAsBytesAndTextItemsAsString() {
        SDTStream stream = JCSMPFactory.onlyInstance().createStream();
        stream.writeString("BytesMessage");
        stream.writeBytes(new byte[] {1, 2, 3});
        stream.writeString("TextMessage");
        stream.writeBytes("text".getBytes(StandardCharsets.UTF_8));

        List<Message<?>> items = RequestReplyServiceImpl.parseMultiResponse(MessageBuilder.withPayload(stream).build());

        assertThat(items)
                .<Object>extracting(Message::getPayload)
                .containsExactly(new byte[] {1, 2, 3}, "text");
    }
}