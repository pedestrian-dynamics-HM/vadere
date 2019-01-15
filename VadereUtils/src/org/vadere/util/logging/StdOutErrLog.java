package org.vadere.util.logging;

import org.apache.log4j.Logger;

import java.io.PrintStream;

public class StdOutErrLog {
	private static final Logger logger = Logger.getLogger(StdOutErrLog.class);

	public static void addStdOutErrToLog(){
		System.setOut(redirectOut(System.out));
		System.setErr(redirectErr(System.err));
		logger.info("Redirect StdOut and StdErr");
	}

	private static PrintStream redirectOut(PrintStream baseStream) {
		return new PrintStream(baseStream){
			public void print(final String s){
				baseStream.print(s);
				logger.info(s);
			}
		};
	}


	private static PrintStream redirectErr(PrintStream baseStream) {
		return new PrintStream(baseStream){
			public void print(final String s){
				baseStream.print(s);
				logger.error(s);
			}
		};
	}

}
