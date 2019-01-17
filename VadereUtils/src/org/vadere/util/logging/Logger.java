package org.vadere.util.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLoggerWrapper;
import org.apache.logging.log4j.util.MessageSupplier;
import org.apache.logging.log4j.util.Supplier;

import javax.security.auth.login.Configuration;

/**
 * Extended Logger interface with convenience methods for
 * the DIAG, NOTICE and VERBOSE custom log levels.
 * <p>Compatible with Log4j 2.6 or higher.</p>
 */
public final class Logger extends ExtendedLoggerWrapper {
	private static final long serialVersionUID = 32544032022288L;
	private final ExtendedLoggerWrapper logger;

	private static final String FQCN = Logger.class.getName();


	private Logger(final org.apache.logging.log4j.Logger logger) {
		super((AbstractLogger) logger, logger.getName(), logger.getMessageFactory());
		this.logger = this;
	}

	public static void setLevel(String levelStr){
		Level level = Level.toLevel(levelStr);
		Configurator.setAllLevels(LogManager.getRootLogger().getName(), level);
	}

	/**
	 * Returns a custom Logger with the name of the calling class.
	 *
	 * @return The custom Logger for the calling class.
	 */
	public static Logger getLogger() {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger();
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger using the fully qualified name of the Class as
	 * the Logger name.
	 *
	 * @param loggerName The Class whose name should be used as the Logger name.
	 *            If null it will default to the calling class.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final Class<?> loggerName) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(loggerName);
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger using the fully qualified name of the Class as
	 * the Logger name.
	 *
	 * @param loggerName The Class whose name should be used as the Logger name.
	 *            If null it will default to the calling class.
	 * @param messageFactory The message factory is used only when creating a
	 *            logger, subsequent use does not change the logger but will log
	 *            a warning if mismatched.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final Class<?> loggerName, final MessageFactory messageFactory) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(loggerName, messageFactory);
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger using the fully qualified class name of the value
	 * as the Logger name.
	 *
	 * @param value The value whose class name should be used as the Logger
	 *            name. If null the name of the calling class will be used as
	 *            the logger name.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final Object value) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(value);
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger using the fully qualified class name of the value
	 * as the Logger name.
	 *
	 * @param value The value whose class name should be used as the Logger
	 *            name. If null the name of the calling class will be used as
	 *            the logger name.
	 * @param messageFactory The message factory is used only when creating a
	 *            logger, subsequent use does not change the logger but will log
	 *            a warning if mismatched.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final Object value, final MessageFactory messageFactory) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(value, messageFactory);
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger with the specified name.
	 *
	 * @param name The logger name. If null the name of the calling class will
	 *            be used.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final String name) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(name);
		return new Logger(wrapped);
	}

	/**
	 * Returns a custom Logger with the specified name.
	 *
	 * @param name The logger name. If null the name of the calling class will
	 *            be used.
	 * @param messageFactory The message factory is used only when creating a
	 *            logger, subsequent use does not change the logger but will log
	 *            a warning if mismatched.
	 * @return The custom Logger.
	 */
	public static Logger getLogger(final String name, final MessageFactory messageFactory) {
		final org.apache.logging.log4j.Logger wrapped = LogManager.getLogger(name, messageFactory);
		return new Logger(wrapped);
	}


	public void setInfo(){
		Configurator.setLevel(logger.getName(), Level.INFO);
	}

	public void setDebug(){
		Configurator.setLevel(logger.getName(), Level.DEBUG);
	}

}

