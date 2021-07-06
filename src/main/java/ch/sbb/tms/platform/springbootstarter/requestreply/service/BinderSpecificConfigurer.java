package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import org.springframework.cloud.stream.binder.Binder;

public interface BinderSpecificConfigurer {
    boolean appliesTo(Binder<?, ?, ?> binder);
}
