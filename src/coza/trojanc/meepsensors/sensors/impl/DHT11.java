/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package coza.trojanc.meepsensors.sensors.impl;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import coza.trojanc.meepsensors.sensors.HumiditySensor;
import coza.trojanc.meepsensors.sensors.TemperatureSensor;
import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

/**
 * WARNING, This class is still untested. Java ME 8 does not support one wire coms for raspberry pi. So this code is not tested!
 */
public class DHT11 implements TemperatureSensor, HumiditySensor{
	
	public static final int DHTLIB_OK = 0;
	public static final int DHTLIB_ERROR_CHECKSUM =	-1;
	public static final int DHTLIB_ERROR_TIMEOUT = -2;
	private static final boolean LOW = false;
	private static final boolean HIGH = true;
	
	private final int pinNumber;
	
	private int humidity = 0;
	private int temperature = 0;
	
	private GPIOPin dhtPin;
	
	public DHT11() throws IOException{
		this(17);
	}
	
	public DHT11(int pin) throws IOException{
		pinNumber = pin;
		dhtPin = (GPIOPin)DeviceManager.open(new GPIOPinConfig(0, pin, GPIOPinConfig.DIR_BOTH_INIT_INPUT, GPIOPinConfig.DEFAULT, GPIOPinConfig.TRIGGER_NONE, LOW));
	}
	
	private int read() throws IOException, InterruptedException{
		// BUFFER TO RECEIVE
		int[] bits = new int[5];
		int cnt = 7;
		int idx = 0;
		// EMPTY BUFFER
		for (int i=0; i< 5; i++) bits[i] = 0;
		/*
		 * MCU will set Data Single-bus voltage level from high to low
		 * and this process must take at least 18ms to ensure DHT’s detection
		 * of MCU's signal,
		 */
		dhtPin.setDirection(GPIOPin.OUTPUT);
		dhtPin.setValue(LOW);
		delay(18);

		/**
		 * MCU will pull up voltage and wait 20-40us for DHT’s response.
		 */
		dhtPin.setValue(HIGH);
		delayMicroseconds(40);
		dhtPin.setDirection(GPIOPin.INPUT);

		/**
		 * Once DHT detects the start signal, it will send out a
		 * low-voltage-level response signal, which lasts 80us
		 */
		int loopCnt = 90; // Wait up to 90us for some inacuracy threashold
		while(dhtPin.getValue() == LOW){
			delayMicroseconds(1);
			if (loopCnt-- == 0) return DHTLIB_ERROR_TIMEOUT;
		}

		/**
		 * Then the programme of DHT sets Data Single-bus voltage level from
		 * low to high and keeps it for 80us for DHT’s preparation for sending data
		 */
		loopCnt = 90;// Wait up to 90us for some inacuracy threashold
		while(dhtPin.getValue() == HIGH){
			delayMicroseconds(1);
			if (loopCnt-- == 0) return DHTLIB_ERROR_TIMEOUT;
		}

		// READ OUTPUT - 40 BITS => 5 BYTES or TIMEOUT
		for (int i=0; i<40; i++){
			/**
			 * When DHT is sending data to MCU, every bit of data begins
			 * with the 50us low-voltage-level and the length of the
			 * following high-voltage-level signal determines whether
			 * data bit is "0" or "1"
			 */
			loopCnt = 60;  // Wait up to 60us for some inacuracy threashold
			while(dhtPin.getValue() == LOW){
				delayMicroseconds(1);
				if (loopCnt-- == 0) return DHTLIB_ERROR_TIMEOUT;
			}

			long t = micros();

			loopCnt = 100;
			while(dhtPin.getValue() == HIGH){
				if (loopCnt-- == 0) return DHTLIB_ERROR_TIMEOUT;
			}

			/**
			 * 26-28us indicates a '0' bit
			 * 70us indicates a '1' bit
			 * We will wait up to 40us to determine if it's a high bit
			 */
			if ((micros() - t) > 40){
				bits[idx] |= (1 << cnt); // shift in a '1' bit
			}
			if (cnt == 0){   // next byte?
				cnt = 7;	// restart at MSB
				idx++;	  // next byte!
			}
			else cnt--; // Move to next bit
		}

		// WRITE TO RIGHT VARS
		// as bits[1] and bits[3] are allways zero they are omitted in formulas.
		humidity	= bits[0];
		temperature = bits[2];

		int sum = bits[0] + bits[2];

		if (bits[4] != sum) return DHTLIB_ERROR_CHECKSUM;
		return DHTLIB_OK;
	}
	
	public void close(){
		try {
			dhtPin.close();
		} catch (IOException ex) {
			Logger.getLogger(DHT11.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	private void delay(int miliseconds) throws InterruptedException{
		Thread.sleep(miliseconds);
	}
	
	private void delayMicroseconds(int microseconds) throws InterruptedException{
		Thread.sleep(0, microseconds*1000);
	}
	
	private long micros(){
		return System.nanoTime()/1000;
	}

	@Override
	public double getHumidity() {
		try {
			read();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return humidity;
	}

	@Override
	public double getTemparature() {
		try {
			read();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return temperature;
	}
}
