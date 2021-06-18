package ch.sbb.tms.platform.springbootstarter.requestreply.service.properties;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.beanutils.BeanUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.test.TestChannelBinder;

import com.solace.spring.cloud.stream.binder.SolaceMessageChannelBinder;
import com.solace.spring.cloud.stream.binder.properties.SolaceConsumerProperties;

import ch.sbb.tms.platform.springbootstarter.requestreply.service.BinderSpecificConfigurer;

class ConsumerPropertiesConfigurerTest {
    @ParameterizedTest
    @MethodSource("argumentsForPropertyConfigurerShouldApplyToProperBinder")
    void propertyConfigurerShouldApplyToProperBinder( //
            BinderSpecificConfigurer configurer, //
            Binder<?, ?, ?> binder, //
            Boolean expectedResult //
    ) {
        assertEquals(expectedResult, configurer.appliesTo(binder));
    }

    private static List<Arguments> argumentsForPropertyConfigurerShouldApplyToProperBinder() {
        DefaultConsumerPropertiesConfigurer defaultConfigurer = new DefaultConsumerPropertiesConfigurer();
        SolaceConsumerPropertiesConfigurer solaceConfigurer = new SolaceConsumerPropertiesConfigurer();

        Binder<?, ?, ?> solaceBinder = Mockito.mock(SolaceMessageChannelBinder.class);
        Binder<?, ?, ?> testChannelBinder = Mockito.mock(TestChannelBinder.class);

        return List.of(//
                Arguments.of(defaultConfigurer, solaceBinder, TRUE), //
                Arguments.of(defaultConfigurer, testChannelBinder, TRUE), //
                Arguments.of(solaceConfigurer, solaceBinder, TRUE), //
                Arguments.of(solaceConfigurer, testChannelBinder, FALSE) //
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    void operationOnNullShouldNotThrowException() {
        SolaceConsumerPropertiesConfigurer propertiesConfigurer = new SolaceConsumerPropertiesConfigurer();

        Map<String, Object> propertiesMap = Mockito.mock(Map.class);
        ExtendedConsumerProperties<SolaceConsumerProperties> targetProperties = Mockito.mock(ExtendedConsumerProperties.class);

        assertDoesNotThrow(() -> propertiesConfigurer.populateProperties(null, null));
        assertDoesNotThrow(() -> propertiesConfigurer.populateProperties(null, targetProperties));
        assertDoesNotThrow(() -> propertiesConfigurer.populateProperties(propertiesMap, null));

        verifyNoInteractions(propertiesMap, targetProperties);
    }

    @SuppressWarnings("unchecked")
    @Test
    void exceptionsDuringMappingShouldBeSilentlySwallowed() {
        Map<String, Object> propertiesMap = Map.of("whatever", "doesn't matter", "dunno", "get it done");

        try (MockedStatic<BeanUtils> utilities = Mockito.mockStatic(BeanUtils.class)) {
            utilities.when(() -> BeanUtils.setProperty(any(), any(), any())).thenThrow(IllegalAccessException.class,
                    InvocationTargetException.class);
            assertNotNull(new SolaceConsumerPropertiesConfigurer().buildConsumerProperties(propertiesMap));

            utilities.verify(times(2), () -> BeanUtils.setProperty(any(), any(), any()));
        }
    }

    @Test
    void solaceConsumerPropertiesShouldBeProperlyBuilt() {
        Random random = new Random();
        int numberOfThreads = random.nextInt(Integer.MAX_VALUE);
        Boolean durable = random.nextInt(2) == 0 ? FALSE : TRUE;
        Boolean respectTtl = random.nextInt(2) == 0 ? FALSE : TRUE;
        String errorQueueName = UUID.randomUUID().toString();
        String additionalSubscription = UUID.randomUUID().toString();

        Map<String, Object> propertiesMap = Map.<String, Object>of( //
                "concurrency", numberOfThreads, //
                "extension", Map.<String, Object>of( //
                        "thisPropertyDoesNotExist", "tryToSetThis", //
                        "provisionDurableQueue", durable.booleanValue(), //
                        "queueRespectsMsgTtl", respectTtl.booleanValue(), //
                        "errorQueueNameOverride", errorQueueName, //
                        "queueAdditionalSubscriptions", new String[] { additionalSubscription } //
                ));

        ExtendedConsumerProperties<SolaceConsumerProperties> properties = new SolaceConsumerPropertiesConfigurer()
                .buildConsumerProperties(propertiesMap);

        assertEquals(numberOfThreads, properties.getConcurrency());
        assertEquals(durable.booleanValue(), properties.getExtension().isProvisionDurableQueue());
        assertEquals(respectTtl.booleanValue(), properties.getExtension().getQueueRespectsMsgTtl());
        assertEquals(errorQueueName, properties.getExtension().getErrorQueueNameOverride());
        assertEquals(1, properties.getExtension().getQueueAdditionalSubscriptions().length);
        assertEquals(additionalSubscription, properties.getExtension().getQueueAdditionalSubscriptions()[0]);
    }
}
