package community.solace.spring.cloud.requestreply.examples.response.config;

import community.solace.spring.cloud.requestreply.examples.response.model.SensorReading;
import community.solace.spring.cloud.requestreply.examples.response.model.SensorRequest;
import community.solace.spring.cloud.requestreply.service.header.RequestReplyMessageHeaderSupportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.HashMap;
import java.util.function.Function;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class PingPongConfig {

    @Bean
    public Function<Message<SensorRequest>, Message<SensorReading>> responseToRequestSolace(
            RequestReplyMessageHeaderSupportService headerSupport
    ) {
        return headerSupport.wrapWithBindingName((readingIn) -> {
            log.info("processing incoming request reading: {}", readingIn.toString());

            SensorReading reading = new SensorReading();
            reading.setSensorID(readingIn.getSensorID() + "-out");
            reading.setBaseUnit(SensorReading.BaseUnit.LUX);
            reading.setRadiation(Math.random() * 35d);

            return reading;

        }, "responseToRequestSolace-out-0", new HashMap<>());
    }
}
