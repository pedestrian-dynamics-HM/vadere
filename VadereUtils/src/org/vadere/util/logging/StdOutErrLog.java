package org.vadere.util.logging;


import java.io.PrintStream;

public class StdOutErrLog {
	private static final Logger logger = Logger.getLogger("STDOUTERR");

	/**
	 * redirect StdOut and StdErr to logfile with custom log level STDOUT and STDERR.
	 */
	public static void addStdOutErrToLog(){
		// check first if logging config is overwritten programatically.
		// If so, do not redirect output. This was already done in this case.
		if(!Logger.isConfigOverwritten()){
			System.setOut(redirectOut(System.out));
			System.setErr(redirectErr(System.err));
			logger.info("Redirect StdOut and StdErr");
		}
	}

	private static PrintStream redirectOut(PrintStream baseStream) {
		return new PrintStream(baseStream){
			public void print(final String s){
				baseStream.print(s);
				logger.stdout(s);
			}
		};
	}


	private static PrintStream redirectErr(PrintStream baseStream) {
		return new PrintStream(baseStream){
			public void print(final String s){
				baseStream.print(s);
				logger.stderr(s);
			}
		};
	}

}
