package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.RequestReplyBinderAdapterFactory;
import ch.sbb.tms.platform.springbootstarter.requestreply.service.header.RequestReplyMessageHeaderSupportService;

@MockBean(RequestReplyBinderAdapterFactory.class)
class SolaceMessageBuilderConfigurerIT extends AbstractRequestReplyIT {
    @Autowired
    private SolaceMessageBuilderConfigurer solaceMessageBuilderConfigurer;

    @Autowired
    private RequestReplyMessageHeaderSupportService headerSupport;

    @Autowired
    private RequestReplyBinderAdapterFactory adapterFactory;

    @Test
    void adoptCorrelationIdToMessageBuilderShouldSucceed() {
        String correlationId = UUID.randomUUID().toString();

        MessageBuilder<String> mb = MessageBuilder.withPayload("adoptToMessageBuilderShouldSucceed");
        RequestMessageBuilderAdapter adapter = solaceMessageBuilderConfigurer.adoptTo(mb);
        assertSame(adapter, adapter.setCorrelationId(correlationId));

        Message<String> message = mb.build();
        assertEquals(correlationId, headerSupport.getCorrelationId(message));
    }

    @Test
    void adoptDestinationWithoutBoundTargetToMessageBuilderShouldSucceed() {
        String destination = UUID.randomUUID().toString();

        MessageBuilder<String> mb = MessageBuilder.withPayload("adoptToMessageBuilderShouldSucceed");
        RequestMessageBuilderAdapter adapter = solaceMessageBuilderConfigurer.adoptTo(mb);
        
        when(adapterFactory.getBindingDestination(eq(destination))).thenReturn(Optional.empty());

        assertSame(adapter, adapter.setDestination(destination));

        Message<String> message = mb.build();
        assertEquals(destination, headerSupport.getDestination(message));

        verify(adapterFactory).getBindingDestination(eq(destination));
    }

    @Test
    void adoptDestinationWithBoundTargetToMessageBuilderShouldSucceed() {
        String destination = UUID.randomUUID().toString();
        String boundMessageChannel = "boundMessageChannel";

        MessageBuilder<String> mb = MessageBuilder.withPayload("adoptToMessageBuilderShouldSucceed");
        RequestMessageBuilderAdapter adapter = solaceMessageBuilderConfigurer.adoptTo(mb);

        when(adapterFactory.getBindingDestination(eq(boundMessageChannel))).thenReturn(Optional.of(destination));

        assertSame(adapter, adapter.setDestination(boundMessageChannel));

        Message<String> message = mb.build();
        assertEquals(destination, headerSupport.getDestination(message));

        verify(adapterFactory).getBindingDestination(eq(boundMessageChannel));
    }

    @Test
    void adoptReplyTopicToMessageBuilderShouldSucceed() {
        String replyTopic = UUID.randomUUID().toString();

        MessageBuilder<String> mb = MessageBuilder.withPayload("adoptToMessageBuilderShouldSucceed");
        RequestMessageBuilderAdapter adapter = solaceMessageBuilderConfigurer.adoptTo(mb);
        assertSame(adapter, adapter.setReplyToTopic(replyTopic));

        Message<String> message = mb.build();
        assertEquals(replyTopic, headerSupport.getReplyTo(message));
    }
}
