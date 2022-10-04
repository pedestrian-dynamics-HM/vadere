package org.vadere.manager.traci.commandHandler;

import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.RemoteScenarioRun;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.VadereVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.scenario.Topography;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

public class VadereCommandHandlerTest extends CommandHandlerTest {

	private VadereCommandHandler vaCmdHandler = VadereCommandHandler.instance;

	// Get

	@Test
	public void process_getAllStimulusInfos() throws IOException {

		VadereVar var = VadereVar.GET_ALL_STIMULUS_INFOS;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String dataPath = "testResources/stimulusInfoData.json";
		String retVal;
		try {
			retVal = IOUtils.readTextFile(dataPath);
		} catch (IOException e) {
			throw e;
		}
		StimulusInfoStore sis = StateJsonConverter.deserializeStimuli(retVal);
		List<StimulusInfo> stimuli = sis.getStimulusInfos();
		List<StimulusInfo> oneTimeStimuli = StimulusController.filterOneTimeStimuli(stimuli);
		List<StimulusInfo> recurringStimuli = StimulusController.filterRecurringStimuli(stimuli);

		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_VADERE_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				StimulusController sc = mock(StimulusController.class, Mockito.RETURNS_DEEP_STUBS);
				when(sc.getOneTimeStimuli()).thenReturn(oneTimeStimuli);
				when(sc.getRecurringStimuli()).thenReturn(recurringStimuli);
				when(remoteManager.getRemoteSimulationRun().getStimulusController()).thenReturn(sc);
			}
		};
		TraCICommand ret = vaCmdHandler.process_getAllStimulusInfos(cmd, rm);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
	}


	// Set

	@Test
	public void process_addTargetChanger() {
		VadereVar var = VadereVar.ADD_TARGET_CHANGER;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String dataPath = "testResources/targetChangerData.json";
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch(IOException e) {
			e.printStackTrace();
		}
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_VADERE_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				String scenario = "scenario002";
				Topography topo = mock(Topography.class, Mockito.RETURNS_DEEP_STUBS);
				Random rnd = mock(Random.class, Mockito.RETURNS_DEEP_STUBS);
				when(rnd.nextInt()).thenReturn(42);
				when(topo.getContextId()).thenReturn(scenario);
				when(simState.getTopography()).thenReturn(topo);
				VadereContext ctx = VadereContext.getCtx(simState.getTopography());
				ctx.put("random", rnd);
				VadereContext.add(scenario, ctx);
			}
		};
		TraCICommand ret = vaCmdHandler.process_addTargetChanger(cmd, rm);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

	@Test
	public void process_removeTargetChanger() throws IOException{
		VadereVar var = VadereVar.REMOVE_TARGET_CHANGER;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "7";
		String dataPath = "testResources/targetChangerData.json";
		String data;
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch(IOException e) {
			throw e;
		}
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_VADERE_STATE, elementID, varID, varType, dataPath));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				TargetChanger tc = mock(TargetChanger.class, Mockito.RETURNS_DEEP_STUBS);
				when(tc.getId()).thenReturn(Integer.parseInt(elementID));
				LinkedList<TargetChanger> lltc = new LinkedList<>(List.of(tc));
				when(simState.getTopography().getTargetChangers()).thenReturn(lltc);
			}
		};
		TraCICommand ret = vaCmdHandler.process_removeTargetChanger(cmd, rm);
		checkSET_OK(ret);
		((TraCISetCommand)ret).getVariableValue();
		testSetValue(ret, varID, varType, elementID, null);
	}

	@Test
	public void process_addStimulusInfos() {
		VadereVar var = VadereVar.ADD_STIMULUS_INFOS;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String dataPath = "testResources/stimulusInfoData.json";
		String data = "";
		try {
			data = IOUtils.readTextFile(dataPath);
		} catch(IOException e) {
			e.printStackTrace();
		}
		TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
				TraCICmd.SET_VADERE_STATE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				RemoteScenarioRun rsr = mock(RemoteScenarioRun.class, Mockito.RETURNS_DEEP_STUBS);
				when(remoteManager.getRemoteSimulationRun()).thenReturn(rsr);
				doNothing().when(rsr).addStimulusInfo(isA(StimulusInfo.class));
			}
		};
		TraCICommand ret = vaCmdHandler.process_addStimulusInfos(cmd, rm);
		checkSET_OK(ret);
		testSetValue(ret, varID, varType, elementID, data);
	}

}