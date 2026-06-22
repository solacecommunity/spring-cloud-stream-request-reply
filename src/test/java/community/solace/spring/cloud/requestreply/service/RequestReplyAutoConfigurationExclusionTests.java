package community.solace.spring.cloud.requestreply.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for
 * <a href="https://github.com/solacecommunity/spring-cloud-stream-request-reply/issues/8">issue&nbsp;#8</a>.
 *
 * <p>A {@code @JsonTest} (or any other sliced test) only loads a small, fixed set of
 * auto-configurations and therefore does <em>not</em> contain the request/reply beans. Before the
 * fix, the request/reply function registration lived in an
 * {@link org.springframework.context.ApplicationContextInitializer} that Spring Boot applies to
 * <em>every</em> application context regardless of slicing or {@code @ImportAutoConfiguration(exclude = ...)}.
 * That initializer registered one reply-consumer bean per configured
 * {@code spring.cloud.stream.requestreply.bindingMapping}, each of which depends on the
 * {@code RequestReplyServiceImpl} bean. Since that service bean is absent in a slice, the context
 * failed to start.</p>
 *
 * <p>The reply-consumer registration is now contributed by {@link RequestReplyFunctionRegistrar},
 * which is imported by {@link RequestReplyAutoConfiguration}. As a result it only runs when the
 * auto-configuration itself is active, so a slice that does not load the auto-configuration starts
 * cleanly and none of the reply-consumer beans are registered.</p>
 */
@JsonTest
@ContextConfiguration(classes = RequestReplyAutoConfigurationExclusionTests.JsonOnlyApplication.class)
@TestPropertySource(properties = {
        // A binding mapping is present in the environment, exactly as it would be in a real
        // application. Before the fix this alone was enough to make the slice fail to start.
        "spring.cloud.stream.requestreply.bindingMapping[0].binding=replyConsumerThatMustNotExist",
        "spring.cloud.stream.requestreply.bindingMapping[0].replyTopic=some/reply/topic"
})
class RequestReplyAutoConfigurationExclusionTests {

    @SpringBootConfiguration
    static class JsonOnlyApplication {
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void jsonSliceStartsWithoutRegisteringReplyConsumers() {
        // The slice itself is functional: this is what the reporter actually wanted to test.
        assertNotNull(objectMapper, "the @JsonTest slice should still provide an ObjectMapper");

        // The reply consumer derived from the binding mapping must not have been registered, because
        // the request/reply auto-configuration is not part of this slice.
        assertFalse(context.containsBean("replyConsumerThatMustNotExist"),
                "no reply-consumer bean must be registered when the request/reply auto-configuration is not loaded");
        assertEquals(0, context.getBeanNamesForType(FunctionRegistration.class).length,
                "no request/reply FunctionRegistration beans must be registered in a slice");

        // Without the fix the RequestReplyServiceImpl is missing yet referenced, which is what broke
        // the context. Assert it is genuinely absent so the test stays meaningful.
        assertEquals(0, context.getBeanNamesForType(RequestReplyServiceImpl.class).length,
                "the request/reply service must not be present in a slice that excludes it");
    }
}
