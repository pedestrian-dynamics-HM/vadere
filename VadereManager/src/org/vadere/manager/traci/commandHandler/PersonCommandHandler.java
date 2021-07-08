package org.vadere.manager.traci.commandHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.vadere.annotation.traci.client.TraCIApi;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.annotation.PersonHandler;
import org.vadere.manager.traci.commandHandler.annotation.PersonHandlers;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.KnowledgeItem;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.traci.CompoundObject;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.Vector3D;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Handel GET/SET {@link org.vadere.manager.traci.commands.TraCICommand}s for the Person API
 */
@TraCIApi(
		name = "PersonAPI",
		nameShort = "pers",
		singleAnnotation = PersonHandler.class,
		multipleAnnotation = PersonHandlers.class,
		cmdEnum = TraCICmd.class,
		varEnum = PersonVar.class,
		var = "V_PERSON",
		cmdGet = 0xae,
		cmdSet = 0xce,
		cmdSub = 0xde,
		cmdResponseSub = 0xee,
		cmdCtx = 0x8e,
		cmdResponseCtx = 0x9e)
public class PersonCommandHandler extends CommandHandler<PersonVar> {

	public static PersonCommandHandler instance;
	private static Logger logger = Logger.getLogger(PersonCommandHandler.class);

	static {
		instance = new PersonCommandHandler();
	}

	private PersonCommandHandler() {
		super();
		init(PersonHandler.class, PersonHandlers.class);
	}

