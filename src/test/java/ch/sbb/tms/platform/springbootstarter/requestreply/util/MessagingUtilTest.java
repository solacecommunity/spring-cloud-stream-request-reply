/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.util;

import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessageValidator.correlationId;
import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessageValidator.length;
import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessageValidator.target;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;
import com.solacesystems.jcsmp.Destination;

class MessagingUtilTest {
    private static final Function<String, Integer> DUMMY_FUNCTION = s -> Optional.ofNullable(s).map(String::length).orElse(null);
    private static final Function<Message<String>, Message<Integer>> TEST_WRAPPER = MessagingUtil.wrap(DUMMY_FUNCTION);
    private static final String PAYLOAD = "Hier könnte Ihre Werbung stehen.";

    @Test
    void forwardMessageShouldSucceed() {
        String target = "forwardMessageShouldSucceed-out-0";
        String testUuid = UUID.randomUUID().toString();

        Message<String> originalMessage = MessageBuilder //
                .withPayload(testUuid) // payload should remain the same
                .setCorrelationId(testUuid) // correlationId must be kept
                .setHeader("hdr_" + testUuid, testUuid) // additional headers should be kept
                .build() //
        ;

        Message<String> forwardMessage = MessagingUtil.forwardMessage(originalMessage, "forwardMessageShouldSucceed-out-0");
        assertNotSame(originalMessage, forwardMessage);
        assertEquals(originalMessage.getPayload(), forwardMessage.getPayload());
        assertEquals(testUuid, HeaderCorrelationIdParserStrategies.retrieve(forwardMessage.getHeaders()));
        assertEquals(testUuid, forwardMessage.getHeaders().get("hdr_" + testUuid));
        assertEquals(target, forwardMessage.getHeaders().get(BinderHeaders.TARGET_DESTINATION));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideArgumentsForVerifyWrap")
    void verifyWrap(String description, Message<String> in, MessageValidator messageValidator) {
        Message<Integer> out = TEST_WRAPPER.apply(in);
        assertTrue(messageValidator.matches(in, out));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Arguments> provideArgumentsForVerifyWrap() {
        return Stream.of( //
                Arguments.of("payload should be properly mapped", buildMessage(), length(32)), //

                Arguments.of("missing correlationId should not fail", buildMessage(), correlationId(null)), //
                Arguments.of("correlationId should be populated from solace", buildMessage(hdr(SolaceHeaders.CORRELATION_ID, "1")),
                        correlationId("1")), //
                Arguments.of("correlationId should be populated from integration",
                        buildMessage(hdr(IntegrationMessageHeaderAccessor.CORRELATION_ID, "2")), correlationId("2")), //
                Arguments.of("correlationId should be populated from http", buildMessage(hdr("X-Correlation-ID", "3")), correlationId("3")), //
                
                Arguments.of("missing reyplyTo should not fail", buildMessage(), target(null)), //
                Arguments.of("target should be populated from solace replyTo", buildMessage(hdr(SolaceHeaders.REPLY_TO, destination("4"))),
                        target("4")), //
                Arguments.of("target should be populated from channel replyTo", buildMessage(hdr(MessageHeaders.REPLY_CHANNEL, "5")),
                        target("5")) //
        );
    }

    static Destination destination(String destination) {
        return new Destination() {

            @Override
            public boolean isTemporary() {
                return false;
            }

            @Override
            public String getName() {
                return destination;
            }
        };
    }

    @SuppressWarnings("rawtypes")
    static Entry<String, Object> hdr(String key, Object val) {
        return new AbstractMap.SimpleEntry(key, val);
    }

    static <T> Message<String> buildMessage(Entry<String, Object>... headers) {
        MessageBuilder<String> mb = MessageBuilder.withPayload(PAYLOAD);

        Arrays.stream(headers).forEach(e -> mb.setHeader(e.getKey(), e.getValue()));

        return mb.build();
    }
}

@FunctionalInterface
interface MessageValidator {
    boolean matches(Message<?> in, Message<?> out);
    
    static MessageValidator length(Integer length) {
        return (in, out) -> Objects.equals(length, out.getPayload());
    }

    static MessageValidator correlationId(String correlationId) {
        return (in, out) -> Objects.equals(correlationId, out.getHeaders().get(IntegrationMessageHeaderAccessor.CORRELATION_ID));
    }

    static MessageValidator target(String target) {
        return (in, out) -> Objects.equals(target, out.getHeaders().get(BinderHeaders.TARGET_DESTINATION));
    }
}