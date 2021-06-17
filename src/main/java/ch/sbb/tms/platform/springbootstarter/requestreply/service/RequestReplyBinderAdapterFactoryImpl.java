package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import static ch.sbb.tms.platform.springbootstarter.requestreply.service.ReplyMessageChannel.CHANNEL_NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderCustomizer;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.style.ToStringCreator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer.MessageBuilderConfigurer;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.properties.BinderConsumerPropertiesConfigurer;

/**
 * RequestReplyBinderAdapterFactoryImpl creates and maintains {@link RequestReplyBinderAdapter} instances
 */
@Service
public class RequestReplyBinderAdapterFactoryImpl
        implements ApplicationContextAware, RequestReplyBinderAdapterFactory, BinderCustomizer {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyBinderAdapterFactoryImpl.class);

    static final AtomicReference<RequestReplyBinderAdapterFactoryImpl> REQUEST_REPLY_BINDER_ADAPTER_FACTORY_REFERENCE = new AtomicReference<>(
            null);

    @Autowired
    @Lazy
    private List<MessageBuilderConfigurer> messageBuilderConfigurers;

    @Autowired
    @Lazy
    private List<BinderConsumerPropertiesConfigurer<?>> configurationPropertiesConfigurers;

    @Autowired
    private BindingServiceProperties bindingServiceProperties;

    @Autowired
    @Qualifier(CHANNEL_NAME)
    private MessageChannel replyMessageChannel;

    private Map<String, Binder<?, ?, ?>> binderByName = new HashMap<>();
    private Map<Binder<?, ?, ?>, String> binderNameByBinder = new HashMap<>();
    private Map<Binder<?, ?, ?>, RequestReplyBinderAdapter> adapterCache = new HashMap<>();

    @Override
    public Binder<?, ?, ?> getDefaultBinder() {
        if (StringUtils.hasText(bindingServiceProperties.getDefaultBinder())) {
            return getBinder(bindingServiceProperties.getDefaultBinder());
        }

        if (binderNameByBinder.size() == 1) {
            return binderNameByBinder.keySet().iterator().next();
        }

        throw new IllegalStateException("spring.cloud.stream.defaultBinder is not defined");
    }

    @Override
    public Binder<?, ?, ?> getBinder(String binderName) {
        return binderByName.get(binderName);
    }

    @Override
    public RequestReplyBinderAdapter getCachedAdapterForBinder(Binder<?, ?, ?> binder) {
        if (binder == null || !adapterCache.containsKey(binder)) {
            throw new NoSuchElementException(String.format("No adapter for binder %s", binder));
        }

        return adapterCache.get(binder);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        synchronized (REQUEST_REPLY_BINDER_ADAPTER_FACTORY_REFERENCE) {
            if (REQUEST_REPLY_BINDER_ADAPTER_FACTORY_REFERENCE.get() == null) {
                REQUEST_REPLY_BINDER_ADAPTER_FACTORY_REFERENCE.set(applicationContext.getBean(RequestReplyBinderAdapterFactoryImpl.class));
            }
        }
    }

    @Override
    public Optional<String> getBindingDestination(String bindingName) {
        return bindingServiceProperties.getBindings().entrySet().stream() //
                .filter(e -> e.getKey().equals(bindingName)) //
                .map(Entry::getValue) //
                .map(BindingProperties::getDestination) //
                .findFirst();
    }

    /**
     * retrieve a {@link MessageBuilderConfigurer} suitable to prepare a {@link Message} to be sent via the given {@link Binder}
     * @param binder the binder for which to retrieve {@link MessageBuilderConfigurer}
     * @return message builder configurer
     */
    protected @NonNull MessageBuilderConfigurer getMessageBuilderConfigurer(Binder<?, ?, ?> binder) {
        return messageBuilderConfigurers.stream() //
                .filter(c -> c.appliesTo(binder)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalStateException(String.format("No message builder configurer for binder %s", binder))) //
        ;
    }

    /**
     * retrieve a {@link BinderConsumerPropertiesConfigurer} suitable to prepare {@link ConsumerProperties} to initialize the given {@link Binder}
     * @param binder the binder for which to retrieve {@link BinderConsumerPropertiesConfigurer}
     * @return the binders property configurer
     */
    private BinderConsumerPropertiesConfigurer<?> getPropertiesConfigurer(Binder<?, ?, ?> binder) {
        return configurationPropertiesConfigurers.stream() //
                .filter(c -> c.appliesTo(binder)) //
                .findFirst() //
                .orElseThrow(() -> new IllegalStateException(String.format("No property configurer for binder %s", binder))) //
        ;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public RequestReplyBinderAdapter createAdapter(Binder<?, ?, ?> binder, RequestReplyProperties properties) {
        if (adapterCache.containsKey(binder)) {
            throw new DuplicateKeyException(String.format("Adapter for binder %s already initialized", binder));
        }

        // fetch suitable configurers 
        MessageBuilderConfigurer messageBuilderConfigurer = getMessageBuilderConfigurer(binder);
        BinderConsumerPropertiesConfigurer<?> propertiesConfigurer = getPropertiesConfigurer(binder);

        // create adapter
        RequestReplyBinderAdapter adapter = new RequestReplyBinderAdapterImpl(messageBuilderConfigurer, propertiesConfigurer, properties);
        adapterCache.put(binder, adapter);

        // register message handler
        // both supported Solace and TibRv binder are instances of AbstractMessageChannelBinder
        // (in fact, even RabbitMQ, Kafka etc should work with this)
        if (binder instanceof AbstractMessageChannelBinder) {
            AbstractMessageChannelBinder messageChannelBinder = (AbstractMessageChannelBinder) binder;
            ConsumerProperties consumerProperties = adapter.buildConsumerProperties(properties.getConsumerProperties());
            messageChannelBinder.bindConsumer(properties.getTopic(), properties.getGroup(), replyMessageChannel, consumerProperties);
        }
        else {
            throw new UnsupportedOperationException("Unsupported binder " + binder);
        }

        return adapter;
    }

    @Override
    public void customize(Binder<?, ConsumerProperties, ProducerProperties> binder, String binderName) {
        if (!binderByName.containsKey(binderName)) {
            binderByName.put(binderName, binder);
            binderNameByBinder.put(binder, binderName);
        }
    }

    class RequestReplyBinderAdapterImpl implements RequestReplyBinderAdapter {
        private MessageBuilderConfigurer messageBuilderConfigurer;
        private BinderConsumerPropertiesConfigurer<?> consumerPropertiesConfigurer;
        private RequestReplyProperties properties;

        public RequestReplyBinderAdapterImpl( //
                MessageBuilderConfigurer messageBuilderConfigurer, //
                BinderConsumerPropertiesConfigurer<?> consumerPropertiesConfigurer, //
                RequestReplyProperties properties //
        ) {
            this.messageBuilderConfigurer = messageBuilderConfigurer;
            this.consumerPropertiesConfigurer = consumerPropertiesConfigurer;
            this.properties = properties;
        }

        @Override
        public @NotNull MessageBuilderConfigurer getMessagebuilderConfigurer() {
            return messageBuilderConfigurer;
        }

        @Override
        public RequestReplyProperties getProperties() {
            return properties;
        }

        @Override
        public String toString() {
            return new ToStringCreator(this) //
                    .append("messageBuilderConfigurer", messageBuilderConfigurer) //
                    .append("properties", properties) //
                    .toString() //
            ;
        }

        @Override
        public ConsumerProperties buildConsumerProperties(Map<String, Object> consumerProperties) {
            return consumerPropertiesConfigurer.buildConsumerProperties(consumerProperties);
        }
    }
}