	@Override
	protected void init_HandlerSingle(Method m) {
		PersonHandler an = m.getAnnotation(PersonHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		PersonHandler[] ans = m.getAnnotation(PersonHandlers.class).value();
		for (PersonHandler a : ans) {
			putHandler(a.cmd(), a.var(), m);
		}
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData) {
		return responseOK(responseDataType, responseData, TraCICmd.GET_PERSON_VALUE, TraCICmd.RESPONSE_GET_PERSON_VALUE);
	}

	public TraCIGetResponse responseERR(String err) {
		return responseERR(err, TraCICmd.GET_PERSON_VALUE, TraCICmd.RESPONSE_GET_PERSON_VALUE);
	}

	public boolean checkIfPedestrianExists(Pedestrian ped, TraCIGetCommand cmd) {
		if (ped == null) {
			cmd.setResponse(responseERR(CommandHandler.ELEMENT_ID_NOT_FOUND));
			logger.debugf("Pedestrian: %s not found.", cmd.getElementIdentifier());
			return false;
		}
		return true;
	}

	public boolean checkIfPedestrianExists(Pedestrian ped, TraCISetCommand cmd) {
		if (ped == null) {
			cmd.setErr(CommandHandler.ELEMENT_ID_NOT_FOUND + cmd.getElementId());
			logger.debugf("Pedestrian: %s not found.", cmd.getElementId());
			return false;
		}
		return true;
	}

	public boolean checkIfIdIsFree(List<String> idList, TraCISetCommand cmd) {
		String id = cmd.getElementId();
		if (idList.contains(id)) {
			cmd.setErr(CommandHandler.ELEMENT_ID_NOT_FREE + id);
			logger.debugf("Pedestrian id: %s already in use.", id);
			return false;
		}
		return true;
	}

	public boolean checkIfMainModelIsPresent(SimulationState state, TraCISetCommand cmd) {
		if (!state.getMainModel().isPresent()) {
			cmd.setErr(CommandHandler.NO_MAIN_MODEL);
			logger.debugf("No main model present.");
			return false;
		}
		return true;
	}

	///////////////////////////// Handler /////////////////////////////

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.HAS_NEXT_TARGET, name = "getHasNextTarget")
	public TraCICommand process_getHasNextTarget(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			int id = Integer.parseInt(cmd.getElementIdentifier());
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements().getElement(id);
			if (checkIfPedestrianExists(ped, cmd)) {
				boolean data = ped.hasNextTarget();
				logger.debugf("Has next target: ", state.getSimTimeInSec(), Integer.toString(id), Boolean.toString(data));
				TraCIGetResponse res;
				if (data) {
					res = responseOK(PersonVar.HAS_NEXT_TARGET.type, 1);
				} else {
					res = responseOK(PersonVar.HAS_NEXT_TARGET.type, 0);
				}
				cmd.setResponse(res);
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.NEXT_TARGET_LIST_INDEX, name = "getNextTargetListIndex")
	public TraCICommand process_getNextTargetListIndex(TraCIGetCommand cmd, RemoteManager remoteManager) {
		int id = Integer.parseInt(cmd.getElementIdentifier());
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements().getElement(id);
			if (checkIfPedestrianExists(ped, cmd)) {
				int nextTargetListIndex = ped.getNextTargetListIndex();
				cmd.setResponse(responseOK(PersonVar.NEXT_TARGET_LIST_INDEX.type, nextTargetListIndex));
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.NEXT_TARGET_LIST_INDEX, name = "setNextTargetListIndex", dataTypeStr = "Integer")
	public TraCICommand process_setNextTargetListIndex(TraCISetCommand cmd, RemoteManager remoteManager) {
		int data = Integer.parseInt(cmd.getVariableValue().toString());
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementId()));
			if (checkIfPedestrianExists(ped, cmd)) {
				ped.setNextTargetListIndex(data);
				cmd.setOK();
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.ID_LIST, name = "getIdList", ignoreElementId = true)
	public TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			List<String> data = state.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.map(p -> Integer.toString(p.getId()))
					.collect(Collectors.toList());
			logger.debugf("%s.%s: t=%f pedIds(#%d)=%s",
					TraCICmd.GET_PERSON_VALUE.logShort(),
					PersonVar.ID_LIST.logShort(),
					state.getSimTimeInSec(),
					data.size(),
					Arrays.toString(data.toArray(String[]::new)));
			cmd.setResponse(responseOK(PersonVar.ID_LIST.type, data));
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.NEXT_ID, name = "getNextFreeId", ignoreElementId = true)
	public TraCICommand process_getNextFreeId(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			int nextFreeID = state.getTopography().getNextDynamicElementId();
			cmd.setResponse(responseOK(PersonVar.NEXT_ID.type, nextFreeID));
			logger.debugf("time: %f ID's: %s", state.getSimTimeInSec(), Integer.toString(nextFreeID));
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.COUNT, name = "getIdCount", ignoreElementId = true)
	public TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			int numPeds = state.getTopography().getPedestrianDynamicElements().getElements().size();
			cmd.setResponse(responseOK(PersonVar.COUNT.type, numPeds));
		});

		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.SPEED, name = "getFreeFlowSpeed")
	public TraCICommand process_getFreeFlowSpeed(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));
			if (checkIfPedestrianExists(ped, cmd)){
				double speed = ped.getFootstepHistory().getAverageSpeedInMeterPerSecond();
				cmd.setResponse(responseOK(PersonVar.SPEED.type, Double.isNaN(speed) ? 0.0 : speed));
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.SPEED, name = "setFreeFlowSpeed", dataTypeStr = "Double")
	public TraCICommand process_setFreeFlowSpeed(TraCISetCommand cmd, RemoteManager remoteManager) {
		String tmp = cmd.getVariableValue().toString();
		Double data = Double.parseDouble(tmp);
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementId()));
			if (checkIfPedestrianExists(ped, cmd)) {
				ped.setFreeFlowSpeed(data);
				cmd.setOK();
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.POSITION, name = "getPosition2D")
	public TraCICommand process_getPosition(TraCIGetCommand cmd, RemoteManager remoteManager) {

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd)) {
				cmd.setResponse(responseOK(PersonVar.POSITION.type, ped.getPosition()));
				logger.tracef("%s.%s: t=%f pedId=%s pos2d=%s",
						TraCICmd.GET_PERSON_VALUE.logShort(),
						PersonVar.POSITION.logShort(),
						state.getSimTimeInSec(),
						cmd.getElementIdentifier(),
						ped.getPosition().toString());
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.POSITION, name = "setPosition2D", dataTypeStr = "VPoint")
	public TraCICommand process_setPosition(TraCISetCommand cmd, RemoteManager remoteManager) {
		VPoint data = (VPoint) cmd.getVariableValue();
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementId()));
			if (checkIfPedestrianExists(ped, cmd)) {
				ped.setPosition(data);
				cmd.setOK();
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.POSITION3D, name = "getPosition3D")
	public TraCICommand process_getPosition3D(TraCIGetCommand cmd, RemoteManager remoteManager) {

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd)) {
				VPoint pos2d = ped.getPosition();
				cmd.setResponse(responseOK(PersonVar.POSITION3D.type, new Vector3D(pos2d.x, pos2d.y, 0.0)));
				logger.debugf("time: %f Pedestrian: %s Position: %s",
						state.getSimTimeInSec(),
						cmd.getElementIdentifier(),
						ped.getPosition().toString());
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.VELOCITY, name = "getVelocity")
	public TraCICommand process_getVelocity(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd)) {
				cmd.setResponse(responseOK(PersonVar.VELOCITY.type, ped.getVelocity()));
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.MAXSPEED, name = "getMaximumSpeed")
	public TraCICommand process_getMaximumSpeed(TraCIGetCommand cmd, RemoteManager rmeoteManager) {
		rmeoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));
			if (checkIfPedestrianExists(ped, cmd)) {
				cmd.setResponse(responseOK(PersonVar.MAXSPEED.type, ped.getAttributes().getMaximumSpeed()));
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.POSITION_LIST, name = "getPosition2DList", ignoreElementId = true)
	public TraCICommand process_getPosition2DList(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			Map<String, VPoint> data = state.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.map(p -> {
						String id = Integer.toString(p.getId());
						VPoint position = p.getPosition();
						return new HashMap.SimpleEntry<>(id, position);
					})
					.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
			TraCIGetResponse res = responseOK(PersonVar.POSITION_LIST.type, data);
			cmd.setResponse(res);
			logger.debugf("time: %f (ID,POSITION)s: %s", state.getSimTimeInSec(), data.toString());
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.LENGTH, name = "getLength")
	public TraCICommand process_getLength(TraCIGetCommand cmd, RemoteManager remoteManager) {

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(PersonVar.LENGTH.type, ped.getRadius() * 2));
		});
		return cmd;
	}


	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.WIDTH, name = "getWidth")
	public TraCICommand process_getWidth(TraCIGetCommand cmd, RemoteManager remoteManager) {

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(PersonVar.WIDTH.type, ped.getRadius() * 2));
		});

		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.ROAD_ID, name = "getRoadId")
	public TraCICommand process_getRoadId(TraCIGetCommand cmd, RemoteManager remoteManager) {
		// return dummy value
		cmd.setResponse(responseOK(PersonVar.ROAD_ID.type, "road000"));
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.ANGLE, name = "getAngle")
	public TraCICommand process_getAngle(TraCIGetCommand cmd, RemoteManager remoteManager) {
		// return dummy value
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd)){
				double angle = ped.getFootstepHistory().getNorthBoundHeadingAngleDeg();
				cmd.setResponse(responseOK(PersonVar.ANGLE.type, angle));
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.TYPE, name = "getType")
	public TraCICommand process_getType(TraCIGetCommand cmd, RemoteManager remoteManager) {
		// return dummy value
		cmd.setResponse(responseOK(PersonVar.TYPE.type, "pedestrian"));
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.TARGET_LIST, name = "getTargetList")
	public TraCICommand process_getTargetList(TraCIGetCommand cmd, RemoteManager remoteManager) {
		remoteManager.accessState((manager, state) -> {
			// return all targets the given element contains.
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));
			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(PersonVar.TARGET_LIST.type,
						ped.getTargets()
								.stream()
								.map(i -> Integer.toString(i))
								.collect(Collectors.toList())
				));
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.INFORMATION_ITEM, name = "setInformation")
	public TraCICommand process_setStimulus(TraCISetCommand cmd, RemoteManager remoteManager) {
		CompoundObject data = (CompoundObject) cmd.getVariableValue();
		double start_t = (double)data.getData(0, TraCIDataType.DOUBLE);
		double obsolete_at = (double)data.getData(1, TraCIDataType.DOUBLE);
		String information = (String)data.getData(2, TraCIDataType.STRING);

//		LinkedList<Integer> data = tmp.stream().map(Integer::parseInt).collect(Collectors.toCollection(LinkedList::new));
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementId()));
			if (checkIfPedestrianExists(ped, cmd)) {
				KnowledgeItem s = new KnowledgeItem(start_t, obsolete_at, information);
				ped.getKnowledgeBase().addInformation(s);
				ped.getKnowledgeBase().setInformationState(InformationState.INFORMATION_RECEIVED);
				cmd.setOK();
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.TARGET_LIST, name = "setTargetList", dataTypeStr = "ArrayList<String>")
	public TraCICommand process_setTargetList(TraCISetCommand cmd, RemoteManager remoteManager) {
		List<String> tmp = (List<String>) cmd.getVariableValue();
		LinkedList<Integer> data = tmp.stream().map(Integer::parseInt).collect(Collectors.toCollection(LinkedList::new));
		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementId()));
			if (checkIfPedestrianExists(ped, cmd)) {
				ped.setTargets(data);
				cmd.setOK();
			}
		});
		return cmd;
	}

	@PersonHandler(cmd = TraCICmd.SET_PERSON_STATE, var = PersonVar.ADD, ignoreElementId = true, name = "createNew", dataTypeStr = "String")
	public TraCICommand process_addPerson(TraCISetCommand cmd, RemoteManager remoteManager) {
		String data = (String) cmd.getVariableValue();
		remoteManager.accessState((manager, state) -> {
			Pedestrian generalPed;
			try {
				generalPed = StateJsonConverter.deserializePedestrian(data);
				List<String> idList = state.getTopography().getPedestrianDynamicElements()
						.getElements()
						.stream()
						.map(p -> Integer.toString(p.getId()))
						.collect(Collectors.toList());
				if (checkIfIdIsFree(idList, cmd)) {
					if (checkIfMainModelIsPresent(state, cmd)) {
						Pedestrian oldPed = state.getTopography().getPedestrianDynamicElements().getElement(Integer.parseInt(idList.get(0)));
						Pedestrian newDynamicElement = (Pedestrian) state.getMainModel().get().createElement(generalPed.getPosition(), generalPed.getId(), oldPed.getClass());
						newDynamicElement.setTargets(generalPed.getTargets());
						state.getTopography().getPedestrianDynamicElements().addElement(newDynamicElement);
						cmd.setOK();
					}
				}
			} catch (JsonParseException e1) {
				cmd.setErr(COULD_NOT_PARSE_OBJECT_FROM_JSON + data);
			} catch (JsonMappingException e2) {
				cmd.setErr(COULD_NOT_MAP_OBJECT_FROM_JSON + data);
			} catch (IOException e3) {
				cmd.setErr("IOException");
			}

		});

		return cmd;
	}


	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.WAITING_TIME, name = "getWaitingTime")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.COLOR, name = "getColor")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.EDGE_POS, name = "getEdgePos")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.MIN_GAP, name = "getMinGap")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.NEXT_EDGE, name = "getNextEdge")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.REMAINING_STAGES, name = "getRemainingStages")
	@PersonHandler(cmd = TraCICmd.GET_PERSON_VALUE, var = PersonVar.VEHICLE, name = "getVehicle")
	public TraCICommand process_NotImplemented(TraCIGetCommand cmd, RemoteManager remoteManager) {
		return super.process_NotImplemented(cmd, remoteManager);
	}

	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager) {
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_PERSON_VALUE, TraCICmd.RESPONSE_SUB_PERSON_VARIABLE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		PersonVar var = PersonVar.fromId(getCmd.getVariableIdentifier());
		Method m = getHandler(getCmd.getTraCICmd(), var);

		logger.tracef("invokeHandler: PersonCommandHandler.%s [CMD: %s VAR: %s]",
				m.getName(),
				cmd.getTraCICmd().logShort(),
				var.toString());
		return invokeHandler(m, this, getCmd, remoteManager);
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCISetCommand setCmd = (TraCISetCommand) cmd;

		PersonVar var = PersonVar.fromId(setCmd.getVariableId());
		Method m = getHandler(setCmd.getTraCICmd(), var);

		return invokeHandler(m, this, setCmd, remoteManager);
	}

}
