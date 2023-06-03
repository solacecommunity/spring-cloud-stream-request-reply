/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package community.solace.spring.cloud.requestreply.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import community.solace.spring.cloud.requestreply.exception.RequestReplyException;

class CheckedExceptionWrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(CheckedExceptionWrapperTest.class);

    @Test
    void throwingUncheckedFunctionShouldSucceed() {
        Function<String, Long> fn = CheckedExceptionWrapper.throwingUnchecked(Long::valueOf);

        assertEquals(Long.valueOf(2), fn.apply("2"));

        try {
            fn.apply("test");
            fail();
        }
        catch (Exception e) {
            assertEquals(RequestReplyException.class, e.getClass());
            assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    @Test
    void throwingUncheckedSupplierShouldSucceed() {
        Supplier<Long> supplier = CheckedExceptionWrapper.throwingUnchecked(() -> Long.valueOf("2"));
        assertEquals(Long.valueOf(2), supplier.get());

        supplier = CheckedExceptionWrapper.throwingUnchecked(() -> Long.valueOf("test"));
        try {
            supplier.get();
            fail();
        }
        catch (Exception e) {
            assertEquals(RequestReplyException.class, e.getClass());
            assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    @Test
    void throwingUncheckedRunnableShouldSucceed() {
        Runnable runnable = CheckedExceptionWrapper.throwingUnchecked(() -> LOG.trace(Long.valueOf("2").toString()));
        runnable.run();

        runnable = CheckedExceptionWrapper.throwingUnchecked(() -> LOG.trace(Long.valueOf("test").toString()));
        try {
            runnable.run();
            fail();
        }
        catch (Exception e) {
            assertEquals(RequestReplyException.class, e.getClass());
            assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }
}