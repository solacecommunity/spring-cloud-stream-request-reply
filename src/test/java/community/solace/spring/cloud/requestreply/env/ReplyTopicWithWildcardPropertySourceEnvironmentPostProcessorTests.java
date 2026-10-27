package community.solace.spring.cloud.requestreply.env;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Proves that Spring Boot picks up the post-processor from {@code META-INF/spring.factories} at
 * startup. Without it, every {@code ${replyTopicWithWildcards|...}} placeholder stays unresolved.
 */
@SpringBootTest(classes = ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessorTests.EmptyApplication.class)
class ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessorTests {

    @SpringBootConfiguration
    static class EmptyApplication {
    }

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void addsReplyTopicWithWildcardsPropertySourceAtStartup() {
        assertInstanceOf(
                ReplyTopicWithWildcardsPropertySource.class,
                environment.getPropertySources().get(ReplyTopicWithWildcardsPropertySource.PROPERTY_SOURCE_NAME)
        );
    }
}