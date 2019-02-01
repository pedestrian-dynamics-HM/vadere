package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import org.vadere.util.logging.Logger;

import java.util.Map;

public class SetLogNameCommand implements ArgumentAction {
    @Override
    public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
        String filename = (String) value;
//        Logger.setFileName(filename); //todo set Filename of Log-file
    }

    @Override
    public void onAttach(Argument arg) {

    }

    @Override
    public boolean consumeArgument() {
        return true;
    }
}
