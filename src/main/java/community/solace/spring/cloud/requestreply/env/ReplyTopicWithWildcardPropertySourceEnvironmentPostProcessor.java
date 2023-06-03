package community.solace.spring.cloud.requestreply.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class ReplyTopicWithWildcardPropertySourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /**
     * The default order of this post-processor.
     */
    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 1;


    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        ReplyTopicWithWildcardsPropertySource.addToEnvironment(environment);
    }

}
