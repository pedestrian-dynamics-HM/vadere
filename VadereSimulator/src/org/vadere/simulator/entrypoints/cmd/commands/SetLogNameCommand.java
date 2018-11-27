package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.log4j.*;

import java.util.Map;

public class SetLogNameCommand implements ArgumentAction {
    @Override
    public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
        String filename = (String) value;

        RollingFileAppender appender = new RollingFileAppender();
        appender.setName(filename);
        appender.setFile(filename);
        appender.setAppend(false);
        appender.setMaxFileSize("10000KB");
        appender.setLayout(new PatternLayout("%d{ABSOLUTE} %5p [%t] %c{1}:%L - %m%n"));
        appender.activateOptions();

        Logger.getRootLogger().addAppender(appender);
    }

    @Override
    public void onAttach(Argument arg) {

    }

    @Override
    public boolean consumeArgument() {
        return true;
    }
}
