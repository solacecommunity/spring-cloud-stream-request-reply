package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.AbstractMessageChannelBinder;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;

@DirtiesContext(methodMode = MethodMode.AFTER_METHOD)
class RequestReplyBinderAdapterIT extends AbstractRequestReplyIT {
    @Autowired
    private RequestReplyBinderAdapterFactory adapterFactory;

    @Test
    void getDefaultBinderShouldSucceed() {
        BindingServiceProperties mockProperties = Mockito.mock(BindingServiceProperties.class);
        ReflectionTestUtils.setField(adapterFactory, "bindingServiceProperties", mockProperties);
        when(mockProperties.getDefaultBinder()).thenReturn("main_session");
        assertNotNull(adapterFactory.getDefaultBinder());
        verify(mockProperties).getDefaultBinder();
    }

    @Test
    void getDefaultForSingleBinderShouldSucceed() {
        BindingServiceProperties mockProperties = Mockito.mock(BindingServiceProperties.class);
        ReflectionTestUtils.setField(adapterFactory, "bindingServiceProperties", mockProperties);
        when(mockProperties.getDefaultBinder()).thenReturn(null);
        assertNotNull(adapterFactory.getDefaultBinder());
        verify(mockProperties).getDefaultBinder();
    }

    @Test
    void requestingUnknownBinderShouldThrowException() {
        assertThrows(RuntimeException.class, () -> adapterFactory.getCachedAdapterForBinder((Binder<?, ?, ?>) null));
        assertThrows(RuntimeException.class, () -> adapterFactory.getCachedAdapterForBinder(Mockito.mock(Binder.class)));
    }

    @Test
    void nonMessageChannelBinderIsUnsupportedAndThrowsException() {
        Binder<?, ?, ?> binder = Mockito.mock(Binder.class);
        RequestReplyProperties properties = new RequestReplyProperties();
        assertThrows(UnsupportedOperationException.class, () -> adapterFactory.createAdapter(binder, properties));
    }

    @Test
    void duplicateBinderShouldTriggerException() {
        Binder<?, ?, ?> binder = Mockito.mock(AbstractMessageChannelBinder.class);
        RequestReplyProperties properties = new RequestReplyProperties();
        properties.setTopic("duplicateBinderShouldTriggerException");

        AtomicReference<RequestReplyBinderAdapter> adapterRef = new AtomicReference<RequestReplyBinderAdapter>();
        assertDoesNotThrow(() -> adapterRef.set(adapterFactory.createAdapter(binder, properties)));

        RequestReplyBinderAdapter adapter = adapterRef.get();
        assertNotNull(adapter);
        assertTrue(StringUtils.hasText(adapter.toString()));
        assertSame(properties, adapter.getProperties());
        assertNotNull(adapter.getMessagebuilderConfigurer());

        verify(binder).bindConsumer(any(), any(), any(), any());

        assertThrows(RuntimeException.class, () -> adapterFactory.createAdapter(binder, properties));
    }
}
