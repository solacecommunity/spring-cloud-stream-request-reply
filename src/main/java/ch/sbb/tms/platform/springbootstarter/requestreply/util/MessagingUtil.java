/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.util;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.function.Function;

/**
 * The messaging utility provides functionality to set the message headers for forwarded or replied messages
 * in conjunction with request/reply functionality
 */
public final class MessagingUtil {
    public static final String SCS_SEND_TO_DESTINATION = "spring.cloud.stream.sendto.destination";

    private MessagingUtil() {
        // static methods only
    }

    /**
     * wrap the given function, copying message headers from incoming to outgoing message,
     * properly setting correlation ID and target
     * 
     * @param <Q> incoming message payload type
     * @param <A> outgoing message payload type
     * @param payloadFunction mapping function from incoming to outgoing payload
     * @return message with the payload function applied to the incoming message and the message headers prepared for answering
     */
    public static <Q, A> Function<Message<Q>, Message<A>> wrap(Function<Q,A> payloadFunction) {
        return request -> {
            MessageBuilder<A> mb = MessageBuilder.withPayload(payloadFunction.apply(request.getPayload()));
            transferAndAdoptHeaders(request, mb);
            return mb.build() //
            ;
        };
    }

    private static <Q, A> void transferAndAdoptHeaders(Message<Q> request, MessageBuilder<A> mb) {
        MessageHeaders requestHeaders = request.getHeaders();
        String correlationId = HeaderCorrelationIdParserStrategies.retrieve(requestHeaders);
        String replyToDestination = HeaderReplyChannelParserStrategies.retrieve(requestHeaders);

        mb.setCorrelationId(correlationId) //
                .setHeader(SolaceHeaders.CORRELATION_ID, correlationId) //
                .setHeader(BinderHeaders.TARGET_DESTINATION, replyToDestination);
    }

    /**
     * set the given message's headers so it is properly forwarded to the designated target channel
     * @param <T> message payload type
     * @param originalMessage the original message to create forward message from
     * @param targetDestination the target message channel name to direct the message at
     * @return a copy of the original message with the target header properly set
     */
    public static final <T> Message<T> forwardMessage(Message<T> originalMessage, String targetDestination) {
        return MessageBuilder.fromMessage(originalMessage) //
                .setHeader(BinderHeaders.TARGET_DESTINATION, targetDestination) //
                .build() //
        ;
    }
}
