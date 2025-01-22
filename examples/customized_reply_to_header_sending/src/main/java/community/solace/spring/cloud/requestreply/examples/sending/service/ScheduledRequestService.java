package community.solace.spring.cloud.requestreply.examples.sending.service;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import community.solace.spring.cloud.requestreply.examples.sending.model.SensorReading;
import community.solace.spring.cloud.requestreply.service.RequestReplyService;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledRequestService {

    private final RequestReplyService requestReplyService;

    /**
     * a note for using scheduled in combination with flux:
     * There seems to be a misbehavior/bug on future await with timeout when reactive flux is used,
     * that after the first timeout all following flux executions are skipped immeditately.
     * A clear bug report would require quite some investigation hence if one runs into troubles feel free to report a reproducer.
     */
    @Scheduled(fixedRateString = "PT1M")
    public void sendRequest() throws RemoteErrorException, InterruptedException, TimeoutException {
        String location = "forInterceptorDemo";
        SensorReading reading = new SensorReading();
        reading.setSensorID("demo");
        log.info("request sensor: {}", reading.getSensorID());

        requestReplyService.requestAndAwaitReplyToTopic(
                reading,
                "last_value/radiation/solar/" + location,
                SensorReading.class,
                Duration.ofSeconds(30)
        );
    }
}
