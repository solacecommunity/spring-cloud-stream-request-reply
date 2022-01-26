package ch.sbb.tms.platform.springbootstarter.requestreply.env;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class ReplyTopicWithWildcardsPropertySource extends PropertySource<ReplyTopicWithWildcards> {
    public static final String PROPERTY_SOURCE_NAME = "replyTopicWithWildcards";
    private ConfigurableEnvironment environment;
    private String uuid;

    public ReplyTopicWithWildcardsPropertySource(String name, ConfigurableEnvironment environment) {
        super(name, new ReplyTopicWithWildcards());
        this.environment = environment;
        this.uuid = UUID.randomUUID().toString();
    }

    static void addToEnvironment(ConfigurableEnvironment environment, Log logger) {
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

        if (parts.length == 2 && Objects.equals(parts[1], "uuid")) {
            /**
             * The {rand.uuid} will generate a new uuid each time it was called.
             * But what we need is an uuid that is generated on process start.
             */
            return uuid;
        }

        if (parts.length < 3) {
            logger.error("replyTopicWithWildcards: usage is: ${replyTopicWithWildcards|name of the binding|wildcard char for this binder}  Example: ${replyTopicWithWildcards|requestReplyRepliesDemoSolace|*}");
        }

        return source.replaceWithWildcards(parts[1], parts[2], environment);
    }
}
