/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package ch.sbb.tms.platform.springbootstarter.requestreply.config;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.style.ToStringCreator;

@ConfigurationProperties("requestreply")
public class RequestReplyProperties {
    public static final String DEFAULT_PREFIX = "requestReply";

    public static final String REQUESTS_POSTFIX = "requests";

    public static final String REPLIES_POSTFIX = "replies";

    private String topic;

    @NotNull
    private Period timeout = new Period();

    private String group;

    private Map<String, Object> consumerProperties;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Period getTimeout() {
        return timeout;
    }

    public void setTimeout(Period timeout) {
        this.timeout = timeout;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Map<String, Object> getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(Map<String, Object> consumerProperties) {
        this.consumerProperties = consumerProperties;
    }

    public static class Period {
        @Min(1)
        Long duration = 500L;

        @NotNull
        TimeUnit unit = TimeUnit.MILLISECONDS;

        /**
         * default constructor
         */
        public Period() {
            // nothing to do here
        }

        /**
         * initializing constructor
         */
        public Period(@Min(1) Long duration, @NotNull TimeUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(TimeUnit unit) {
            this.unit = unit;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this) //
                    .append("duration", String.format("%s %s", duration, unit)) //
                    .toString() //
            ;
        }
    }

    @Override
    public String toString() {
        return new ToStringCreator(this) //
                .append("topic", topic) //
                .append("group", group) //
                .append("timeout", timeout) //             
                .toString() //
        ;
    }
}
