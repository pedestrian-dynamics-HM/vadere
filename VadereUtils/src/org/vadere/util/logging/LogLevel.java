package org.vadere.util.logging;

public class LogLevel {

	public static final String OFF_STR = "OFF";
	public static final String FATAL_STR = "FATAL";
	public static final String ERROR_STR = "ERROR";
	public static final String WARN_STR = "WARN";
	public static final String INFO_STR = "INFO";
	public static final String DEBUG_STR = "DEBUG";
	public static final String ALL_STR = "ALL";

	public static final LogLevel OFF = new LogLevel(OFF_STR);
	public static final LogLevel FATAL = new LogLevel(FATAL_STR);
	public static final LogLevel ERROR = new LogLevel(ERROR_STR);
	public static final LogLevel WARN = new LogLevel(WARN_STR);
	public static final LogLevel INFO = new LogLevel(INFO_STR);
	public static final LogLevel DEBUG = new LogLevel(DEBUG_STR);
	public static final LogLevel ALL = new LogLevel(ALL_STR);


	private String levelStr;

	private LogLevel(String levelStr) {
		this.levelStr = levelStr;
	}

	public String getLevelStr() {
		return levelStr;
	}

	public static LogLevel toLogLevel(String levelStr){
		LogLevel logLevel;
		switch (levelStr) {
			case OFF_STR: logLevel = OFF;
			break;
			case FATAL_STR: logLevel = FATAL;
			break;
			case ERROR_STR: logLevel = ERROR;
			break;
			case WARN_STR: logLevel = WARN;
			break;
			case INFO_STR: logLevel = INFO;
			break;
			case DEBUG_STR: logLevel = DEBUG;
			break;
			case ALL_STR: logLevel = ALL;
			default: logLevel = INFO;
		}
		return logLevel;
	}

}
