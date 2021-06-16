package ch.sbb.tms.platform.springbootstarter.requestreply.service.header.configurer;

import javax.validation.constraints.NotEmpty;

public interface RequestMessageBuilderAdapter {
    RequestMessageBuilderAdapter setCorrelationId(String correlationId);

    RequestMessageBuilderAdapter setReplyToTopic(String topic);

    RequestMessageBuilderAdapter setDestination(@NotEmpty String requestDestination);
}
