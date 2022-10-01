package org.vadere.manager.traci.commandHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections.ListUtils;
import org.vadere.annotation.traci.client.TraCIApi;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.annotation.VadereHandler;
import org.vadere.manager.traci.commandHandler.annotation.VadereHandlers;
import org.vadere.manager.traci.commandHandler.variables.VadereVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.psychology.perception.StimulusController;
import org.vadere.simulator.control.scenarioelements.TargetChangerController;
import org.vadere.state.attributes.scenario.AttributesTargetChanger;
import org.vadere.state.psychology.perception.json.StimulusInfo;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.state.util.StateJsonConverter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Handel GET/SET/SUB {@link org.vadere.manager.traci.commands.TraCICommand}s for the Vadere API
 */
@TraCIApi(
		name = "MiscAPI",
		nameShort = "va",
		singleAnnotation = VadereHandler.class,
		multipleAnnotation = VadereHandlers.class,
		cmdEnum = TraCICmd.class,
		varEnum = VadereVar.class,
		var = "V_MISC",
		cmdGet = 0xac,
		cmdSet = 0xcc,
		cmdSub = 0xdc,
		cmdResponseSub = 0xec,
		cmdCtx = 0x8c,
		cmdResponseCtx = 0x9c
)

public class VadereCommandHandler extends CommandHandler<VadereVar> {

	public static VadereCommandHandler instance;


	static {
		instance = new VadereCommandHandler();
	}

	public VadereCommandHandler() {
		super();
		init(VadereHandler.class, VadereHandlers.class);
	}

	@Override
	protected void init_HandlerSingle(Method m) {
		VadereHandler an = m.getAnnotation(VadereHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		VadereHandler[] ans = m.getAnnotation(VadereHandlers.class).value();
		for (VadereHandler a : ans) {
			putHandler(a.cmd(), a.var(), m);
		}
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData) {
		return responseOK(responseDataType, responseData, TraCICmd.GET_VADERE_VALUE, TraCICmd.RESPONSE_GET_VADERE_VALUE);
	}

	public TraCIGetResponse responseERR(String err) {
		return responseERR(err, TraCICmd.GET_VADERE_VALUE, TraCICmd.RESPONSE_GET_VADERE_VALUE);
	}

	@VadereHandler(cmd = TraCICmd.SET_VADERE_STATE, var = VadereVar.ADD_TARGET_CHANGER, dataTypeStr = "String", name = "createTargetChanger", ignoreElementId = true)
	public TraCICommand process_addTargetChanger(TraCISetCommand cmd, RemoteManager remoteManager) {
		String data = (String) cmd.getVariableValue();
		AttributesTargetChanger atc;
		try {
			atc = (AttributesTargetChanger) StateJsonConverter.deserializeScenarioElementType(data, ScenarioElementType.TARGET_CHANGER);
			remoteManager.accessState((manager, state) -> {
				//todo[TargetChanger] the creation of a TargetChanger with controller is complex and bad to read, can a pattern be applied?
				TargetChanger tc = new TargetChanger(atc);
				//todo[random]: use Random object from context for now. This should be replaced by the meta seed.
				VadereContext ctx = VadereContext.getCtx(state.getTopography());
				Random rnd = (Random) ctx.get("random");
				TargetChangerController tcc = new TargetChangerController(state.getTopography(), tc, rnd);
				manager.getRemoteSimulationRun().addTargetChangerController(tcc);
				state.getTopography().addTargetChanger(tc);
				cmd.setOK();
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return cmd;
	}

	@VadereHandler(cmd = TraCICmd.SET_VADERE_STATE, var = VadereVar.ADD_STIMULUS_INFOS, dataTypeStr = "String", name = "addStimulusInfos", ignoreElementId = true)
	public TraCICommand process_addStimulusInfos(TraCISetCommand cmd, RemoteManager remoteManager) {
		String data = (String) cmd.getVariableValue();
		StimulusInfoStore sis;
		try {
			sis = StateJsonConverter.deserializeStimuli(data);
		} catch (IOException e) {
			cmd.setErr("Error deserializing " + data);
			return cmd;
		}
		remoteManager.accessState((manager, state) -> {
			for (StimulusInfo si : sis.getStimulusInfos()) {
				manager.getRemoteSimulationRun().addStimulusInfo(si);
			}
			cmd.setOK();
		});
		return cmd;
	}

	@VadereHandler(cmd = TraCICmd.GET_VADERE_VALUE, var = VadereVar.GET_ALL_STIMULUS_INFOS, name = "getAllStimulusInfos", ignoreElementId = true)
	public TraCICommand process_getAllStimulusInfos(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			StimulusController sic = manager.getRemoteSimulationRun().getStimulusController();
			List<StimulusInfo> lsi = ListUtils.union(sic.getRecurringStimuli(), sic.getOneTimeStimuli());
			StimulusInfoStore sis = new StimulusInfoStore();
			sis.setStimulusInfos(lsi);


			TraCIGetResponse res;
			String data;
			try {
				data = StateJsonConverter.serializeStimuli(sis);
				res = responseOK(VadereVar.GET_ALL_STIMULUS_INFOS.type, data);
			} catch (JsonProcessingException e) {
				res = responseERR(CommandHandler.COULD_NOT_SERIALIZE_OBJECT + sis.toString());
			}
			cmd.setResponse(res);
		});
		return cmd;
	}

	@VadereHandler(cmd = TraCICmd.SET_VADERE_STATE, var = VadereVar.REMOVE_TARGET_CHANGER, name = "removeTargetChanger")
	public TraCICommand process_removeTargetChanger(TraCISetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			LinkedList<TargetChanger> lltc = (LinkedList<TargetChanger>) state.getTopography().getTargetChangers();
			if (lltc.remove(lltc
					.stream()
					.filter(p -> p.getId() == Integer.parseInt(cmd.getElementId()))
					.findFirst().orElse(null)))
				cmd.setOK();
			else
				cmd.setErr("ID not found.");
		});

		return cmd;
	}

	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager) {
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_VADERE_VALUE, TraCICmd.RESPONSE_SUB_VADERE_VALUE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		VadereVar var = VadereVar.fromId(getCmd.getVariableIdentifier());
		switch (var) {
			case GET_ALL_STIMULUS_INFOS:
				return process_getAllStimulusInfos(getCmd, remoteManager);
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCISetCommand setCmd = (TraCISetCommand) cmd;

		VadereVar var = VadereVar.fromId(setCmd.getVariableId());

		switch (var) {
			case ADD_TARGET_CHANGER:
				return process_addTargetChanger(setCmd, remoteManager);
			case REMOVE_TARGET_CHANGER:
				return process_removeTargetChanger(setCmd, remoteManager);
			case ADD_STIMULUS_INFOS:
				return process_addStimulusInfos(setCmd, remoteManager);
			default:
				return process_UnknownCommand(setCmd, remoteManager);

		}
	}

}
