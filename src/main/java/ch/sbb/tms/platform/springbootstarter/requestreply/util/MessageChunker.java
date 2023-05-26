package ch.sbb.tms.platform.springbootstarter.requestreply.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.parser.SpringHeaderParser;
import com.solacesystems.jcsmp.SDTStream;
import com.solacesystems.jcsmp.impl.sdt.StreamImpl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

public final class MessageChunker {

    @SuppressWarnings("unchecked")
    public static <T> List<Pair<Message<T>, Integer>> mapChunked(List<Message<T>> messages, int maxBytes) {
        List<Pair<Message<T>, Integer>> msgs = new ArrayList<>();

        int currentBytes = 0;
        int currentMsgs = 0;
        SDTStream body = new StreamImpl();
        for (Message<?> message : messages) {
            if (!(message.getPayload() instanceof byte[])) {
                // this message was unable to be converted
                return messages.stream()
                        .map(msg -> Pair.of(msg, 1))
                        .collect(Collectors.toList());
            }

            currentBytes += ((byte[]) message.getPayload()).length;
            currentMsgs++;

            body.writeString("BytesMessage");
            body.writeBytes((byte[]) message.getPayload());

            if (currentBytes > maxBytes) {
                msgs.add(Pair.of(
                        (Message<T>) createSDTStreamMessage(body, message.getHeaders()),
                        currentMsgs
                ));

                currentBytes = 0;
                currentMsgs = 0;
                body = new StreamImpl();
            }
        }

        if (currentBytes > 0) {
            msgs.add(Pair.of(
                    (Message<T>) createSDTStreamMessage(body, messages.get(messages.size() - 1).getHeaders()),
                    currentMsgs
            ));
        }

        return msgs;
    }

    private static Message<SDTStream> createSDTStreamMessage(SDTStream body, MessageHeaders messageHeaders) {
        Map<String, Object> msgHeader = new HashMap<>(messageHeaders);
        // protect against spring want to encode message.
        if (StringUtils.isNotEmpty((String) msgHeader.get(MessageHeaders.CONTENT_TYPE))) {
            msgHeader.put(SpringHeaderParser.GROUPED_CONTENT_TYPE, msgHeader.get(MessageHeaders.CONTENT_TYPE));
            msgHeader.remove(MessageHeaders.CONTENT_TYPE);
        }
        // Enforce spring no to encode with for example jackson.
        msgHeader.put(FunctionProperties.EXPECT_CONTENT_TYPE_HEADER, "");
        msgHeader.put(SpringHeaderParser.GROUPED_MESSAGES, true);

        return MessageBuilder
                .withPayload(body)
                .copyHeaders(msgHeader)
                .build();
    }
}
