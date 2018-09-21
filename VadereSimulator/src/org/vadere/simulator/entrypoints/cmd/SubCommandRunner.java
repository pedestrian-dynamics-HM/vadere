package org.vadere.simulator.entrypoints.cmd;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * run a sub command from the {@link VadereConsole}. All arguments will be accessible
 * throw the Namespace object
 */
public interface SubCommandRunner {

	void run(Namespace ns, ArgumentParser parser) throws Exception;

}
