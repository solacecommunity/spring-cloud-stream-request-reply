package ch.sbb.tms.platform.springbootstarter.requestreply.model;

import java.sql.Timestamp;

public class SensorReading {
    private final Timestamp timestamp;
    private String sensorID;
    private Double temperature;
    private BaseUnit baseUnit;

    public SensorReading() {
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getSensorID() {
        return sensorID;
    }

    public void setSensorID(String sensorID) {
        this.sensorID = sensorID;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public BaseUnit getBaseUnit() {
        return baseUnit;
    }

    public void setBaseUnit(BaseUnit baseUnit) {
        this.baseUnit = baseUnit;
    }

    @Override
    public String toString() {
        return "SensorReading [ " + timestamp + " " + sensorID + " " + String.format("%.1f", temperature) + " " + baseUnit + " ]";
    }

    public enum BaseUnit {
        CELSIUS,
        FAHRENHEIT
    }
}