package org.vadere.simulator.projects.migration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.PrintStream;

public class StdOutRedirecter {

	private Logger logger = LogManager.getLogger(StdOutRedirecter.class);
	private PrintStream defaultOut;
	private PrintStream defaultErr;

	public StdOutRedirecter(){
//		this.logger = logger;
	}

	public void redirect(){
		defaultOut = System.out;
		defaultErr = System.err;

		System.setOut(useLogger(System.out));
		System.setErr(useLogger(System.err));

	}

	private PrintStream useLogger(final PrintStream realPrintStream){
		return  new PrintStream(realPrintStream){
			@Override
			public void print(final String string){
				logger.warn(string);
			}

			@Override
			public void println(final String string){
				logger.warn(string);
			}
		};
	}

	public void reset(){
		System.setOut(defaultOut);
		System.setErr(defaultErr);
	}
}
