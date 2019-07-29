package org.vadere.manager.client;

import java.io.IOException;

public interface ConsoleCommand {

	void execute(String[] args) throws IOException;

}
