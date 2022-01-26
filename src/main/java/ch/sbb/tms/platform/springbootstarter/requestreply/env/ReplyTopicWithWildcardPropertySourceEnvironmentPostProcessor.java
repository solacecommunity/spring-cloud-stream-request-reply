package ch.sbb.tms.platform.springbootstarter.requestreply.env;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * The default order of this post-processor.
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 1;

    private final Log logger;

    /**
     * Create a new {@link ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessor} instance.
     *
     * @param logger the logger to use
     */
    public ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessor(Log logger) {
        this.logger = logger;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ReplyTopicWithWildcardsPropertySource.addToEnvironment(environment, this.logger);
    }

}
