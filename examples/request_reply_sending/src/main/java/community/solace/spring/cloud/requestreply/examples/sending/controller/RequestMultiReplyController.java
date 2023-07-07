package community.solace.spring.cloud.requestreply.examples.sending.controller;

import java.time.Duration;
import java.util.List;

import community.solace.spring.cloud.requestreply.service.RequestReplyService;
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
@Tag(name = "Request multi reply", description = "Samples for request to multy replies")
public class RequestMultiReplyController {

    private final RequestReplyService requestReplyService;

    @Operation(description = "Example using solace binder: dynamic topics, responders send list")
    @GetMapping(value = "/temperature/last_hour/{location}")
    public List<SensorReading> requestMultiReplySampleSolaceKnownSize(
            @PathVariable("location") final String location
    ) {
        SensorReading reading = new SensorReading();
        reading.setSensorID(location);

        log.info("Request " + reading);

        return requestReplyService.requestReplyToTopicReactive(
                        reading,
                        "last_hour/temperature/celsius/" + location,
                        SensorReading.class,
                        Duration.ofSeconds(30)
                )
                .collectList()
                .block();
    }

    @Operation(description = "Example using solace binder: dynamic topics, responder sends stream (unknown size of items)")
    @GetMapping(value = "/temperature/last_day/{location}")
    public List<SensorReading> requestMultiReplySampleSolaceRandomSize(
            @PathVariable("location") final String location
    ) {
        SensorReading reading = new SensorReading();
        reading.setSensorID(location);

        log.info("Request " + reading);

        return requestReplyService.requestReplyToTopicReactive(
                        reading,
                        "last_day/temperature/celsius/" + location,
                        SensorReading.class,
                        Duration.ofSeconds(30)
                )
                .collectList()
                .block();
    }
}
