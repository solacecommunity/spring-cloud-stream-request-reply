package community.solace.spring.cloud.requestreply.sampleapps.simple;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

@ActiveProfiles(AbstractRequestReplySimpleIT.PROFILE_LOCAL_APP)
@SpringBootApplication
public class RequestReplyTestSimpleApplication {
    private static final Logger LOG = LoggerFactory.getLogger(RequestReplyTestSimpleApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RequestReplyTestSimpleApplication.class, args);
    }

    @Bean
    @Autowired
    public Function<Message<String>, Message<String>> reverse(RequestReplyMessageHeaderSupportService headerSupport) {
        return headerSupport.wrap((value) -> new StringBuilder(value).reverse().toString(), null);
    }

    @Bean
    public Consumer<Message<String>> logger() {
        return (msg) -> {
            LOG.info(String.format("Received message: %s", msg.getPayload()));
        };
    }

    @Bean
    public MessageConverter sensorReadingReferenceConverter() {
        return new MessageConverter() {
            @Override
            public Message<?> toMessage(Object payload, MessageHeaders headers) {
                if (payload instanceof SensorReading) {
                    return new GenericMessage<>(new AtomicReference<>((SensorReading) payload));
                }
                return null;
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Object fromMessage(Message<?> message, Class<?> targetClass) {
                if (message.getPayload() instanceof AtomicReference) {
                    AtomicReference ar = (AtomicReference) message.getPayload();
                    Object data = ar.get();

                    if (data instanceof SensorReading) {
                        return data;
                    }
                }
                return null;
            }
        };
    }
}
