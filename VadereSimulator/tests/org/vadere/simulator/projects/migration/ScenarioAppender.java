package org.vadere.simulator.projects.migration;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class ScenarioAppender extends AppenderSkeleton {

	private final Path basePath;
	private HashMap<String, RollingFileAppender> appenders;

	public ScenarioAppender(Path basePath) {
		this.basePath = basePath;
		this.appenders = new HashMap<>();
	}

	private RollingFileAppender createAppender(String name) {
		Path loggingBasePath = basePath.resolve(name);
		if (!loggingBasePath.toFile().exists()) {
			try {
				Files.createDirectories(loggingBasePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String filePath = Paths.get(loggingBasePath.toString(), "log.out").toString();
		RollingFileAppender appender = null;
		try {
			appender = new RollingFileAppender(new PatternLayout("%d{ABSOLUTE} %5p %c{1}:%L - %m%n"), filePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		appender.setName(name);
		return appender;
	}

	@Override
	protected void append(LoggingEvent event) {
		Object obj = event.getMDC("scenario.Name");
		if (obj != null) {
			String appenderName = (String) obj;
			if (appenders.containsKey(appenderName)) {
				appenders.get(appenderName).doAppend(event);
			} else {
				RollingFileAppender appender = createAppender(appenderName);
				appenders.put(appenderName, appender);
				appender.doAppend(event);
			}
		}
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	public void close() {
		appenders.forEach((k, v) -> v.close());
	}
}
