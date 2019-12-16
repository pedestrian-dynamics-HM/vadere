package org.vadere.manager.traci.commandHandler;

import org.junit.Test;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;

import static org.mockito.Mockito.when;

public class SimulationCommandHandlerTest extends CommandHandlerTest {

    private SimulationCommandHandler simCmdHandler = SimulationCommandHandler.instance;


    @Test
    public void process_getSimTime() {
        // 1a) create TraCIPacket based on the API call.
        // 1b) extract the command from the TraCIPacket.
        TraCIGetCommand cmd = (TraCIGetCommand)getFirstCommand(TraCIGetCommand.build(
                TraCICmd.GET_SIMULATION_VALUE,
                SimulationVar.CURR_SIM_TIME.id,
                "-1"));
        // 2) Create the TestRemoteManager and implement the needed mock of the Simulation state
        RemoteManager rm = new TestRemoteManager() {
            @Override
            protected void mockIt() {
                when(simState.getSimTimeInSec()).thenReturn(42.4);
            }
        };

        // 3) call the actual commandhandler method.
        TraCICommand ret = simCmdHandler.process_getSimTime(cmd, rm, SimulationVar.CURR_SIM_TIME);

        // 4) check if the return code is correct. See CommandHandlerTest parent class for generic tests of TraCICommand responses.
        checkGET_OK(ret); //

        // 5) check TraCIGetCommand specific fields
        TraCIGetCommand getRet = (TraCIGetCommand)ret;

        // 5a) ensure that the element id is the same compared to the client request.
        checkElementIdentifier(getRet, "-1");
        // 5b) check the expected return value (most important)
        checkReturnValue(getRet, 42.4);
        // 5c) ensure that the variable identifier is the same compared to the client request.
        checkVariableIdentifier(getRet, SimulationVar.CURR_SIM_TIME.id);
    }
}