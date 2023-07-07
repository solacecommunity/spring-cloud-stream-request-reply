package community.solace.spring.cloud.requestreply.examples.response.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import community.solace.spring.cloud.requestreply.examples.response.model.SensorReading;
import community.solace.spring.cloud.requestreply.examples.response.model.SensorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PingMultiPongConfig {

    /**
     * The producer is a flux knowing the amount of responses when creating the first response.
     * Plain functional programming.
     */
    @Bean
    public Function<Message<SensorRequest>, List<Message<SensorReading>>> responseMultiToRequestKnownSizeSolace(
            RequestReplyMessageHeaderSupportService headerSupport
    ) {
        return headerSupport.wrapList((readingIn) -> {
            log.info(readingIn.toString());

            List<SensorReading> responses = new ArrayList<>();
            for (int i = 0; i <= 101; i++) {
                SensorReading reading = new SensorReading();
                reading.setSensorID(readingIn.getSensorID());
                reading.setTimestamp(String.format("2020-04-01T%02d:%02d:00.000Z", 10 + (i * 5 / 60), (i * 5) % 60));
                reading.setBaseUnit(SensorReading.BaseUnit.CELSIUS);
                reading.setTemperature(Math.random() * 35d);
                responses.add(reading);
            }

            return responses;
        }, "responseMultiToRequestKnownSizeSolace-out-0");
    }


    /**
     * The producer is a flux not knowing the amount of responses when creating the first response.
     * Memory optimized. Should only be used with big result sets, because of bigger initial overhead.
     */
    @Bean
    public Function<Flux<Message<SensorRequest>>, Flux<Message<SensorReading>>> responseMultiToRequestRandomSizeSolace(
            RequestReplyMessageHeaderSupportService headerSupport
    ) {
        return headerSupport.wrapFlux((readingIn, responseSink) -> {
            log.info(readingIn.toString());

            for (int i = 0; i <= 210 + (100 * Math.random()); i++) {
                SensorReading reading = new SensorReading();
                reading.setSensorID(readingIn.getSensorID() + "-out" );
                reading.setTimestamp(String.format("2020-04-01T%02d:%02d:00.000Z", 10 + (i * 5 / 60), (i * 5) % 60));
                reading.setBaseUnit(SensorReading.BaseUnit.CELSIUS);
                reading.setTemperature(Math.random() * 35d);
                responseSink.next(reading);
            }

            if (Math.random() < 0.2) {
                responseSink.error(new IllegalArgumentException("Business log error with 20% chance"));
            }

            responseSink.complete();
        }, "responseMultiToRequestRandomSizeSolace-out-0");
    }
}
