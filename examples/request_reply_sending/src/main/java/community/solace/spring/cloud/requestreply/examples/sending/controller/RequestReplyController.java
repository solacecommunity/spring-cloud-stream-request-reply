package community.solace.spring.cloud.requestreply.examples.sending.controller;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import community.solace.spring.cloud.requestreply.service.RequestReplyService;
import community.solace.spring.cloud.requestreply.service.header.parser.errormessage.RemoteErrorException;
import community.solace.spring.cloud.requestreply.examples.sending.model.SensorReading;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Request single reply", description = "Samples for request to single replies")
public class RequestReplyController {

    private final RequestReplyService requestReplyService;

    @Operation(description = "Example using solace binder: dynamic topics")
    @GetMapping(value = "/temperature/last_value/{location}")
    public SensorReading requestReplySampleSolaceDynamic(
            @PathVariable("location") final String location
    ) throws InterruptedException, TimeoutException, RemoteErrorException {
        SensorReading reading = new SensorReading();
        reading.setSensorID(location);

        log.info("Request " + reading);

        return requestReplyService.requestAndAwaitReplyToTopic(
                reading,
                "last_value/temperature/celsius/" + location,
                SensorReading.class,
                Duration.ofSeconds(30)
        );
    }

//    @Operation(description = "Example using local rvd binder: dynamic topics")
//    @GetMapping(value = "/betriebslage/last_value/{trainNumber}")
//    public TrainPosition requestReplySampleTibrvDynamic(
//            @PathVariable("trainNumber") final String trainNumber
//    ) throws InterruptedException, TimeoutException, RemoteErrorException {
//        TrainPosition tp = new TrainPosition();
//        tp.setTrainNumber(trainNumber);
//
//        log.info("Request " + tp);
//
//        return requestReplyService.requestAndAwaitReplyToTopic(
//                tp,
//                "betriebslage.zugpos." + trainNumber,
//                TrainPosition.class,
//                Duration.ofSeconds(30)
//        );
//    }


    @Operation(description = "Example using solace binder: static topics from configuration")
    @GetMapping(value = "/temperature_last_value")
    public SensorReading requestReplySampleSolaceStatic() throws InterruptedException, TimeoutException, RemoteErrorException {
        SensorReading reading = new SensorReading();
        reading.setSensorID("temp1337");

        log.info("Request " + reading);

        return requestReplyService.requestAndAwaitReplyToBinding(
                reading,
                "requestReplyRepliesDemoSolaceStatic",
                SensorReading.class,
                Duration.ofSeconds(30)
        );
    }

//    @Operation(description = "Example using local rvd binder: static topics from configuration")
//    @GetMapping(value = "/betriebslage_last_value")
//    public TrainPosition requestReplySampleTibrvStatic() throws InterruptedException, TimeoutException, RemoteErrorException {
//        TrainPosition tp = new TrainPosition();
//        tp.setTrainNumber("479");
//
//        log.info("Request " + tp);
//
//        return requestReplyService.requestAndAwaitReplyToBinding(
//                tp,
//                "requestReplyRepliesDemoTibrvStatic",
//                TrainPosition.class,
//                Duration.ofSeconds(30)
//        );
//    }

}
