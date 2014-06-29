
package coza.trojanc.meepsensors.sensors.data;

import java.util.Formatter;

/**
 *
 */
public class Position {

	/**
	 * The current time
	 */
	private final long timeStamp;

	/**
	 * The latitude of this position
	 */
	private final double latitude;

	/**
	 * Direction of longitude
	 */
	private final char latitudeDirection;

	/**
	 * The longitude of this position
	 */
	private final double longitude;

	/**
	 * Direction of latitude
	 */
	private final char longitudeDirection;

	/**
	 * The altitude of this position
	 */
	private final double altitude;
	
	/**
	 * Constructor
	 * 
	 * @param time The current time
	 * @param latitude The latitude of this position
	 * @param latitudeDirection Direction of longitude
	 * @param longitude The longitude of this position
	 * @param longitudeDirection Direction of latitude
	 * @param altitude The altitude of this position
	 */
	public Position(long time, double latitude, char latitudeDirection, double longitude, char longitudeDirection, double altitude) {
		this.timeStamp = time;
		this.latitude = latitude;
		this.latitudeDirection = latitudeDirection;
		this.longitude = longitude;
		this.longitudeDirection = longitudeDirection;
		this.altitude = altitude;
	}
	
	/**
	 * Get the time this position was recorded
	 * 
	 * @return The time this position was recorded in seconds from the epoch
	 */
	public long getTime() {
		return timeStamp;
	}

	/**
	 * Get the current latitude
	 * 
	 * @return The latitude
	 */
	public double getLatitude() {
		return latitude;
	}
	
	/**
	 * Get the direction of latitude (N or S)
	 * 
	 * @return The latitude direction as a single character
	 */
	public char getLatitudeDirection() {
		return latitudeDirection;
	}

	/**
	 * Get the longitude
	 * 
	 * @return The longitude
	 */
	public double getLongitude() {
		return longitude;
	}
	
	/**
	 * Get the longitude direction (E or W)
	 * 
	 * @return The longitude direction as a single character
	 */
	public char getLongitudeDirection() {
		return longitudeDirection;
	}

	/**
	 * Get the altitude (in metres above sea level)
	 * 
	 * @return The altitude in metres above sea level
	 */
	public double getAltitude() {
		return altitude;
	}

}