package org.vadere.simulator.entrypoints.cmd.commands;

import org.junit.Test;
import org.vadere.simulator.entrypoints.cmd.VadereConsole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SetLogNameCommandTest {

    @Test
    public void testSetLogNameCommand(){
        try {
	        String path = Paths.get(getClass().getResource("/org/vadere/simulator/entrypoints/test.scenario").toURI()).toString();
	        System.out.println(path);
	        VadereConsole.main(new String[] {"--logname", path, "scenario-run", "-f", path});
            assertTrue(Files.lines(Paths.get(path)).count() > 0);
        } catch (IOException | URISyntaxException e) {
            fail(e.getMessage());
        }
    }

}
