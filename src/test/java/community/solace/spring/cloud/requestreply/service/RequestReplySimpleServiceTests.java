package community.solace.spring.cloud.requestreply.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import community.solace.spring.cloud.requestreply.AbstractRequestReplySimpleIT;
import community.solace.spring.cloud.requestreply.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static community.solace.spring.cloud.requestreply.model.SensorReading.BaseUnit.CELSIUS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class RequestReplySimpleServiceTests extends AbstractRequestReplySimpleIT {

    @Captor
    ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
    @Captor
    ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
    @MockitoBean
    private StreamBridge streamBridge;
    @Autowired
    private RequestReplyServiceImpl requestReplyService;

    @Test
    void requestAndAwaitReplyToTopic_expectException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        Mockito.verify(streamBridge)
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        assertEquals(
                "requestReplyRepliesDemo-out-0",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                messageCaptor.getValue()
                             .getHeaders()
                             .get(BinderHeaders.TARGET_DESTINATION)
        );

        assertNotNull(
                messageCaptor.getValue()
                             .getHeaders()
                             .get("correlationId")
        );

        assertEquals(
                request,
                messageCaptor.getValue()
                             .getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToTopicWithMsg_expectException_whenNoResponse() {
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

        Mockito.verify(streamBridge)
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                messageCaptor.getValue()
                             .getHeaders()
                             .get(BinderHeaders.TARGET_DESTINATION)
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "theMessageId1",
                messageCaptor.getValue()
                             .getHeaders()
                             .get("correlationId")
        );

        assertEquals(
                requestContent,
                messageCaptor.getValue()
                             .getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToTopic_expectMsgSendAndReturnResponse_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
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

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToTopic_expectMsgSendAndReturnUnpackedResponse_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
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
                                   // RR-Service must unpack payload
                                   new AtomicReference<>(expectedResponse),
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

        resetMocks();
    }


    @Test
    void requestAndAwaitReplyToTopic_expectAdditionalHeaders_whenRequestReceived() throws TimeoutException, RemoteErrorException, InterruptedException {
        String headerKeyBanane = "HeaderKeyBanane";

        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");
        AtomicReference<String> headerValue = new AtomicReference<>(null);

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);
                   headerValue.set((String) msg.getHeaders()
                                               .get(headerKeyBanane));
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


        SensorReading ignored = requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100),
                Map.of(headerKeyBanane, "HeaderValueBanane")
        );
        assertEquals(
                "HeaderValueBanane",
                headerValue.get()
        );

        resetMocks();
    }


    @Test
    void requestAndAwaitReplyToTopic_expectException_whenErrorResponse() {
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

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToBinding_expectException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToBinding(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        Mockito.verify(streamBridge)
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        assertEquals(
                "requestReplyRepliesDemo-out-0",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "the/request/topic",
                messageCaptor.getValue()
                             .getHeaders()
                             .get(BinderHeaders.TARGET_DESTINATION)
        );

        assertNotNull(
                messageCaptor.getValue()
                             .getHeaders()
                             .get("correlationId")
        );

        assertEquals(
                request,
                messageCaptor.getValue()
                             .getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToBindingWithMsg_expectException_whenNoResponse() {
        SensorReading requestContent = new SensorReading();
        requestContent.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToBinding(
                MessageBuilder.withPayload(requestContent)
                              .setHeader("solace_correlationId", "theMessageId2")
                              .build(),
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        Mockito.verify(streamBridge)
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "the/request/topic",
                messageCaptor.getValue()
                             .getHeaders()
                             .get(BinderHeaders.TARGET_DESTINATION)
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "theMessageId2",
                messageCaptor.getValue()
                             .getHeaders()
                             .get("correlationId")
        );

        assertEquals(
                requestContent,
                messageCaptor.getValue()
                             .getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToBinding_expectMsgSendAndReturnResponse_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
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

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToBinding_expectMsgSendAndReturnUnpackedResponse_whenResponseSend() throws TimeoutException, RemoteErrorException, InterruptedException {
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
                                   // RR-Service must unpack payload
                                   new AtomicReference<>(expectedResponse),
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

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToBinding_expectException_whenErrorResponse() {
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
    }

    @Test
    void onReplyReceived_whenNoPendingResponses_thenLogErrorAndNotBlowUp() {
        requestReplyService.onReplyReceived(
                MessageBuilder.withPayload(
                                      "foo"
                              )
                              .setHeader("correlationId", "demooo")
                              .build()
        );
    }

    @Test
    void requestReplyToTopicReactive_expectException_whenNoResponse() {
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

        assertEquals(
                "requestReplyRepliesDemo-out-0",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue()
                             .getHeaders()
                             .getReplyChannel()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                messageCaptor.getValue()
                             .getHeaders()
                             .get(BinderHeaders.TARGET_DESTINATION)
        );

        assertNotNull(
                messageCaptor.getValue()
                             .getHeaders()
                             .get("correlationId")
        );

        assertEquals(
                request,
                messageCaptor.getValue()
                             .getPayload()
        );
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnResponse_whenResponseWithKnownSizeSend() {
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

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnResponse_knownSize_whenResponseContainsError() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponseA = new SensorReading(Ten_oClock, "livingroom", 22.0, CELSIUS);

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
                                   .withPayload("")
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "0")
                                   .setHeader("errorMessage", "The error message")
                                   .setHeader("replyIndex", "0")
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
                .expectErrorMessage("The error message")
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnResponse_whenResponseWithUnknownSizeIsSend() {
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
                                   .setHeader("totalReplies", "-1")
                                   .setHeader("replyIndex", "0")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload(expectedResponseB)
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "-1")
                                   .setHeader("replyIndex", "1")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload(expectedResponseC)
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "-1")
                                   .setHeader("replyIndex", "2")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload("")
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "3")
                                   .setHeader("replyIndex", "3")
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

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnResponse_unknownSize_whenResponseContainsError() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponseA = new SensorReading(Ten_oClock, "livingroom", 22.0, CELSIUS);

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
                                   .setHeader("totalReplies", "-1")
                                   .setHeader("replyIndex", "0")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload("")
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "0")
                                   .setHeader("errorMessage", "The error message")
                                   .setHeader("replyIndex", "0")
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
                .expectErrorMessage("The error message")
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnEmptyList_whenResponseHasTotalRepliesNull() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .fromMessage(msg)
                                   .setHeader("totalReplies", "0")
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
                .expectNextCount(0)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestReplyToBindingReactive_expectMsgSendAndReturnUnpackedResponse_whenSingleResponseWithUnknownSizeIsSend() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponseA = new SensorReading(Ten_oClock, "livingroom", 22.0, CELSIUS);

        // Receive message and mck the response.
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
                                   .setHeader("totalReplies", "-1")
                                   .setHeader("replyIndex", "0")
                                   .build()
                   );
                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .withPayload("")
                                   .setHeaders(new MessageHeaderAccessor(msg))
                                   .setHeader("totalReplies", "1")
                                   .setHeader("replyIndex", "1")
                                   .build()
                   );

                   return true;
               });


        Flux<SensorReading> flux = requestReplyService.requestReplyToBindingReactive(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        StepVerifier
                .create(flux)
                .expectNext(expectedResponseA)
                .expectComplete()
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestReplyToBindingReactive_expectMsgSendAndReturnUnpackedResponse_whenMultipleResponsesSend() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponseA = new SensorReading(Ten_oClock, "livingroom", 22.0, CELSIUS);
        SensorReading expectedResponseB = new SensorReading(Eleven_oClock, "bedroom", 23.0, CELSIUS);
        SensorReading expectedResponseC = new SensorReading(Twelve_oClock, "garage", 24.0, CELSIUS);

        // Receive message and mck the response.
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


        Flux<SensorReading> flux = requestReplyService.requestReplyToBindingReactive(
                request,
                "requestReplyRepliesDemo",
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

        resetMocks();
    }

    @Test
    void requestReplyToBindingReactive_expectMsgSendAndReturnEmptyList_whenResponseHasError() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .fromMessage(msg)
                                   .setHeader("totalReplies", "0")
                                   .setHeader("errorMessage", "Something went wrong")
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
                .expectNextCount(0)
                .expectErrorMessage("Something went wrong")
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestReplyToTopicReactive_expectMsgSendAndReturnEmptyList_whenResponseHasError() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                       anyString(),
                       any(Message.class)
               ))
               .thenAnswer(invocation -> {
                   Message<SensorReading> msg = invocation.getArgument(1);

                   requestReplyService.onReplyReceived(
                           MessageBuilder
                                   .fromMessage(msg)
                                   .setHeader("totalReplies", "0")
                                   .setHeader("errorMessage", "Something went wrong")
                                   .build()
                   );

                   return true;
               });


        Flux<SensorReading> flux = requestReplyService.requestReplyToBindingReactive(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        );

        StepVerifier
                .create(flux)
                .expectNextCount(0)
                .expectErrorMessage("Something went wrong")
                .verify(Duration.ofSeconds(10));

        resetMocks();
    }

    @Test
    void requestAndAwaitReplyToTopic_threadLeak() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");
        for (int i = 0; i < 10; i++) {
            try {
                requestReplyService.requestAndAwaitReplyToBinding(
                        request,
                        "requestReplyRepliesDemo",
                        SensorReading.class,
                        Duration.ofMillis(1)
                );
            }
            catch (TimeoutException | RemoteErrorException | InterruptedException ignored) {
            }
        }

        Mockito.verify(streamBridge, Mockito.timeout(500)
                                            .times(10))
               .send(
                       destinationCaptor.capture(),
                       messageCaptor.capture()
               );

        await()
                .atMost(Duration.ofSeconds(3))
                .until(requestReplyService::runningRequests, equalTo(0));
    }
}
