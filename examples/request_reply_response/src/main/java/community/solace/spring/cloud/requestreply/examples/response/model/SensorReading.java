package community.solace.spring.cloud.requestreply.examples.response.model;

import java.io.Serializable;

import lombok.Data;

import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class SensorReading implements Serializable {

    private static final long serialVersionUID = -311241661573351788L;

    private String timestamp;
    private String sensorID;

    private Double temperature;
    private SensorReading.BaseUnit baseUnit;

    public enum BaseUnit {
        CELSIUS,
        FAHRENHEIT
    }

    @Override
    public String toString() {
        return "SensorReading [ "+timestamp + " " + sensorID + " " + String.format("%.1f", temperature) + " " + baseUnit + " ]";
    }
}