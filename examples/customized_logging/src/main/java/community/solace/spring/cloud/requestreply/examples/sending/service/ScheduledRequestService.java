package community.solace.spring.cloud.requestreply.examples.sending.service;

import community.solace.spring.cloud.requestreply.examples.sending.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.RequestReplyService;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@RequiredArgsConstructor
@Service
public class ScheduledRequestService {

    private final RequestReplyService requestReplyService;

    @Scheduled(fixedRateString = "PT1M")
    public void sendRequest() throws RemoteErrorException, InterruptedException, TimeoutException {
        String location = "forLoggingDemo";
        SensorReading reading = new SensorReading();
        reading.setSensorID("demo");

        requestReplyService.requestAndAwaitReplyToTopic(
                reading,
                "last_value/humidity/percent/" + location,
                SensorReading.class,
                Duration.ofSeconds(30)
        );
    }
}
