package community.solace.spring.cloud.requestreply.service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import community.solace.spring.cloud.requestreply.AbstractRequestReplyLoggingIT;
import community.solace.spring.cloud.requestreply.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import community.solace.spring.cloud.requestreply.service.logging.RequestReplyLogger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;

class RequestReplyLoggingServiceTests extends AbstractRequestReplyLoggingIT {

    @Captor
    ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
    @Captor
    ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
    @MockitoBean
    private StreamBridge streamBridge;
    @Autowired
    private RequestReplyServiceImpl requestReplyService;
    @MockitoBean
    private RequestReplyLogger requestReplyLogger;

    @Test
    void requestAndAwaitReplyToTopic_expectExceptionAndErrorLogToBeanLogger_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        // at least one error log must be thrown into the customized logger:
        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Failed.+to.+collect.*"),
                    any(Object[].class));

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToTopicWithMsg_expectExceptionAndErrorLogToBeanLogger_whenNoResponse() {
        SensorReading requestContent = new SensorReading();
        requestContent.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                MessageBuilder.withPayload(requestContent)
                              .setHeader("solace_correlationId", "theMessageId1")
                              .build(),
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        // at least one error log must be thrown into the customized logger:
        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Failed.+to.+collect.*"),
                    any(Object[].class));

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

        Mockito.verify(requestReplyLogger, Mockito.never())
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    anyString(),
                    any(Object[].class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .logRequest(any(),
                           any(Level.class),
                           matches(".*[S,s]ending.+message.*"),
                           any(Message.class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .logReply(any(),
                         any(Level.class),
                         matches(".*[R,r]eceive.+[R,r]esponse.*"),
                         anyLong(),
                         any(Message.class));

        resetMocks();
    }


    @Test
    void requestAndAwaitReplyToTopic_expectExceptionAndErrorLogToBeanLogger_whenErrorResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");
        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   // Simply echo all request header back.
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .fromMessage(msg)
                                   .setHeader("errorMessage", "Something went wrong")
                                   .build()
                   );

                   return true;
               });

        RemoteErrorException error = assertThrows(RemoteErrorException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(10000)
        ));

        assertEquals(
                "Something went wrong",
                error.getMessage()
        );

        // at least one error log must be thrown into the customized logger:
        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Failed.+to.+collect.*"),
                    any(Object[].class));

        resetMocks();
    }


    @Test
    void requestAndAwaitReplyToBinding_expectMsgSendAndReturnResponseAndLogsToRequestAndReplyBeanLogger_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mck the response.
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


        SensorReading response = requestReplyService.requestAndAwaitReplyToBinding(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        assertEquals(
                expectedResponse,
                response
        );

        Mockito.verify(requestReplyLogger, Mockito.never())
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    anyString(),
                    any(Object[].class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .logRequest(any(),
                           any(Level.class),
                           matches(".*[S,s]ending.+message.*"),
                           any(Message.class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .logReply(any(),
                         any(Level.class),
                         matches(".*[R,r]eceive.+[R,r]esponse.*"),
                         anyLong(),
                         any(Message.class));

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToBinding_expectExceptionAndErrorLogToBeanLogger_whenErrorResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        // Receive message and mck the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   // Simply echo all request header back.
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .fromMessage(msg)
                                   .setHeader("errorMessage", "Something went wrong")
                                   .build()
                   );

                   return true;
               });


        RemoteErrorException error = assertThrows(RemoteErrorException.class, () -> requestReplyService.requestAndAwaitReplyToBinding(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        assertEquals(
                "Something went wrong",
                error.getMessage()
        );

        // at least one error log must be thrown into the customized logger:
        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Failed.+to.+collect.*"),
                    any(Object[].class));


        resetMocks();
    }

    @Test
    void onReplyReceived_whenNoCorrelationId_thenLogErrorAndNotBlowUp() {
        requestReplyService.onReplyReceived(
                MessageBuilder.withPayload(
                                      "foo"
                              )
                              .build()
        );

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Received.+unexpected.+message.+without.+correlation.*"),
                    any(Object[].class));
    }

    @Test
    void onReplyReceived_whenNoPendingResponses_thenLogInfoAndNotBlowUp() {
        requestReplyService.onReplyReceived(
                MessageBuilder.withPayload(
                                      "foo"
                              )
                              .setHeader("correlationId", "demooo")
                              .build()
        );

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.INFO),
                    matches(".*Received.+unexpected.+message.*"),
                    any(Object[].class));
    }

    @Test
    void requestReplyToTopicReactive_expectExceptionAndErrorLogToBeanLogger_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        Flux<SensorReading> flux = requestReplyService.requestReplyToTopicReactive(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        StepVerifier
                .create(flux)
                .expectNextCount(0)
                .expectErrorMatches(t -> t instanceof TimeoutException)
                .verify(Duration.ofSeconds(10));

        Mockito.verify(streamBridge)
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    matches(".*Failed.+to.+collect.*"),
                    any(Object[].class));
    }

    @Test
    void requestReplyToTopicReactive_expectLoggingEntriesToRequestAndReply_whenResponseWithKnownSizeSend() {
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

        // we still need the above verifier to await the log events could happen already.
        Mockito.verify(requestReplyLogger, Mockito.never())
               .log(eq(LoggerFactory.getLogger(RequestReplyServiceImpl.class)),
                    eq(Level.ERROR),
                    anyString(),
                    any(Object[].class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(1))
               .logRequest(any(),
                           any(Level.class),
                           matches(".*[S,s]ending.+message.*"),
                           any(Message.class));

        Mockito.verify(requestReplyLogger, Mockito.timeout(500).times(3))
               .logReply(any(),
                         any(Level.class),
                         matches(".*[R,r]eceive.+[R,r]esponse.*"),
                         anyLong(),
                         any(Message.class));

        resetMocks();
    }
}
