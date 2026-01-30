package community.solace.spring.cloud.requestreply.service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT;
import community.solace.spring.cloud.requestreply.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import community.solace.spring.cloud.requestreply.service.messageinterceptor.RequestSendingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static community.solace.spring.cloud.requestreply.model.SensorReading.BaseUnit.CELSIUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class RequestReplyMessageSendingInterceptorTests extends AbstractRequestReplyLoggingIT {

    @Captor
    ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
    @MockitoBean
    private StreamBridge streamBridge;
    @Autowired
    private RequestReplyServiceImpl requestReplyService;
    @MockitoBean
    private RequestSendingInterceptor requestSendingInterceptor;

    @BeforeEach
    void setUpSendingInterceptor() {
        Mockito.when(requestSendingInterceptor.interceptRequestSendingMessage(any(), anyString()))
               .thenAnswer(d -> d.getArgument(0));
    }

    @Test
    void requestAndAwaitReplyToTopic_expectInterceptorCallAndTimeoutException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        // the interceptor must be called with the expected binding name:
        Mockito.verify(requestSendingInterceptor, Mockito.times(1))
               .interceptRequestSendingMessage(
                       messageCaptor.capture(),
                       eq("requestReplyRepliesDemo"));
        assertEquals(request, messageCaptor.getValue()
                                           .getPayload());

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToTopic_expectMsgSendAndReturnResponseAndRespondPlusRequestLoggingOnBeanLogger_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   // Simply echo all request header back.
                   requestReplyService.onReplyReceived(
                           MessageBuilder.createMessage(
                                   // RR-Service must not unpack payload
                                   expectedResponse,
                                   msg.getHeaders()
                           )
                   );

                   return true;
               });

        SensorReading response = requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        assertEquals(
                expectedResponse,
                response
        );

        // the interceptor must be called with the expected binding name:
        Mockito.verify(requestSendingInterceptor, Mockito.times(1))
               .interceptRequestSendingMessage(
                       messageCaptor.capture(),
                       eq("requestReplyRepliesDemo"));
        assertEquals(request, messageCaptor.getValue()
                                           .getPayload());

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectInterceptorCalledAndReply_whenResponseWithKnownSizeSend() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponseA = new SensorReading(Ten_oClock, "livingroom", 22.0, CELSIUS);
        SensorReading expectedResponseB = new SensorReading(Eleven_oClock, "bedroom", 23.0, CELSIUS);
        SensorReading expectedResponseC = new SensorReading(Twelve_oClock, "garage", 24.0, CELSIUS);

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload(expectedResponseA)
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "3")
                                   .setHeader("replyIndex", "0")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload(expectedResponseB)
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "3")
                                   .setHeader("replyIndex", "1")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload(expectedResponseC)
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "3")
                                   .setHeader("replyIndex", "2")
                                   .build()
                   );

                   return true;
               });


        Flux<SensorReading> flux = requestReplyService.requestReplyToTopicReactive(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        StepVerifier
                .create(flux)
                .expectNext(expectedResponseA)
                .expectNext(expectedResponseB)
                .expectNext(expectedResponseC)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        // the interceptor must be called with the expected binding name:
        Mockito.verify(requestSendingInterceptor, Mockito.times(1))
               .interceptRequestSendingMessage(
                       messageCaptor.capture(),
                       eq("requestReplyRepliesDemo"));
        assertEquals(request, messageCaptor.getValue()
                                           .getPayload());

        resetMocks();
    }
}
