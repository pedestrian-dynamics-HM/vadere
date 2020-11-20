package org.vadere.manager;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.vadere.manager.server.VadereServer;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.VadereArgumentParser;
import org.vadere.util.version.Version;

public class VadereManagerArgumentParser extends VadereArgumentParser {

	@Override
	public Namespace parseArgsAndProcessInitialOptions(String[] args) throws ArgumentParserException {
		if (versionIsRequested(args)) {
			System.out.println(String.format("Vadere %s (Commit Hash: %s) [TraCI: %s]",
					Version.releaseNumber(),
					Version.getVersionControlCommitHash(),
					VadereServer.currentVersion.getVersionString()));
			System.exit(0);
		}

		Namespace namespace = argumentParser.parseArgs(args);

		String configFile = namespace.getString("configfile");
		if (configFile != null) {
			VadereConfig.setConfigPath(configFile);
		}

		return namespace;
	}
}
