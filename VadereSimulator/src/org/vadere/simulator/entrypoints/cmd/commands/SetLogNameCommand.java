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

        // FIXME: Create a "FileAppender" which writes to "filename".
        //   See http://logging.apache.org/log4j/2.x/manual/customconfig.html and
        //   https://stackoverflow.com/questions/15441477/how-to-add-log4j2-appenders-at-runtime-programmatically/33472893#33472893
        // Logger.setFileName(filename); //todo set Filename of Log-file
    }

    @Override
    public void onAttach(Argument arg) {

    }

    @Override
    public boolean consumeArgument() {
        return true;
    }
}
