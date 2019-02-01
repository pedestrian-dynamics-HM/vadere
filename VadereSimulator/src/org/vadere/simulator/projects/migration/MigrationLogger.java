package org.vadere.simulator.projects.migration;

import org.vadere.util.io.IOUtils;

import java.io.IOException;

/**
 *  Semi-Wrapper for logging in MigrationAssistant. For each project a separate logfile should be
 *  created, containing the standard log output. This Class will collect the logging and write it
 *  to the given file if needed. To remove duplicate code, the {@link #last()} method returns the
 *  last added log statement to be used for the logging framework.
 */
public class MigrationLogger {

	private StringBuilder sb;
	private String last;

	public MigrationLogger(){
		sb = new StringBuilder();
	}

	public void rest(){
		sb.setLength(0);
	}

	public void writeLog(String filePath) throws IOException {
		IOUtils.writeTextFile(filePath, sb.toString());
		rest();
	}

	public String getLog(){
		return sb.toString();
	}

	public void error(String message){
		sb.append("ERROR:").append(message).append("\n");
		last = message;
	}

	public void warn(String message){
		sb.append("WARN:").append(message).append("\n");
		last = message;
	}

	public void info(String message){
		sb.append("Info:").append(message).append("\n");
		last = message;
	}

	public String last(){
		return last;
	}
}
