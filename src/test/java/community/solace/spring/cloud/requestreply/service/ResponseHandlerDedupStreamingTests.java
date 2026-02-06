/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2026.
 */

package community.solace.spring.cloud.requestreply.service;

import java.util.concurrent.atomic.AtomicInteger;

import community.solace.spring.cloud.requestreply.service.logging.DefaultRequestReplyLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Focused deduplication tests for the streaming/unknown-size scenario.
 */
class ResponseHandlerDedupStreamingTests {

    @AfterEach
    void clearMaxBitsSystemProperty() {
        System.clearProperty("spring.cloud.stream.requestreply.dedup.maxBitsWhenUnknown");
    }

    @Test
    void checkDuplicate_shouldDeduplicateNumericIndices_whenTotalRepliesUnknown_upToDefaultCap() {
        AtomicInteger received = new AtomicInteger();
        ResponseHandler handler = new ResponseHandler(
                msg -> received.incrementAndGet(),
                true,
                null,
                new DefaultRequestReplyLogger()
        );

        assertFalse(handler.checkDuplicate("0"));
        assertTrue(handler.checkDuplicate("0"));

        assertFalse(handler.checkDuplicate("99999"));
        assertTrue(handler.checkDuplicate("99999"));

        // Above default cap (100000): not deduped (current behavior is to skip/cap dedup).
        assertFalse(handler.checkDuplicate("100000"));
        assertFalse(handler.checkDuplicate("100000"));
    }

    @Test
    void checkDuplicate_shouldDeduplicateRanges_whenTotalRepliesUnknown() {
        ResponseHandler handler = new ResponseHandler(
                msg -> {
                },
                true,
                null,
                new DefaultRequestReplyLogger()
        );

        assertFalse(handler.checkDuplicate("10-12"));
        // With start-only dedup semantics, only ranges with the same start are considered duplicates.
        assertFalse(handler.checkDuplicate("11"));
        assertTrue(handler.checkDuplicate("10-12"));
        // non-overlapping -> not duplicate
        assertFalse(handler.checkDuplicate("13-15"));
    }

    @Test
    void checkDuplicate_shouldStillProcessHighIndex_whenMaxBitsWhenUnknownIsLow_streamingSingleNoDup() {
        System.setProperty("spring.cloud.stream.requestreply.dedup.maxBitsWhenUnknown", "10");

        AtomicInteger processed = new AtomicInteger();
        ResponseHandler handler = new ResponseHandler(
                msg -> processed.incrementAndGet(),
                true,
                null,
                new DefaultRequestReplyLogger()
        );

        // Index well above cap: dedup should be skipped, but message must still be processed.
        assertFalse(handler.checkDuplicate("1000"));
        handler.receive(org.springframework.messaging.support.MessageBuilder.withPayload("p").build());

        assertEquals(1, processed.get());
    }
}
