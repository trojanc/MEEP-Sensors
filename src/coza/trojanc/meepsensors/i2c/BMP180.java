package coza.trojanc.meepsensors.i2c;

import coza.trojanc.meepsensors.Logger;
import coza.trojanc.meepsensors.sensors.BarometricSensor;
import coza.trojanc.meepsensors.sensors.TemperatureSensor;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author Charl
 */
public class BMP180 extends I2CSensor implements BarometricSensor, TemperatureSensor {

	/** A reference to a logger */
	private static final Logger LOG = Logger.getLogger(I2CSensor.class);

	/**
	 * Device address BMP180 address is 0x77
	 */
	private static final int BMP180_ADDR = 0x77;

	/**
	 * EEPROM registers - these represent calibration data
 	 */
	private short AC1;
	private short AC2;
	private short AC3;
	private int AC4;
	private int AC5;
	private int AC6;
	private short B1;
	private short B2;
	private short MB;
	private short MC;
	private short MD;

	/**
	 * Variable common between temperature & pressure calculations
	 */
	protected int B5;

	/**
	 * Total of bytes use for callibration
	 */
	private static final int CALIBRATION_BYTES = 22;

	/**
	 * Address byte length
	 * Size of each address (in bytes)
	 */
	private final int SUB_ADDRESS_BYTES = 1;

	/**
	 * EEPROM address data
	 */
	private final int EEPROM_start = 0xAA;

	/**
	 * BMP180 control registry
	 */
	private final int CONTROL_REGISTER = 0xF4;


	// Temperature read address
	private static final int TEMP_ADDR = 0xF6;

	// Read temperature command
	private static final byte GET_TEMP_CMD = (byte) 0x2E;

	//Uncompensated Temperature data
	private int UT;

	// Temperature read address
	private static final int PRESS_ADDR = 0xF6;

	// Uncompensated pressure
	private int UP;

	//Barometer configuration
	private byte pressureCmd;
	private int delay;
	private int oss;

	// Shared ByteBuffers
	private ByteBuffer uncompTemp;

	public BMP180(){
		super(BMP180_ADDR);
		uncompTemp = ByteBuffer.allocateDirect(2);
		initDevice(BMPMode.STANDARD);
	}


