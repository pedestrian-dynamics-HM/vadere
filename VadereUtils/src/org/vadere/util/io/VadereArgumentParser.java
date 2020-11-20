package org.vadere.util.io;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.version.Version;

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
                .description("Run the Vadere pedestrian simulator.");

        addOptionsToParser(argumentParser);
    }

    // Getters
    public ArgumentParser getArgumentParser() {
        return argumentParser;
    }

    // Methods
    public Namespace parseArgsAndProcessInitialOptions(String[] args) throws ArgumentParserException {
        if (versionIsRequested(args)) {
            System.out.println(String.format("Vadere %s (Commit Hash: %s)", Version.releaseNumber(), Version.getVersionControlCommitHash()));
            System.exit(0);
        }

        Namespace namespace = argumentParser.parseArgs(args);

        String configFile = namespace.getString("configfile");
        if (configFile != null) {
            VadereConfig.setConfigPath(configFile);
        }

        return namespace;
    }

    protected boolean versionIsRequested(String[] args) {
        boolean versionRequrested = false;

        for (String currentArgument : args) {
            if (currentArgument.contains("--version")) {
                versionRequrested = true;
                break;
            }
        }

        return versionRequrested;
    }

    private void addOptionsToParser(ArgumentParser parser) {
        parser.addArgument("--version")
                .action(Arguments.storeTrue())
                .dest("version")
                .help("Print version information and exit Vadere.");

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
