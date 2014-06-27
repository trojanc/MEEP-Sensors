package coza.trojanc.meepsensors;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by Charl on 2014-06-27.
 */
public class Logger {

	private static final Map<String, Logger> LOGGER_MAP = new HashMap<>();


	private java.util.logging.Logger LOG;

	private Logger(String name){
		this.LOG = java.util.logging.Logger.getLogger(name);
		LOGGER_MAP.put(name, this);
	}

	public static Logger getLogger(Class clazz){
		return getLogger(clazz.getClass().getName());
	}

	public static Logger getLogger(String name){
		if(LOGGER_MAP.containsKey(name)){
			return LOGGER_MAP.get(name);
		}else{
			return new Logger(name);
		}
	}


	public void debug(String message){
		LOG.log(Level.FINE, message);
	}


	public void info(String message){
		LOG.log(Level.INFO, message);
	}

	public void warn(String message){
		LOG.log(Level.WARNING, message);
	}

	public void warn(String message, Throwable th){
		LOG.log(Level.WARNING, message, th);
	}

	public void severe(String message, Throwable th){
		LOG.log(Level.SEVERE, message, th);
	}

}
