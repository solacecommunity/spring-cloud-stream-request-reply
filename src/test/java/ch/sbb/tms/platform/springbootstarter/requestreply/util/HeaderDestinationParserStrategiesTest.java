/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.util;

import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessagingUtilTest.buildMessage;
import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessagingUtilTest.destination;
import static ch.sbb.tms.platform.springbootstarter.requestreply.util.MessagingUtilTest.hdr;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.messaging.Message;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;

class HeaderDestinationParserStrategiesTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("provideArgumentsForVerifyDestinationHeader")
    void verifyDestinationHeader(String description, Message<String> message, String expectedDestination) {
        assertEquals(expectedDestination, HeaderDestinationParserStrategies.retrieve(message.getHeaders()));
    }

    private static Stream<Arguments> provideArgumentsForVerifyDestinationHeader() {
        String target = "to" + UUID.randomUUID();

        return Stream.of( //
                Arguments.of("missing destination should not fail", buildMessage(), null), //
                Arguments.of("destination should be retrievable from spring cloud standard",
                        buildMessage(hdr(MessagingUtil.SCS_SEND_TO_DESTINATION, target)), target), //
                Arguments.of("destination should be retrievable from solace",
                        buildMessage(hdr(SolaceHeaders.DESTINATION, destination(target))), target), //
                Arguments.of("destination should be retrievable from binder",
                        buildMessage(hdr(BinderHeaders.TARGET_DESTINATION, target)), target) //
        );
    }

}
