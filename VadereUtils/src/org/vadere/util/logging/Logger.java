package org.vadere.util.logging;

import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;


public class Logger {

	org.apache.log4j.Logger logger;

	public static Logger getLogger(Class clazz){
		return new Logger(org.apache.log4j.Logger.getLogger(clazz));
	}
	public static Logger getRootLogger() {
		return new Logger(org.apache.log4j.Logger.getRootLogger());
	}

	public static void setFileName(String fileName){
		RollingFileAppender appender = new RollingFileAppender();
		appender.setName(fileName);
		appender.setFile(fileName);
		appender.setAppend(false);
		appender.setMaxFileSize("10000KB");
		appender.setLayout(new PatternLayout("%d{ABSOLUTE} %5p [%t] %c{1}:%L - %m%n"));
		appender.activateOptions();

		org.apache.log4j.Logger.getRootLogger().addAppender(appender);
	}


	private Logger(org.apache.log4j.Logger logger){
		this.logger = logger;
	}


	public void setLevel(LogLevel logLevel){
		logger.setLevel(Level.toLevel(logLevel.getLevelStr()));
	}

	public LogLevel getLevel(){
		Level level = logger.getLevel();
		LogLevel logLevel;
		switch (level.toInt()){
			case  Level.ALL_INT : logLevel = LogLevel.ALL;
			break;
			case Level.DEBUG_INT: logLevel = LogLevel.DEBUG;
			break;
			case Level.ERROR_INT: logLevel = LogLevel.ERROR;
			break;
			case Level.FATAL_INT: logLevel = LogLevel.FATAL;
			break;
			case Level.INFO_INT: logLevel = LogLevel.INFO;
			break;
			case Level.OFF_INT: logLevel = LogLevel.OFF;
			break;
			case Level.WARN_INT: logLevel = LogLevel.WARN;
			default: logLevel = LogLevel.INFO;
		}

		return logLevel;
	}


	public void addAppender(Appender appender){
		logger.addAppender(appender);
	}

	public void info(String message, Object... args){
		logger.log(Logger.class.getName(), Level.INFO, String.format(message, args), null);
	}

	public void info(Object message) {
		logger.log(Logger.class.getName(), Level.INFO, message, null);
	}

	public void info(Object message, Throwable t) {
		logger.log(Logger.class.getName(), Level.INFO, message, t);
	}

	public void warn(String message, Object... args){
		logger.log(Logger.class.getName(), Level.WARN, String.format(message, args), null);
	}

	public void warn(Object message) {
		logger.log(Logger.class.getName(), Level.WARN, message, null);
	}

	public void warn(Object message, Throwable t) {
		logger.log(Logger.class.getName(), Level.WARN, message, t);
	}

	public void error(String message, Object... args){
		logger.log(Logger.class.getName(), Level.ERROR, String.format(message, args), null);
	}

	public void error(Object message) {
		logger.log(Logger.class.getName(), Level.ERROR, message, null);
	}

	public void error(Object message, Throwable t) {
		logger.log(Logger.class.getName(), Level.ERROR, message, t);
	}

	public void debug(String message, Object... args){
		logger.log(Logger.class.getName(), Level.DEBUG, String.format(message, args), null);
	}

	public void debug(Object message){
		logger.log(Logger.class.getName(), Level.DEBUG, message, null);
	}

	public void debug(Object message, Throwable t){
		logger.log(Logger.class.getName(), Level.DEBUG, message, t);
	}

}
