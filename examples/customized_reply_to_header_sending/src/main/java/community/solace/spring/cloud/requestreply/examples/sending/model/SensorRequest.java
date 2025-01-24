package community.solace.spring.cloud.requestreply.examples.sending.model;

import lombok.Data;

import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SensorRequest {
    private String timestamp;
    private String sensorID;
}
