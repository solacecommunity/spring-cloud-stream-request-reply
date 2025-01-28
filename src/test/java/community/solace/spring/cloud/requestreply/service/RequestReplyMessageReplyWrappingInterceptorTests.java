package community.solace.spring.cloud.requestreply.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

import community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.ReplyWrappingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class RequestReplyMessageReplyWrappingInterceptorTests extends AbstractRequestReplyLoggingIT {

    @Captor
    ArgumentCaptor<Message<?>> messageCaptor;
    @Captor
    ArgumentCaptor<String> destinationCaptor;
    @Autowired
    private RequestReplyMessageHeaderSupportService headerSupport;
    @MockitoBean
    private ReplyWrappingInterceptor replyWrappingInterceptor;

    @BeforeEach
    void setUpSendingInterceptor() {
        Mockito.when(replyWrappingInterceptor.interceptReplyWrappingPayloadMessage(any(), anyString()))
               .thenAnswer(d -> d.getArgument(0));
    }

    private static class RequestTester {
        public String id;
    }

    private static class ReplyTester {
        public String id;
        public String value;
    }

    @Test
    void sendingReplyThroughFlux_shouldDoWrappingInterceptorCall() {
        RequestTester requestTester = new RequestTester();
        requestTester.id = UUID.randomUUID()
                               .toString();
        final String bindingName = "anyBindingName-out-0";

        Function<Flux<Message<ReplyTester>>, Flux<Message<RequestTester>>> flux = headerSupport.wrapFlux((readingIn, responseSink) -> {
            responseSink.next(requestTester);
            responseSink.complete();
        }, bindingName);

        Message<ReplyTester> replyMessage = MessageBuilder.withPayload(new ReplyTester())
                                                          .build();
        Message<RequestTester> requestMessage = MessageBuilder.withPayload(requestTester)
                                                              .build();

        StepVerifier
                .create(flux.apply(Flux.just(replyMessage)))
                .expectNextMatches(actualRequestMessage -> actualRequestMessage.getPayload() == requestMessage.getPayload())
                .thenConsumeWhile(x -> true)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        // the interceptor must be called with the expected binding name:
        Mockito.verify(replyWrappingInterceptor, Mockito.times(1))
               .interceptReplyWrappingPayloadMessage(
                       messageCaptor.capture(),
                       eq(bindingName));
        assertEquals(requestTester, messageCaptor.getValue()
                                                 .getPayload());

        Mockito.verify(replyWrappingInterceptor, Mockito.times(1))
               .interceptReplyWrappingFinishingEmptyMessage(
                       messageCaptor.capture(),
                       eq(bindingName));
        assertEquals("", messageCaptor.getValue()
                                      .getPayload());

        Mockito.verify(replyWrappingInterceptor, Mockito.never())
               .interceptReplyWrappingErrorMessage(
                       any(),
                       anyString());

        resetMocks();
    }

    @Test
    void sendingReplyBy_shouldDoWrappingInterceptorCall() {
        ReplyTester replyTester = new ReplyTester();
        replyTester.id = UUID.randomUUID()
                             .toString();
        final String bindingName = "anyOtherBindingName-out-0";

        headerSupport.<RequestTester, ReplyTester, Function<RequestTester, ReplyTester>, RuntimeException>wrapWithBindingName(
                             (RequestTester actualRequestTester) -> replyTester, bindingName, new HashMap<>())
                     .apply(MessageBuilder.withPayload(new RequestTester())
                                          .build());

        // the interceptor must be called with the expected binding name:
        Mockito.verify(replyWrappingInterceptor, Mockito.times(1))
               .interceptReplyWrappingPayloadMessage(
                       messageCaptor.capture(),
                       eq(bindingName));
        assertEquals(replyTester, messageCaptor.getValue()
                                               .getPayload());

        Mockito.verify(replyWrappingInterceptor, Mockito.never())
               .interceptReplyWrappingErrorMessage(
                       any(),
                       anyString());

        resetMocks();
    }

}
