package ch.sbb.tms.capaopt.springbootstarter.requestreply.util;

import static ch.sbb.tms.capaopt.springbootstarter.requestreply.util.CheckedExceptionWrapper.throwingUnchecked;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sbb.tms.capaopt.springbootstarter.requestreply.exception.RequestReplyException;

public class CheckedExceptionWrapperTest {
    private static final Logger LOG = LoggerFactory.getLogger(CheckedExceptionWrapperTest.class);

    @Test
    public void throwingUncheckedFunctionShouldSucceed() {
        Function<String, Long> fn = throwingUnchecked(Long::valueOf);

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
    public void throwingUncheckedSupplierShouldSucceed() {
        Supplier<Long> supplier = throwingUnchecked(() -> Long.valueOf("2"));
        assertEquals(Long.valueOf(2), supplier.get());

        supplier = throwingUnchecked(() -> Long.valueOf("test"));
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
    public void throwingUncheckedRunnableShouldSucceed() {
        Runnable runnable = throwingUnchecked(() -> LOG.trace(Long.valueOf("2").toString()));
        runnable.run();

        runnable = throwingUnchecked(() -> LOG.trace(Long.valueOf("test").toString()));
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