	/**
	 * This method read the calibration data common for the Temperature sensor and
	 * Barometer sensor included in the BMP180
	 */
	private void initDevice(BMPMode mode) {
		pressureCmd = mode.getCommand();
		delay = mode.getDelay();
		oss = mode.getOSS();
		try {
			//Small delay before starting
			Thread.sleep(500);
			//Getting calibration data
			gettingCalibration();
		} catch (IOException | InterruptedException e) {
			LOG.severe("Exception while initDevice()", e);
		}
	}
	/**
	 * Method for reading the temperature. Remember the sensor will provide us
	 * with raw data, and we need to transform in some analyzed value to make
	 * sense. All the calculations are normally provided by the manufacturer. In
	 * our case we use the calibration data collected at construction time.
	 *
	 * @return Temperature in Celsius as a double
	 * @throws IOException If there is an IO error reading the sensor
	 */
	@Override
	public double getTemparature(){
		try {

		// Write the read temperature command to the command register
		write(CONTROL_REGISTER, GET_TEMP_CMD);

		// Delay before reading the temperature
		try {
			Thread.sleep(100);
		} catch (InterruptedException ex) {
		}

		//Read uncompressed data
		uncompTemp.clear();
		int result = i2cDevice.read(TEMP_ADDR, SUB_ADDRESS_BYTES, uncompTemp);
		if (result < 2) {
			LOG.warn("Not enough data for temperature read");
		}

		// Get the uncompensated temperature as a signed two byte word
		uncompTemp.rewind();
		byte[] data = new byte[2];
		uncompTemp.get(data);
		UT = ((data[0] << 8) & 0xFF00) + (data[1] & 0xFF);

		// Calculate the actual temperature
		int X1 = ((UT - AC6) * AC5) >> 15;
		int X2 = (MC << 11) / (X1 + MD);
		B5 = X1 + X2;
		float celsius = (float) ((B5 + 8) >> 4) / 10;

		return celsius;
		}
		catch(IOException e){
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Read the barometric pressure (in hPa) from the device.
	 *
	 * @return double Pressure measurement in hPa
	 */
	@Override
	public double getPressure(){
		try{
			// Write the read pressure command to the command register
			write(CONTROL_REGISTER, pressureCmd);

			// Delay before reading the pressure - use the value determined by the oversampling setting (mode)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException ex) {
			}

			// Read the uncompensated pressure value
			ByteBuffer uncompPress = ByteBuffer.allocateDirect(3);
			int result = i2cDevice.read(PRESS_ADDR, SUB_ADDRESS_BYTES, uncompPress);
			if (result < 3) {
				LOG.warn("Couldn't read all bytes, only read = " + result);
				return 0;
			}

			// Get the uncompensated pressure as a three byte word
			uncompPress.rewind();

			byte[] data = new byte[3];
			uncompPress.get(data);

			UP = ((((data[0] << 16) & 0xFF0000) + ((data[1] << 8) & 0xFF00) + (data[2] & 0xFF)) >> (8 - oss));

			// Calculate the true pressure
			int B6 = B5 - 4000;
			int X1 = (B2 * (B6 * B6) >> 12) >> 11;
			int X2 = AC2 * B6 >> 11;
			int X3 = X1 + X2;
			int B3 = ((((AC1 * 4) + X3) << oss) + 2) / 4;
			X1 = AC3 * B6 >> 13;
			X2 = (B1 * ((B6 * B6) >> 12)) >> 16;
			X3 = ((X1 + X2) + 2) >> 2;
			int B4 = (AC4 * (X3 + 32768)) >> 15;
			int B7 = (UP - B3) * (50000 >> oss);

			int Pa;
			if (B7 < 0x80000000) {
				Pa = (B7 * 2) / B4;
			} else {
				Pa = (B7 / B4) * 2;
			}

			X1 = (Pa >> 8) * (Pa >> 8);
			X1 = (X1 * 3038) >> 16;
			X2 = (-7357 * Pa) >> 16;

			Pa += ((X1 + X2 + 3791) >> 4);

			return (Pa) / 100;

		}catch (IOException e){
			e.printStackTrace();
			return -1;
		}
	}


	/**
	 * Method for reading the calibration data. Do not worry too much about this
	 * method. Normally this information is given in the device information sheet.
	 *
	 * @throws IOException
	 */
	public void gettingCalibration() throws IOException {
		// Read all of the calibration data into a byte array
		ByteBuffer calibData = ByteBuffer.allocateDirect(CALIBRATION_BYTES);
		int result = i2cDevice.read(EEPROM_start, SUB_ADDRESS_BYTES, calibData);
		if (result < CALIBRATION_BYTES) {
			LOG.warn("Not all the callibration bytes were read");
			return;
		}
		// Read each of the pairs of data as a signed short
		calibData.rewind();
		AC1 = calibData.getShort();
		AC2 = calibData.getShort();
		AC3 = calibData.getShort();

		// Unsigned short values
		byte[] data = new byte[2];
		calibData.get(data);
		AC4 = (((data[0] << 8) & 0xFF00) + (data[1] & 0xFF));
		calibData.get(data);
		AC5 = (((data[0] << 8) & 0xFF00) + (data[1] & 0xFF));
		calibData.get(data);
		AC6 = (((data[0] << 8) & 0xFF00) + (data[1] & 0xFF));

		// Signed sort values
		B1 = calibData.getShort();
		B2 = calibData.getShort();
		MB = calibData.getShort();
		MC = calibData.getShort();
		MD = calibData.getShort();
	}

	/**
	 * Relationship between Oversampling Setting and conversion delay (in ms) for each Oversampling Setting constant
	 * Ultra low power:        4.5 ms minimum conversion delay
	 * Standard:               7.5 ms
	 * High Resolution:       13.5 ms
	 * Ultra high Resolution: 25.5 ms
	 */
	public enum BMPMode {

		ULTRA_LOW_POWER(0, 5),
		STANDARD(1, 8),
		HIGH_RESOLUTION(2, 14),
		ULTRA_HIGH_RESOLUTION(3, 26);

		/**
		 * Over sample setting value
		 */
		private final int oss;

		/**
		 * Minimum conversion time in ms
		 */
		private final int delay;

		/**
		 * Read pressure command
		 */
		private static final byte GET_PRESSURE_COMMAND = (byte) 0x34;

		/**
		 * Command byte to read pressure
		 */
		private final byte cmd;

		/**
		 * Create a new instance of a BMPMode
		 * @param oss
		 * @param delay
		 */
		BMPMode(int oss, int delay) {
			this.oss = oss;
			this.delay = delay;
			this.cmd = (byte) (GET_PRESSURE_COMMAND + ((oss << 6) & 0xC0));
		}

		/**
		 * Return the conversion delay (in ms) associated with this oversampling
		 * setting
		 *
		 * @return delay
		 */
		public int getDelay() {
			return delay;
		}

		/**
		 * Return the command to the control register for this oversampling setting
		 *
		 * @return command
		 */
		public byte getCommand() {
			return cmd;
		}

		/**
		 * Return this oversampling setting
		 *
		 * @return Oversampling setting
		 */
		public int getOSS() {
			return oss;
		}
	}
}
