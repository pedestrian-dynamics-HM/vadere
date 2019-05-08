package org.vadere.simulator.entrypoints.cmd.commands;

import org.junit.Test;
import org.vadere.simulator.entrypoints.cmd.VadereConsole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class SetLogNameCommandTest {

    @Test
    public void testSetLogNameCommand(){
        String path = "test.log";
        VadereConsole.main(new String[] {"--logname", path, "scenario-run", "-f", "../VadereModelTests/TestOSM/scenarios/basic_2_density_discrete_ca.scenario"});
        try {
            assertTrue(Files.lines(Paths.get(path)).count() > 0);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}
