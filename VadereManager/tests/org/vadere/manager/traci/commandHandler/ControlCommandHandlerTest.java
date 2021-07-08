package org.vadere.manager.traci.commandHandler;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.CmdType;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIVersion;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.control.TraCICloseCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.server.VadereServer;
import org.vadere.manager.traci.commands.control.TraCISendFileCommand;
import org.vadere.manager.traci.commands.control.TraCISimStepCommand;
import org.vadere.manager.traci.response.TraCIGetVersionResponse;
import org.vadere.manager.traci.response.TraCIResponse;
import org.vadere.manager.traci.response.TraCISimTimeResponse;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.simulator.control.simulation.SimThreadState;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControlCommandHandlerTest extends CommandHandlerTest {

	private ControlCommandHandler ctrCmdHandler = ControlCommandHandler.instance;

	private void process_close_(SimThreadState simThreadState) {

		TraCICmd traciCmd = TraCICmd.CLOSE;
		CmdType cmdType = traciCmd.type;

		TraCICloseCommand rawCmd = (TraCICloseCommand) getFirstCommand(TraCICloseCommand.build());
		RemoteManager rm = mock(RemoteManager.class, Mockito.RETURNS_DEEP_STUBS);
		doNothing().when(rm).setClientCloseCommandReceived(true);
		when(rm.getCurrentSimThreadState()).thenReturn(simThreadState);
		TraCICommand cmd = ctrCmdHandler.process_close(rawCmd, rm);

		testTraCICommand(cmd, traciCmd, cmdType);

		assertThat(cmd, instanceOf(TraCICloseCommand.class));
		TraCICloseCommand closeCommand = (TraCICloseCommand) cmd;
		assertThat(closeCommand.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
		TraCIResponse res = closeCommand.getResponse();
	}

	@Test
	public void process_close(){
		process_close_(SimThreadState.INIT);
	}

	@Test
	public void process_close_mainLoop(){
		process_close_(SimThreadState.MAIN_LOOP);
	}

	@Test
	public void process_close_postLoop(){
		process_close_(SimThreadState.POST_LOOP);
	}



	@Test
	public void process_simStep() {

		TraCICmd traciCmd = TraCICmd.SIM_STEP;
		CmdType cmdType = traciCmd.type;

		double targetTime = 5.0;

		TraCISimStepCommand rawCmd = (TraCISimStepCommand) getFirstCommand(TraCISimStepCommand.build(targetTime));
		RemoteManager rm = mock(RemoteManager.class, Mockito.RETURNS_DEEP_STUBS);
		when(rm.nextStep(targetTime)).thenReturn(true);
		when(rm.getSimulationStoppedEarlyAtTime()).thenReturn(Double.MAX_VALUE);
		TraCICommand cmd = ctrCmdHandler.process_simStep(rawCmd, rm);

		testTraCICommand(cmd, traciCmd, cmdType);

		assertThat(cmd, instanceOf(TraCISimStepCommand.class));
		TraCISimStepCommand simStepCommand = (TraCISimStepCommand) cmd;
		assertThat(simStepCommand.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
		TraCISimTimeResponse res = simStepCommand.getResponse();
		assertThat(simStepCommand.getTargetTime(), equalTo(targetTime));
	}

	@Test
	public void process_getVersion() {

		TraCICmd traciCmd = TraCICmd.GET_VERSION;
		CmdType cmdType = traciCmd.type;

		TraCIVersion version = VadereServer.currentVersion;
		String versionString = version.getVersionString();
		int versionId = version.traciBaseVersion;

		TraCIGetVersionCommand rawCmd = (TraCIGetVersionCommand) getFirstCommand(TraCIGetVersionCommand.build());
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {

			}
		};
		TraCICommand cmd = ctrCmdHandler.process_getVersion(rawCmd, rm);

		testTraCICommand(cmd, traciCmd, cmdType);

		checkGETVersion_OK(cmd);

		TraCIGetVersionCommand getVersionCommand = (TraCIGetVersionCommand) cmd;
		TraCIGetVersionResponse res = getVersionCommand.getResponse();
		assertThat(res.getVersionId(), equalTo(versionId));
		assertThat(res.getVersionString(), equalTo(versionString));
	}

	@Test
	public void process_load_file() {

		TraCICmd traciCmd = TraCICmd.SEND_FILE;
		CmdType cmdType = traciCmd.type;

		String fileName = "fileName";
		String file = "file";

		TraCISendFileCommand rawCmd = (TraCISendFileCommand) getFirstCommand(
				TraCISendFileCommand.TraCISendFileCommand(fileName, file));
		RemoteManager rm = mock(RemoteManager.class, Mockito.RETURNS_DEEP_STUBS);
		doNothing().when(rm).loadScenario(rawCmd.getFile(), new HashMap<>());
		doNothing().when(rm).startSimulation();
		TraCICommand cmd = ctrCmdHandler.process_load_file(rawCmd, rm);

		testTraCICommand(cmd, traciCmd, cmdType);

		assertThat(cmd, instanceOf(TraCISendFileCommand.class));
		TraCISendFileCommand sendFileCommand = (TraCISendFileCommand) cmd;
		assertThat(sendFileCommand.getFile(), equalTo(file));
	}
}