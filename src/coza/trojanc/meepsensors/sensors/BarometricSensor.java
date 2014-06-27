package coza.trojanc.meepsensors.sensors;

/**
 *
 * @author Charl
 */
public interface BarometricSensor extends MEEPSensor{
    
    /**
     * Get the current pressure
     * @return 
     */
    public double getPressure();
}
