package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.util.Map;

public class SetLogLevelCommand implements ArgumentAction {
	@Override
	public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
		Level level = Level.toLevel((String) value);
		Logger.getRootLogger().setLevel(level);
	}

	@Override
	public void onAttach(Argument arg) {

	}

	@Override
	public boolean consumeArgument() {
		return true;
	}
}
