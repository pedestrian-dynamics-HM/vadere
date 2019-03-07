package org.vadere.simulator.entrypoints.cmd.commands;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.vadere.util.logging.Logger;

import java.util.Map;

public class SetLogNameCommand implements ArgumentAction {
    @Override
    public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag, Object value) throws ArgumentParserException {
        String filename = (String) value;
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
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
        ctx.updateLoggers();
    }

    @Override
    public void onAttach(Argument arg) {

    }

    @Override
    public boolean consumeArgument() {
        return true;
    }
}
