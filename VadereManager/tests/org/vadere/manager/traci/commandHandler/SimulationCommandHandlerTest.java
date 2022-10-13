package org.vadere.manager.traci.commandHandler;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.TestRemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.commands.get.TraCIGetCompoundPayload;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.CompoundObjectBuilder;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.get.TraCIGetCacheHashCommand;
import org.vadere.state.traci.TraCIException;
import org.vadere.util.io.IOUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SimulationCommandHandlerTest extends CommandHandlerTest {

	private SimulationCommandHandler simCmdHandler = SimulationCommandHandler.instance;


	// Get


	@Test
	public void process_getCacheHash() throws IOException {
		SimulationVar var = SimulationVar.CACHE_HASH;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		String retValWin = "6a81c4357c1639d13c00609915125f8b1ba76518";
		String retValLin = "ac5468570f9165d503dd913aef66ab8f7fa06b47";

		// response ok //
		String basePath = "testResources/testProject001/scenarios";
		String scenario = "scenario002.scenario";
		String data;
		try {
			data = IOUtils.readTextFile(Paths.get(basePath, scenario).toString());
		} catch (IOException e) {
			throw e;
		}

		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCacheHashCommand.build(
				TraCICmd.GET_SIMULATION_VALUE, elementID, varID, varType, data));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				return;
			}
		};
		TraCICommand ret = simCmdHandler.process_getCacheHash(cmd, rm);
		checkGET_OK(ret);
		TraCIGetCacheHashCommand getCacheHashRet = (TraCIGetCacheHashCommand) ret;
		checkElementIdentifier(getCacheHashRet, elementID);
		// todo the falling distinctive is a workaround
		// Mockito does, unfortunately, not support mocking static methods.
		// Also: The scenario is deserialized and serialised internally, setting the lineseperators platform dependent
		// "\r\n" for windows and "\n" for linux -> the hash values are different
		if (System.getProperty("os.name").contains("Windows")) {
			checkReturnValue(getCacheHashRet, retValWin);
		} else {
			checkReturnValue(getCacheHashRet, retValLin);
		}
		checkVariableIdentifier(getCacheHashRet, SimulationVar.CACHE_HASH.id);

		// response err //
		String data2 = "jsonInvalid";
		TraCIGetCommand cmd2 = (TraCIGetCommand) getFirstCommand(TraCIGetCacheHashCommand.build(
				TraCICmd.GET_SIMULATION_VALUE, elementID, varID, varType, data2));
		TraCICommand ret2 = simCmdHandler.process_getCacheHash(cmd2, rm);
		checkGET_Err(ret2);
	}

	@Test
	public void process_getSimTime() {
		SimulationVar var = SimulationVar.TIME;
		int varID = var.id;
		TraCIDataType varType = var.type;
		String elementID = "-1";
		double retVal = 42.4;
		TraCIGetCommand cmd = (TraCIGetCommand) getFirstCommand(TraCIGetCommand.build(
				TraCICmd.GET_SIMULATION_VALUE, varID, elementID));
		RemoteManager rm = new TestRemoteManager() {
			@Override
			protected void mockIt() {
				when(simState.getSimTimeInSec()).thenReturn(retVal);
			}
		};
		TraCICommand ret = simCmdHandler.process_getSimTime(cmd, rm);
		checkGET_OK(ret);
		testGetValue(ret, varID, varType, elementID, retVal);
		
	}


	@Test
	public void process_apply_control() {

			String basePath = "testResources";
			String scenario = "stimulusInfoData2.json";
			String json = null;
			try {
				json = IOUtils.readTextFile(Paths.get(basePath, scenario).toString());
			} catch (IOException e) {
				fail();
			}

			SimulationVar var = SimulationVar.EXTERNAL_INPUT;
			int varID = var.id;
			TraCIDataType varType = var.type;
			String elementID = "-1";

			// set up control command
			CompoundObject data = CompoundObjectBuilder.builder()
					.add(TraCIDataType.STRING) // sending node
					.add(TraCIDataType.STRING) // model name
					.add(TraCIDataType.STRING) // command
					.build("10", "RouteChoice" , json);

			TraCISetCommand cmd = (TraCISetCommand) getFirstCommand(TraCISetCommand.build(
					TraCICmd.SET_SIMULATION_STATE, elementID, varID, varType, data));

			Pedestrian p = new Pedestrian(new AttributesAgent(10), new Random(1));
			RemoteManager rm = new TestRemoteManager() {
				@Override
				protected void mockIt() {
					Topography topo = mock(Topography.class, Mockito.RETURNS_DEEP_STUBS);
					Pedestrian pedEl1 = new Pedestrian(new AttributesAgent(), new Random());
					LinkedList<Integer> t = new LinkedList<>();
					t.add(1);
					pedEl1.setTargets(t);
					when(topo.getPedestrianDynamicElements().getElement(0)).thenReturn(pedEl1);
					when(simState.getSimTimeInSec()).thenReturn(4.0);

					StimulusInfoStore infoStore = new StimulusInfoStore();
					when(remoteManager.getRemoteSimulationRun().getStimulusController().getScenarioStore().getStimulusInfoStore()).thenReturn(infoStore);
				}
			};

			try {
				TraCICommand ret = simCmdHandler.process_apply_control(cmd, rm);
			} catch (Exception e) {
				e.printStackTrace();
			}


	}
}