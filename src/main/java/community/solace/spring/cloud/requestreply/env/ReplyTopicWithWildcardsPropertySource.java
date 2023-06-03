package community.solace.spring.cloud.requestreply.env;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class ReplyTopicWithWildcardsPropertySource extends PropertySource<ReplyTopicWithWildcards> {

    private static final Log logger = LogFactory.getLog(ReplyTopicWithWildcardsPropertySource.class);
    public static final String PROPERTY_SOURCE_NAME = "replyTopicWithWildcards";
    public static final int ACTION_INDEX = 1;
    public static final int BINDING_NAME_INDEX = 1;
    public static final int WILDCARD_INDEX = 2;
    public static final int PARAMETER_COUNT_UUID = 2;
    public static final int PARAMETER_COUNT_REPLACE_WITH_WILDCARDS = 3;
    private ConfigurableEnvironment environment;
    private String uuid;

    public ReplyTopicWithWildcardsPropertySource(String name, ConfigurableEnvironment environment) {
        super(name, new ReplyTopicWithWildcards());
        this.environment = environment;
        this.uuid = UUID.randomUUID().toString();
    }

    static void addToEnvironment(ConfigurableEnvironment environment) {
        MutablePropertySources sources = environment.getPropertySources();
        PropertySource<?> existing = sources.get(PROPERTY_SOURCE_NAME);
        if (existing != null) {
            logger.trace("ReplyTopicWithWildcardsPropertySource already present");
            return;
        }
        ReplyTopicWithWildcardsPropertySource propertySource = new ReplyTopicWithWildcardsPropertySource(PROPERTY_SOURCE_NAME, environment);
        if (sources.get(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME) != null) {
            sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            sources.addLast(propertySource);
        }
        logger.trace("ReplyTopicWithWildcardsPropertySource add to Environment");
    }

    @Override
    public Object getProperty(String name) {
        if (!name.startsWith(PROPERTY_SOURCE_NAME)) {
            return null;
        }
        String[] parts = name.split(Pattern.quote("|"));

        if (parts.length == PARAMETER_COUNT_UUID && Objects.equals(parts[ACTION_INDEX], "uuid")) {
            /**
             * The {rand.uuid} will generate a new uuid each time it was called.
             * But what we need is an uuid that is generated on process start.
             */
            return uuid;
        }

        if (parts.length < PARAMETER_COUNT_REPLACE_WITH_WILDCARDS) {
            logger.error("replyTopicWithWildcards: usage is: ${replyTopicWithWildcards|name of the binding|wildcard char for this binder}  Example: ${replyTopicWithWildcards|requestReplyRepliesDemoSolace|*}");
        }

        return source.replaceWithWildcards(parts[BINDING_NAME_INDEX], parts[WILDCARD_INDEX], environment);
    }
}
