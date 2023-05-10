package ch.sbb.tms.platform.springbootstarter.requestreply.model;

import java.sql.Timestamp;
import java.util.Objects;

public class SensorReading {
    private final Timestamp timestamp;
    private String sensorID;
    private Double temperature;
    private BaseUnit baseUnit;

    public SensorReading() {
        timestamp = new Timestamp(System.currentTimeMillis());
    }

    public SensorReading(Timestamp timestamp, String sensorID, Double temperature, BaseUnit baseUnit) {
        this.timestamp = timestamp;
        this.sensorID = sensorID;
        this.temperature = temperature;
        this.baseUnit = baseUnit;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorReading that = (SensorReading) o;
        return timestamp.equals(that.timestamp) && sensorID.equals(that.sensorID) && Objects.equals(temperature, that.temperature) && baseUnit == that.baseUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, sensorID, temperature, baseUnit);
    }

    public enum BaseUnit {
        CELSIUS,
        FAHRENHEIT
    }
}