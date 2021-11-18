package ch.sbb.tms.platform.springbootstarter.requestreply.service;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import ch.sbb.tms.platform.springbootstarter.requestreply.AbstractRequestReplyIT;
import ch.sbb.tms.platform.springbootstarter.requestreply.model.SensorReading;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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
    void requestAndAwaitReply_expectMsgSendAndException_whenNoResponse() {
        SensorReading request = new SensorReading();
        request.setSensorID("toilet");

        assertThrows(Exception.class, () -> {
            requestReplyService.requestAndAwaitReply(
                    request,
                    "last_value/temperature/celsius/demo",
                    SensorReading.class,
                    Duration.ofMillis(100)
            );
        });

        Mockito.verify(streamBridge).send(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertEquals(
                "last_value/temperature/celsius/demo",
                destinationCaptor.getValue()
        );

        assertEquals(
                "requestReply/response/itTests",
                messageCaptor.getValue().getHeaders().getReplyChannel()
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
    void requestAndAwaitReply_expectMsgSendAdnReturnResponse_whenResponseSend() throws ExecutionException, InterruptedException, TimeoutException {
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
                            expectedResponse,
                            msg.getHeaders()
                    )
            );

            return true;
        });


        SensorReading response = requestReplyService.requestAndAwaitReply(
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
}