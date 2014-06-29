package coza.trojanc.meepsensors.sensors.data;


/**
 *
 */
public class Velocity {

	/**
	 * Time when this data was recorded
	 */
	private final long timeStamp;

	/**
	 * The true track bearing
	 */
	private final double trueTrack;

	/**
	 * The speed over the ground in km/h
	 */
	private final double groundSpeed;
	
	/**
	 * Constructor
	 * 
	 * @param timeStamp Time when this data was recorded
	 * @param trueTrack The true track bearing
	 * @param groundSpeed The speed over the ground in km/h
	 */
	public Velocity(long timeStamp, double trueTrack, double groundSpeed) {
		this.timeStamp = timeStamp;
		this.trueTrack = trueTrack;
		this.groundSpeed = groundSpeed;
	}

	/**
	 * Get the track for the GPS sensor (i.e. a bearing)
	 * 
	 * @return the trueTrack
	 */
	public double getTrueTrack() {
		return trueTrack;
	}

	/**
	 * Get the velocity of the GPS sensor (in Km/h)
	 * 
	 * @return the speed over the ground
	 */
	public double getGroundSpeed() {
		return groundSpeed;
	}
	
	/**
	 * Get the time stamp for when this data was recorded
	 * 
	 * @return The number of seconds from the epoch when this data was collected
	 */
	public long getTimeStamp() {
		return timeStamp;
	}

}