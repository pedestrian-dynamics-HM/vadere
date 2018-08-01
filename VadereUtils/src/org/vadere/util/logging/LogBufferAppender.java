package org.vadere.util.logging;


import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * This appender is used to collect all log statements create by the underling logger and to
 * programmatically create the String representation of the log events.
 *
 * This useful if you want to show the Log of a specific Logger to the user at runtime.
 * At the time of writing this is used in the Migration assistant to show the user any
 * problems during scenario file migration. This Appender can be used with any Logger in vader.
 */
public class LogBufferAppender extends AppenderSkeleton {

	private StringBuffer sb;
	private Layout layout;

	public LogBufferAppender() {
		sb = new StringBuffer();
		layout = new PatternLayout("%d{ABSOLUTE} %5p %c{1}:%L - %m%n");
	}

	public String getMigrationLog(){
		return sb.toString();
	}


	public void rest(){
		sb.setLength(0);
	}

	@Override
	protected void append(LoggingEvent loggingEvent) {
		sb.append(layout.format(loggingEvent));
	}

	@Override
	public boolean requiresLayout() {
		return true;
	}

	@Override
	public void close() {
		sb.setLength(0);
		sb = null;
	}
}
