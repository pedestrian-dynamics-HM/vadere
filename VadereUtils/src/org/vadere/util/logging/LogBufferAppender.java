//package org.vadere.util.logging;
//
//
//import org.apache.logging.log4j.core.Filter;
//import org.apache.logging.log4j.core.Layout;
//import org.apache.logging.log4j.core.LogEvent;
//import org.apache.logging.log4j.core.appender.AbstractAppender;
//import org.apache.logging.log4j.core.config.plugins.Plugin;
//import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
//import org.apache.logging.log4j.core.config.plugins.PluginElement;
//import org.apache.logging.log4j.core.config.plugins.PluginFactory;
//import org.apache.logging.log4j.core.layout.PatternLayout;
//
//import java.io.Serializable;
//
///**
// * This appender is used to collect all log statements create by the underling logger and to
// * programmatically create the String representation of the log events.
// *
// * This useful if you want to show the Log of a specific Logger to the user at runtime.
// * At the time of writing this is used in the Migration assistant to show the user any
// * problems during scenario file migration. This Appender can be used with any Logger in vader.
// */
//@Plugin(name="MigrationAppender", category="Core", elementType="appender", printObject=true)
//public class LogBufferAppender extends AbstractAppender {
//
//	private StringBuffer sb;
//	private Layout layout;
//
//	protected LogBufferAppender(String name, Filter filter, Layout<? extends Serializable> layout,
//								boolean ignoreExceptions) {
//		super(name, filter, layout, ignoreExceptions);
//		sb = new StringBuffer();
//	}
//
//	public String getMigrationLog(){
//		return sb.toString();
//	}
//
//	public void rest(){
//		sb.setLength(0);
//	}
//
//	@Override
//	public void append(LogEvent logEvent) {
//		sb.append(new String(getLayout().toByteArray(logEvent)));
//	}
//
//	@PluginFactory
//	public static LogBufferAppender createAppender(
//			@PluginAttribute("name") String name,
//			@PluginElement("Layout") Layout<? extends Serializable> layout,
//			@PluginElement("Filter") final Filter filter,
//			@PluginAttribute("otherAttribute") String otherAttribute) {
//		if (name == null) {
//			LOGGER.error("No name provided for MyCustomAppenderImpl");
//			return null;
//		}
//		if (layout == null) {
//			layout = PatternLayout.createDefaultLayout();
//		}
//		return new LogBufferAppender(name, filter, layout, true);
//	}
//}
