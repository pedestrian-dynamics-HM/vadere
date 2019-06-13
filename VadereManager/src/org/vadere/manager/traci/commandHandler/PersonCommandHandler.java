package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.util.logging.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Handel GET/SET {@link org.vadere.manager.traci.commands.TraCICommand}s for the Person API
 */
public class PersonCommandHandler extends CommandHandler{

	private static Logger logger = Logger.getLogger(PersonCommandHandler.class);

	public static PersonCommandHandler instance;

//	private HashMap<TraCIPersonVar, Method> handler;

	static {
		instance = new PersonCommandHandler();
	}

	private PersonCommandHandler(){
//		handler = new HashMap<>();
//		init();
	}

//	private void init(){
//		for (Method m : this.getClass().getDeclaredMethods()){
//			if (m.isAnnotationPresent(PersonHandler.class)){
//				PersonHandler an = m.getAnnotation(PersonHandler.class);
//				handler.put(an.variable(), m);
//			}
//		}
//	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData){
		return  responseOK(responseDataType, responseData, TraCICmd.GET_PERSON_VALUE, TraCICmd.RESPONSE_GET_PERSON_VALUE);
	}

	public TraCIGetResponse responseERR(String err){
		return responseERR(err, TraCICmd.GET_PERSON_VALUE, TraCICmd.RESPONSE_GET_PERSON_VALUE);
	}

	public boolean checkIfPedestrianExists(Pedestrian ped, TraCIGetCommand cmd){
		if (ped == null) {
			cmd.setResponse(responseERR(CommandHandler.ELEMENT_ID_NOT_FOUND));
			logger.debugf("Pedestrian: %s not found.", cmd.getElementIdentifier());
			return false;
		}
		return true;
	}

	@PersonHandler(
		commandIdentifier = TraCICmd.GET_PERSON_VALUE,
		variable = TraCIPersonVar.ID_LIST,
		clientCommandName = "getIDList")
	private TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){
		// elementIdentifier ignored.
		remoteManager.accessState((manager, state) -> {
			List<String> data = state.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.map(p -> Integer.toString(p.getId()))
					.collect(Collectors.toList());
			TraCIGetResponse res = responseOK(traCIVar.returnType, data);
			cmd.setResponse(res);
			logger.debugf("time: %f ID's: %s", state.getSimTimeInSec(), Arrays.toString(data.toArray(String[]::new)));
		});

		return cmd;
	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.COUNT,
			clientCommandName = "getIDCount")
	private TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			int numPeds = state.getTopography().getPedestrianDynamicElements().getElements().size();
			cmd.setResponse(responseOK(traCIVar.returnType, numPeds));
		});

		return cmd;

	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.SPEED,
			clientCommandName = "getSpeed")
	private TraCICommand process_getSpeed(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(traCIVar.returnType, ped.getVelocity().getLength()));

		});

		return cmd;
	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.POS_2D,
			clientCommandName = "getPosition2D")
	private TraCICommand process_getPosition(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd)) {
				cmd.setResponse(responseOK(traCIVar.returnType, ped.getPosition()));
				logger.debugf("time: %f Pedestrian: %s Position: %s",
						state.getSimTimeInSec(),
						cmd.getElementIdentifier(),
						ped.getPosition().toString());
			}
		});

		return cmd;
	}


	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.LENGTH,
			clientCommandName = "getLength")
	private TraCICommand process_getLength(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(traCIVar.returnType, ped.getRadius()*2));
		});

		return cmd;
	}


	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.WIDTH,
			clientCommandName = "getWidth")
	private TraCICommand process_getWidth(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Pedestrian ped = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()));

			if (checkIfPedestrianExists(ped, cmd))
				cmd.setResponse(responseOK(traCIVar.returnType, ped.getRadius()*2));
		});

		return cmd;
	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.ROAD_ID,
			clientCommandName = "getRoadId"
	)
	private TraCICommand process_getRoadId(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar) {
		// return dummy value
		cmd.setResponse(responseOK(traCIVar.returnType, "road000"));
		return cmd;
	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.ANGLE,
			clientCommandName = "getAngle"
	)
	private TraCICommand process_getAngle(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar) {
		// return dummy value
		cmd.setResponse(responseOK(traCIVar.returnType, 0.0));
		return cmd;
	}

	@PersonHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			variable = TraCIPersonVar.TYPE,
			clientCommandName = "getType"
	)
	private TraCICommand process_getType(TraCIGetCommand cmd, RemoteManager remoteManager, TraCIPersonVar traCIVar) {
		// return dummy value
		cmd.setResponse(responseOK(traCIVar.returnType, "pedestrian"));
		return cmd;
	}


	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager){
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.SUB_PERSON_VARIABLE, TraCICmd.RESPONSE_SUB_PERSON_VARIABLE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		TraCIPersonVar var = TraCIPersonVar.fromId(getCmd.getVariableIdentifier());

//		Method m = handler.getOrDefault(var, null);
//		if(m == null){
//			return process_NotImplemented(getCmd, remoteManager);
//		} else {
//			try {
//				return (TraCIGetCommand) m.invoke(this, getCmd, remoteManager, var);
//			} catch (IllegalAccessException | InvocationTargetException e) {
//				e.printStackTrace();
//			}
//			return process_UnknownCommand(getCmd, remoteManager);
//		}

		switch (var){
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager, var);
			case COUNT:
				return process_getIDCount(getCmd, remoteManager, var);
			case SPEED:
				return process_getSpeed(getCmd, remoteManager, var);
			case POS_2D:
				return process_getPosition(getCmd, remoteManager, var);
			case LENGTH:
				return process_getLength(getCmd, remoteManager, var);
			case WIDTH:
				return process_getWidth(getCmd, remoteManager, var);
			case WAITING_TIME:
			case POS_3D:
			case ANGLE:
				return process_getAngle(getCmd, remoteManager, var);
			case ROAD_ID:
				return process_getRoadId(getCmd, remoteManager, var);
			case TYPE:
				return process_getType(getCmd, remoteManager, var);
			case COLOR:
			case EDGE_POS:
			case MIN_GAP:
			case NEXT_EDGE:
			case REMAINING_STAGES:
			case VEHICLE:
				return process_NotImplemented(getCmd, remoteManager);
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager){
		return process_NotImplemented(cmd, remoteManager);
	}


}
