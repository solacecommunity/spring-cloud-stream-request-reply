/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.util;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.MessageHeaders;

import com.solace.spring.cloud.stream.binder.messaging.SolaceHeaders;

public enum HeaderCorrelationIdParserStrategies {
    SOLACE(headers -> headers.get(SolaceHeaders.CORRELATION_ID, String.class)), //
    INTEGRATION(headers -> headers.get(IntegrationMessageHeaderAccessor.CORRELATION_ID, String.class)), //
    HTTP(headers -> headers.get("X-Correlation-ID", String.class)) //
    ;

    static final Logger LOG = LoggerFactory.getLogger(HeaderDestinationParserStrategies.class);

    private Function<MessageHeaders, String> extractor;

    HeaderCorrelationIdParserStrategies(Function<MessageHeaders, String> extractor) {
        this.extractor = extractor;
    }

    public static final String retrieve(MessageHeaders messageHeaders) {
        if (messageHeaders != null) {
            for (HeaderCorrelationIdParserStrategies strategy : HeaderCorrelationIdParserStrategies.values()) {
                String correlationId = strategy.extractor.apply(messageHeaders);
                if (correlationId != null) {
                    LOG.trace("Retrieved reply to channel '{}' from {}", correlationId, strategy);
                    return correlationId;
                }
            }
        }

        return null;
    }
}