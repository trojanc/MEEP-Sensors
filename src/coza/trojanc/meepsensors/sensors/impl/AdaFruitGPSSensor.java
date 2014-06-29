package coza.trojanc.meepsensors.sensors.impl;

import coza.trojanc.meepsensors.Logger;
import coza.trojanc.meepsensors.sensors.GpsSensor;
import coza.trojanc.meepsensors.sensors.data.Position;
import coza.trojanc.meepsensors.sensors.data.Velocity;
import jdk.dio.DeviceManager;
import jdk.dio.uart.UART;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Charl on 2014-06-28.
 */
public class AdaFruitGPSSensor implements GpsSensor{

	private static final int UART_DEVICE_ID = 40;

	private UART uart;

	/**
	 * A reference to a logger
	 */
	private static final Logger LOG = Logger.getLogger(AdaFruitGPSSensor.class);

	/**
	 * ID for the Position response
	 */
	private static final String POSITION_TAG = "GPGGA";

	/**
	 * ID for the velocity response
	 */
	private static final String VELOCITY_TAG = "GPVTG";

	/**
	 * Buffer for fields in a response
	 */
	private final ArrayList<String> fields = new ArrayList<>();

	/**
	 * Buffer for reading responses
	 */
	protected BufferedReader serialBufferedReader;


	public AdaFruitGPSSensor(){
		try {
			uart = DeviceManager.open(UART_DEVICE_ID);
			uart.setBaudRate(9600);
			serialBufferedReader = new BufferedReader(new InputStreamReader(Channels.newInputStream(uart)));
			LOG.info("Opened GPS sensor");
		} catch (IOException ioe) {
			LOG.warn("Exception while trying to initialise GPS sensor", ioe);
		}

	}

	/**
	 * Get a line of raw data from the GPS sensor
	 *
	 * @return The complete line of data
	 * @throws IOException If there is an IO error
	 */
	private String readDataLine() throws IOException {
		String dataLine;
		/*
		 * All data lines start with a '$' character so keep reading until we find a
		 * valid line of data
		 */
		do {
			dataLine = serialBufferedReader.readLine();
			LOG.info("Got line : " + dataLine);

			// Got a valid line, so break out of the loop
			if (dataLine.startsWith("$")) {
				break;
			}
		} while (true);
		return dataLine;
	}

	/**
	 * Get a string of raw data from the GPS receiver.  How this happens is
	 * sub-class dependent.
	 *
	 * @param type The type of data to be retrieved
	 * @return A line of data for that type
	 * @throws IOException If there is an IO error
	 */
	public String getRawData(String type) throws IOException {
		String dataLine = null;

		do {
			/**
			 * Retrieve a line with the appropriate tag. Return null in the case of
			 * an error
			 */
			try {
				dataLine = readDataLine();
			} catch (IOException ex) {
				return null;
			}

			if(dataLine.length() < 7){
				System.out.println("Too little data : " + dataLine);
				continue;
			}

			// Extract the type of the data
			String dataType = dataLine.substring(1, 6);

			// If this is the type we're looking break out of the loop
			if (dataType.compareTo(type) == 0) {
				break;
			}
		} while (true);

		return dataLine.substring(7);
	}

	/**
	 * Get the current position
	 *
	 * @return The position data
	 * @throws IOException If there is an IO error
	 */
	public Position getPosition() {
		String rawData;
		long timeStamp = 0;
		double latitude = 0;
		double longitude = 0;
		double altitude = 0;
		char latitudeDirection = 0;
		char longitudeDirection = 0;

		// Read data repeatedly, until we have valid data
		while (true) {
			/*
			 * When rawData is returned, we have the correct tag, but we
			 * still need to check if the values are valid
			 */
			try {
				rawData = getRawData(POSITION_TAG);
			} catch (IOException e) {
				return null;
			}

			// Handle situation where we didn't get data
			if (rawData == null) {
				LOG.warn("NULL position data received");
				continue;
			}

			if (rawData.contains("$GP")) {
				LOG.warn("Corrupt position data");
				continue;
			}

			int fieldCount = splitCSVString(rawData);

			/*
			 * The position data must have 10 fields to it to be valid, so reject the
			 * data if we don't have the correct number
			 */
			if (fieldCount < 10) {
				LOG.warn("Incorrect position field count");
				continue;
			}
			LOG.info("position data = " + rawData);

			// Record a time stamp for the reading
			Date now = new Date();
			timeStamp = now.getTime() / 1000;

			/*
			 * Parse the relevant fields into values that we can use to create a new
			 * Position object.
			 */
			try {
				latitude = Double.parseDouble(fields.get(1)) / 100;
				latitudeDirection = fields.get(2).toCharArray()[0];
			} catch (NumberFormatException nfe) {
				LOG.warn("Badly formatted latitude number", nfe);
				continue;
			}

			try {
				longitude = Double.parseDouble(fields.get(3)) / 100;
				longitudeDirection = fields.get(4).toCharArray()[0];
			} catch (NumberFormatException nfe) {
				LOG.warn("Badly formatted longitude number", nfe);
				continue;
			}

			try {
				altitude = Double.parseDouble(fields.get(8));
			} catch (NumberFormatException nfe) {
				LOG.warn("Badly formatted altitude number", nfe);
				continue;
			}

      		// Passed all the tests so we have valid data
			break;
		}

    /* Return the encapsulated data */
		return new Position(timeStamp, latitude, latitudeDirection, longitude, longitudeDirection, altitude);
	}

	/**
	 * Get the current velocity
	 *
	 * @return The velocity data
	 * @throws IOException If there is an IO error
	 */
	public Velocity getVelocity() {

		double track = 0;
		double speed = 0;
		String rawData;
		while (true) {
			try {
				rawData = getRawData(VELOCITY_TAG);
			} catch (IOException e) {
				return null;
			}
			LOG.info("velocity data = " + rawData);

			// Handle the situation where we didn't get valid data
			if (rawData == null) {
				LOG.warn("NULL velocity data received");
				continue;
			}

			int fieldCount = splitCSVString(rawData);

			if (fieldCount < 8) {
				LOG.warn("Incorrect velocity field count, expected 8 got " + fieldCount);
				continue;
			}

			// Extract the track and velocity of the GPS receiver
			try {
				track = Double.parseDouble(fields.get(0));
			} catch (NumberFormatException nfe) {
				LOG.warn("Badly formatted track number", nfe);
				continue;
			}

			try {
				speed = Double.parseDouble(fields.get(6));
			} catch (NumberFormatException nfe) {
				LOG.warn("Badly formatted speed number", nfe);
				continue;
			}

			break;
		}



		// Record a time stamp for the reading
		Date now = new Date();
		long timeStamp = now.getTime() / 1000;

		// Return the Velocity object
		return new Velocity(timeStamp, track, speed);
	}

	/**
	 * Break a comma separated value string into its individual fields. We need to
	 * have this as explicit code because Java ME does not support String.split or
	 * java.util.regex and StringTokenizer has a bug that affects empty fields.
	 *
	 * @param input The CSV input string
	 * @return The number of fields extracted
	 */
	private int splitCSVString(String input) {
		// Clear the list of data fields
		fields.clear();
		int start = 0;
		int end;

		while ((end = input.indexOf(",", start)) != -1) {
			fields.add(input.substring(start, end));
			start = end + 1;
		}

		return fields.size();
	}

	/**
	 * Close the connection to the GPS receiver via the UART
	 *
	 * @throws IOException If there is an IO error
	 */
	@Override
	public void close() throws IOException {
		serialBufferedReader.close();
		uart.close();
	}
}
