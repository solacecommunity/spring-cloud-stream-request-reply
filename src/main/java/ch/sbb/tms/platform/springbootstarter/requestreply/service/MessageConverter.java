package ch.sbb.tms.platform.springbootstarter.requestreply.service;


import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.function.context.FunctionProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MessageConverter {

    private final CompositeMessageConverter messageConverter;

    public MessageConverter(CompositeMessageConverter messageConverter) {
        this.messageConverter = messageConverter;
    }

    public Message<?> convertMessageToBytesIfNecessary(Message<?> msg, String expectedContentType) {
        if (msg.getPayload() instanceof byte[]) {
            return msg;
        }

        String contentType = msg.getHeaders().containsKey(FunctionProperties.EXPECT_CONTENT_TYPE_HEADER)
                ? (String) msg.getHeaders().get(FunctionProperties.EXPECT_CONTENT_TYPE_HEADER)
                : expectedContentType;

        if (!StringUtils.hasText((String) msg.getHeaders().get(MessageHeaders.CONTENT_TYPE))) {
            Map<String, Object> headersMap = new HashMap<>(msg.getHeaders());
            headersMap.put(MessageHeaders.CONTENT_TYPE, expectedContentType);
            msg = MessageBuilder
                    .withPayload(msg.getPayload())
                    .copyHeaders(headersMap)
                    .build();
        }

        if (StringUtils.hasText((String) msg.getHeaders().get(MessageHeaders.CONTENT_TYPE))) {
            Map<String, Object> headersMap = new HashMap<>(msg.getHeaders());
            String[] expectedContentTypes = StringUtils.delimitedListToStringArray(contentType, ",");
            for (String ct : expectedContentTypes) {
                headersMap.put(MessageHeaders.CONTENT_TYPE, ct);
                Message<?> message = MessageBuilder
                        .withPayload(msg.getPayload())
                        .copyHeaders(headersMap)
                        .build();
                Message<?> result = messageConverter.toMessage(message.getPayload(), message.getHeaders());
                if (result != null) {
                    return result;
                }
            }
        }
        return msg;
    }
}
