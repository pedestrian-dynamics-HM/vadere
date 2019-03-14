package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Map;

public class SetLogNameCommand implements ArgumentAction {
    @Override
    public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
        String filename = (String) value;
<<<<<<< HEAD
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final Configuration config = loggerContext.getConfiguration();
        final LoggerConfig logConfig = config.getLoggerConfig("STDOUTERR");
        final LoggerConfig rootConfig = config.getRootLogger();
        rootConfig.removeAppender("FILE");
        logConfig.removeAppender("FILE");
        RollingFileAppender oldAppender = (RollingFileAppender) config.getAppender("FILE");
        RollingFileAppender newAppender = RollingFileAppender.newBuilder()
                .withName("FILE")
                .withFileName(filename)
                .withFilePattern(filename + "-%d{MM-dd-yy-HH-mm-ss}-%i.log.gz")
                .withLayout(oldAppender.getLayout())
                .withPolicy(oldAppender.getTriggeringPolicy())
                .build();
        newAppender.start();
        config.addAppender(newAppender);
        logConfig.addAppender(newAppender, null, null);
        rootConfig.addAppender(newAppender, null, null);
        loggerContext.updateLoggers();
=======

        // FIXME: Create a "FileAppender" which writes to "filename".
        //   See http://logging.apache.org/log4j/2.x/manual/customconfig.html and
        //   https://stackoverflow.com/questions/15441477/how-to-add-log4j2-appenders-at-runtime-programmatically/33472893#33472893
        // Logger.setFileName(filename); //todo set Filename of Log-file
>>>>>>> d2890e8ca262b20aceab0d657c996308bd82cb17
    }

    @Override
    public void onAttach(Argument arg) {

    }

    @Override
    public boolean consumeArgument() {
        return true;
    }
}
