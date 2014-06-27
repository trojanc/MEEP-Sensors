package coza.trojanc.meepsensors.sensors;

/**
 *
 * @author Charl
 */
public interface TemperatureSensor extends MEEPSensor{
    
    /**
     * Get the temperature of the sensor
     * @return 
     */
    public double getTemparature();
}
