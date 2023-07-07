package community.solace.spring.cloud.requestreply.examples.sending.model;

import java.sql.Timestamp;

import lombok.Data;

@Data
public class SensorReading {
    private Timestamp timestamp;
    private String sensorID;
    private Double temperature;
    private BaseUnit baseUnit;

    public SensorReading() {
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public String toString() {
        return "SensorReading [ "+timestamp + " " + sensorID + " " + String.format("%.1f", temperature) + " " + baseUnit + " ]";
    }
}