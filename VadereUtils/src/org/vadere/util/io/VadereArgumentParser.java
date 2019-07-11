package org.vadere.util.io;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.vadere.util.config.VadereConfig;

/**
 * This class provides the functionality to parse command line arguments
 * and should be used by all Vadere end-user applications, i.e.:
 *
 *   - VadereApplication
 *   - VadereConsole
 *   - PostVisualizationWindow
 */
public class VadereArgumentParser {

    // Variables
    public ArgumentParser argumentParser;

    // Constructors
    public VadereArgumentParser() {
        argumentParser = ArgumentParsers.newArgumentParser("Vadere")
                .defaultHelp(true)
                .description("Runs the Vadere pedestrian simulator.");

        addOptionsToParser(argumentParser);
    }

    // Getters
    public ArgumentParser getArgumentParser() {
        return argumentParser;
    }

    // Methods
    public Namespace parseArgsAndProcessOptions(String[] args) throws ArgumentParserException {
        Namespace namespace = argumentParser.parseArgs(args);

        String configFile = namespace.getString("configfile");

        if (configFile != null) {
            VadereConfig.setConfigPath(configFile);
        }

        return namespace;
    }

    private void addOptionsToParser(ArgumentParser parser) {
        parser.addArgument("--loglevel")
                .required(false)
                .type(String.class)
                .dest("loglevel")
                .choices("OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")
                .setDefault("INFO")
                .help("Set Log Level.");


        parser.addArgument("--logname")
                .required(false)
                .type(String.class)
                .dest("logname")
                .help("Write log to given file.");

        parser.addArgument("--config-file")
                .required(false)
                .type(String.class)
                .dest("configfile")
                .help("Use given config file instead of the default config file.");
    }

}
