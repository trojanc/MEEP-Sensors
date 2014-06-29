package coza.trojanc.meepsensors.sensors;

import coza.trojanc.meepsensors.sensors.data.Position;
import coza.trojanc.meepsensors.sensors.data.Velocity;

/**
 * Created by Charl on 2014-06-28.
 */
public interface GpsSensor extends MEEPSensor{

	public Velocity getVelocity();

	public Position getPosition();
}
