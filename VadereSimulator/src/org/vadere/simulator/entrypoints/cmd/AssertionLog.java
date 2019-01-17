package org.vadere.simulator.entrypoints.cmd;

import org.vadere.util.logging.Logger;
import org.vadere.util.logging.StdOutErrLog;

public class AssertionLog {

	private static Logger logger = Logger.getLogger(AssertionLog.class);

	public static void main(String[]args){
		AssertionLog a = new AssertionLog();
		StdOutErrLog.addStdOutErrToLog();
		a.logAssertion();
	}

	public void logAssertion(){
		logger.info("Test Info");
		logger.warn("Test Warn");
		logger.error("Test Err");
		logger.debugf("Test Debug %d", 5);
		assert false : "Fail";
	}

}

