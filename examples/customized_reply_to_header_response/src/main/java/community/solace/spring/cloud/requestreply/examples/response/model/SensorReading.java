package community.solace.spring.cloud.requestreply.examples.response.model;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.io.Serializable;

@Data
@Validated
public class SensorReading implements Serializable {

    private static final long serialVersionUID = -311241661573351788L;

    private String timestamp;
    private String sensorID;

    private Double radiation;
    private SensorReading.BaseUnit baseUnit;

    public enum BaseUnit {
        LUX
    }

    @Override
    public String toString() {
        return "SensorReading [ " + timestamp + " " + sensorID + " " + String.format("%.1f", radiation) + " " + baseUnit + " ]";
    }
}