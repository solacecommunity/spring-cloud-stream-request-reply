package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.model.SensorReading;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class RequestReplyServiceTests extends AbstractRequestReplyIT {

    @Captor
    ArgumentCaptor<Message<?>> messageCaptor;
    @Captor
    ArgumentCaptor<String> destinationCaptor;
    @MockBean
    private StreamBridge streamBridge;
    @Autowired
    private RequestReplyService requestReplyService;

    @Test
    void requestAndAwaitReplyToTopic_expectMsgSendAndException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToTopic(
                request,
                "last_value/temperature/celsius/demo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        Mockito.verify(streamBridge).send(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(
                "requestReplyRepliesDemo-out-0",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                messageCaptor.getValue().getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );

        assertNotNull(
                messageCaptor.getValue().getHeaders().get("correlationId")
        );

        assertEquals(
                request,
                messageCaptor.getValue().getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToTopicWithMsg_expectMsgSendAndException_whenNoResponse() {
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

        Mockito.verify(streamBridge).send(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                messageCaptor.getValue().getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "theMessageId1",
                messageCaptor.getValue().getHeaders().get("correlationId")
        );

        assertEquals(
                requestContent,
                messageCaptor.getValue().getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToTopic_expectMsgSendAndReturnResponse_whenResponseSend() throws InterruptedException, TimeoutException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                anyString(),
                any(Message.class)
        )).thenAnswer(invocation -> {
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
    void requestAndAwaitReplyToTopic_expectMsgSendAndReturnUnpackedResponse_whenResponseSend() throws InterruptedException, TimeoutException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mock the response.
        Mockito.when(streamBridge.send(
                anyString(),
                any(Message.class)
        )).thenAnswer(invocation -> {
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
    void requestAndAwaitReplyToBinding_expectMsgSendAndException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(TimeoutException.class, () -> requestReplyService.requestAndAwaitReplyToBinding(
                request,
                "requestReplyRepliesDemo",
                SensorReading.class,
                Duration.ofMillis(100)
        ));

        Mockito.verify(streamBridge).send(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(
                "requestReplyRepliesDemo-out-0",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "the/request/topic",
                messageCaptor.getValue().getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );

        assertNotNull(
                messageCaptor.getValue().getHeaders().get("correlationId")
        );

        assertEquals(
                request,
                messageCaptor.getValue().getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToBindingWithMsg_expectMsgSendAndException_whenNoResponse() {
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

        Mockito.verify(streamBridge).send(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "the/request/topic",
                messageCaptor.getValue().getHeaders().get(BinderHeaders.TARGET_DESTINATION)
        );

        assertEquals(
                "requestReply/response/{StagePlaceholder}/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
        );

        assertEquals(
                "theMessageId2",
                messageCaptor.getValue().getHeaders().get("correlationId")
        );

        assertEquals(
                requestContent,
                messageCaptor.getValue().getPayload()
        );
    }

    @Test
    void requestAndAwaitReplyToBinding_expectMsgSendAndReturnResponse_whenResponseSend() throws InterruptedException, TimeoutException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mck the response.
        Mockito.when(streamBridge.send(
                anyString(),
                any(Message.class)
        )).thenAnswer(invocation -> {
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
    void requestAndAwaitReplyToBinding_expectMsgSendAndReturnUnpackedResponse_whenResponseSend() throws InterruptedException, TimeoutException {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        SensorReading expectedResponse = new SensorReading();
        expectedResponse.setSensorID("livingroom");

        // Receive message and mck the response.
        Mockito.when(streamBridge.send(
                anyString(),
                any(Message.class)
        )).thenAnswer(invocation -> {
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
}