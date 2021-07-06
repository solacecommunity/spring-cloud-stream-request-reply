package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Optional;

import org.springframework.cloud.stream.binder.Binder;

import ch.sbb.tms.platform.springbootstarter.requestreply.config.RequestReplyProperties;


public interface RequestReplyBinderAdapterFactory {
    static RequestReplyBinderAdapterFactory getProxy() {
        InvocationHandler handler = (proxy, method, args) -> {
            RequestReplyBinderAdapterFactory bean = RequestReplyBinderAdapterFactoryImpl.REQUEST_REPLY_BINDER_ADAPTER_FACTORY_REFERENCE
                    .get();

            if (bean == null) {
                throw new IllegalStateException("no bean of type RequestReplyBinderAdapterFactory initialized");
            }

            return method.invoke(bean, args);
        };

        return (RequestReplyBinderAdapterFactory) Proxy.newProxyInstance( //
                RequestReplyBinderAdapterFactory.class.getClassLoader(), //
                new Class[] { RequestReplyBinderAdapterFactory.class }, //
                handler //
        );
    }

    RequestReplyBinderAdapter createAdapter(Binder<?, ?, ?> binder, RequestReplyProperties properties);

    RequestReplyBinderAdapter getCachedAdapterForBinder(Binder<?, ?, ?> binder);

    default RequestReplyBinderAdapter getCachedAdapterForBinder(String binderName) {
        return getCachedAdapterForBinder(getBinder(binderName));
    }

    default RequestReplyBinderAdapter getCachedAdapterForDefaultBinder() {
        return getCachedAdapterForBinder(getDefaultBinder());
    }

    Binder<?, ?, ?> getDefaultBinder();

    Binder<?, ?, ?> getBinder(String binderName);

    Optional<String> getBindingDestination(String bindingName);
}
