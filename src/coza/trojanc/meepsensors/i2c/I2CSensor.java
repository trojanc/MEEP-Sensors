package coza.trojanc.meepsensors.i2c;

import coza.trojanc.meepsensors.Logger;
import jdk.dio.DeviceManager;
import jdk.dio.i2cbus.I2CDevice;
import jdk.dio.i2cbus.I2CDeviceConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Charl on 2014-06-27.
 */
public abstract class I2CSensor {

	private static final Logger LOG = Logger.getLogger(I2CSensor.class);

	/**
	 * Reference to the I2C device
	 */
	protected I2CDevice i2cDevice = null;             // I2C device

	/**
	 * Default I2C Bus
	 */
	public static final int DEFAULT_BUS = 1;

	/**
	 * Default clock speed
	 * 3.4Mhz
	 */
	public static final int DEFAULT_CLOCK = 3400000;

	/**
	 * Default address bits size. 7 bits
	 */
	public static final int DEFAULT_ADDRESS_BITS = 7;               // default address

	/**
	 * Size of the registry
	 */
	private final int registrySize = 1;

	/**
	 * Byte buffer for sending commands
	 */
	private ByteBuffer bufferOut;

	/**
	 * Byte buffer for reading responses
	 */
	private ByteBuffer bufferIn;

	/**
	 * Constructor for the I2C sensor. This method update the device address and
	 * try to connect to the device
	 *
	 * @param address Device's address
	 */
	public I2CSensor(int address) {
		this(DEFAULT_BUS, address, DEFAULT_ADDRESS_BITS, DEFAULT_CLOCK);
	}

	/**
	 * Constructor for the I2C sensor.
	 *
	 * @param i2cBus Device bus.
	 * @param address Device address
	 * @param addressSizeBits I2C normally uses 7 bits addresses
	 * @param serialClock Clock speed
	 */
	public I2CSensor(int i2cBus, int address, int addressSizeBits, int serialClock) {
		connectToDevice(i2cBus, address, addressSizeBits, serialClock);
	}

	/**
	 * This method tries to connect to the I2C device, initializing i2cDevice
	 * variable
	 */
	private void connectToDevice(int i2cBus, int address, int addressSizeBits, int serialClock) {
		bufferOut = ByteBuffer.allocateDirect(registrySize);
		bufferIn = ByteBuffer.allocateDirect(1);
		try {
			I2CDeviceConfig config = new I2CDeviceConfig(i2cBus, address, addressSizeBits, serialClock);
			i2cDevice = DeviceManager.open(config);
			LOG.info("Connected to the device OK.");
		} catch (IOException e) {
			LOG.warn("Exception trying to connect to device.", e);
		}
	}

	/**
	 * Writes a singe byte to a registry
	 *
	 * @param registry Registry to write
	 * @param byteToWrite Byte to be written
	 */
	public void write(int registry, byte byteToWrite) {
		bufferOut.clear();
		bufferOut.put(byteToWrite);
		bufferOut.rewind();
		try {
			i2cDevice.write(registry, registrySize, bufferOut);
		} catch (IOException e) {
			LOG.warn("Error writing registry", e);
		}

	}

	/**
	 * This method reads one byte from a specified registry address. The method
	 * checks that the byte is actually read, otherwise it'll show some messages
	 * in the output
	 *
	 * @param registry Registry to be read
	 * @return Byte read from the registry
	 */
	public byte read(int registry) {
		bufferIn.clear();
		int result = -1;
		try {
			result = i2cDevice.read(registry, registrySize, bufferIn);
		} catch (IOException e) {
			LOG.warn("Error reading byte", e);
		}
		if (result < 1) {
			LOG.warn("Byte could not be read");
		} else {
			bufferIn.rewind();
			return bufferIn.get();
		}
		return 0;
	}

	public void close(){
		try {
			i2cDevice.close();
		} catch (IOException e) {
			LOG.warn("Exception trying to close I2C device");
		}
	}
}
