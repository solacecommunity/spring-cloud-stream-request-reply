package community.solace.spring.cloud.requestreply.service;

import community.solace.spring.cloud.requestreply.config.RequestReplyProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Registers one {@link FunctionRegistration} per configured
 * {@code spring.cloud.stream.requestreply.bindingMapping} so that spring-cloud-stream routes incoming
 * replies to {@link RequestReplyServiceImpl#onReplyReceived(Message)}.
 *
 * <p>This logic used to live in an {@link org.springframework.context.ApplicationContextInitializer}.
 * Spring Boot applies such initializers to <em>every</em> application context it creates - including
 * sliced test contexts such as {@code @JsonTest} - and they cannot be turned off through
 * {@code @ImportAutoConfiguration(exclude = ...)} or {@code spring.autoconfigure.exclude}. Because the
 * registered reply consumers depend on the {@link RequestReplyServiceImpl} bean (which is absent in a
 * slice), the context failed to start
 * (<a href="https://github.com/solacecommunity/spring-cloud-stream-request-reply/issues/8">issue&nbsp;#8</a>).</p>
 *
 * <p>By contributing the bean definitions from a registrar that is
 * {@link org.springframework.context.annotation.Import imported} by {@link RequestReplyAutoConfiguration},
 * the registration now runs only when the auto-configuration itself is active. Excluding the
 * auto-configuration therefore also disables the reply-consumer registration, which is the behaviour a
 * test slice expects.</p>
 */
class RequestReplyFunctionRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware, BeanFactoryAware {

    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyFunctionRegistrar.class);

    /**
     * The generic type of the reply consumer, i.e. {@code Consumer<Message<Object>>}. spring-cloud-function
     * uses it to wire the registration to the {@code <bindingName>-in-0} input binding.
     */
    private static final ResolvableType REPLY_CONSUMER_TYPE = ResolvableType.forClassWithGenerics(
            Consumer.class,
            ResolvableType.forClassWithGenerics(Message.class, Object.class));

    private Environment environment;
    private BeanFactory beanFactory;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void registerBeanDefinitions(final AnnotationMetadata importingClassMetadata, final BeanDefinitionRegistry registry) {
        final BindResult<RequestReplyProperties> bindResult = Binder.get(environment)
                .bind("spring.cloud.stream.requestreply", RequestReplyProperties.class);

        if (!bindResult.isBound()) {
            return;
        }

        for (final String bindingName : bindResult.get().getBindingMappingNames()) {
            if (registry.containsBeanDefinition(bindingName)) {
                // Never override a function the application defined itself under the same name.
                LOG.debug("Skip binding: {} for receiving replies, a bean with that name already exists", bindingName);
                continue;
            }

            final RootBeanDefinition definition = new RootBeanDefinition(FunctionRegistration.class);
            definition.setInstanceSupplier(replyConsumerSupplier());
            registry.registerBeanDefinition(bindingName, definition);

            LOG.info("Register binding: {} for receiving replies", bindingName);
        }
    }

    private Supplier<FunctionRegistration<Consumer<Message<?>>>> replyConsumerSupplier() {
        return () -> {
            final RequestReplyServiceImpl service = beanFactory.getBean(RequestReplyServiceImpl.class);
            return new FunctionRegistration<Consumer<Message<?>>>(service::onReplyReceived)
                    .type(REPLY_CONSUMER_TYPE.getType());
        };
    }
}
