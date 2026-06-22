package community.solace.spring.cloud.requestreply.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifies the behaviour the reporter of
 * <a href="https://github.com/solacecommunity/spring-cloud-stream-request-reply/issues/8">issue&nbsp;#8</a>
 * originally expected: when {@link RequestReplyAutoConfiguration} is excluded, none of the
 * request/reply infrastructure - in particular the per-binding reply consumers that previously came
 * from an un-excludable {@code ApplicationContextInitializer} - must be contributed to the context.
 *
 * <p>A binding mapping is present in the environment so that, before the fix, the initializer would
 * have registered a reply consumer regardless of the exclusion and broken the context.</p>
 */
@SpringBootTest(classes = RequestReplyExcludeAutoConfigurationTests.MinimalApplication.class)
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=community.solace.spring.cloud.requestreply.service.RequestReplyAutoConfiguration",
        "spring.cloud.stream.requestreply.bindingMapping[0].binding=excludedReplyBinding",
        "spring.cloud.stream.requestreply.bindingMapping[0].replyTopic=reply/excluded"
})
class RequestReplyExcludeAutoConfigurationTests {

    /**
     * Deliberately a plain {@code @Configuration} (no component scanning) so that the only way the
     * request/reply beans could appear is through the auto-configuration that we exclude here.
     */
    @Configuration
    @EnableAutoConfiguration
    @Import(TestChannelBinderConfiguration.class)
    static class MinimalApplication {
    }

    @Autowired
    private ApplicationContext context;

    @Test
    void excludingTheAutoConfigurationRemovesAllRequestReplyBeans() {
        assertEquals(0, context.getBeanNamesForType(RequestReplyServiceImpl.class).length,
                "the request/reply service must not be created when the auto-configuration is excluded");
        assertFalse(context.containsBean("excludedReplyBinding"),
                "no reply consumer must be registered for a binding mapping when the auto-configuration is excluded");
        assertEquals(0, context.getBeanNamesForType(FunctionRegistration.class).length,
                "no request/reply FunctionRegistration beans must be registered when the auto-configuration is excluded");
    }
}
