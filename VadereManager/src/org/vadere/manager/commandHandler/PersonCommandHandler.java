package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.manager.stsc.commands.TraCIGetCommand;
import org.vadere.manager.stsc.respons.StatusResponse;
import org.vadere.manager.stsc.respons.TraCIGetResponse;
import org.vadere.manager.stsc.respons.TraCIStatusResponse;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.List;
import java.util.stream.Collectors;

public class PersonCommandHandler extends CommandHandler{


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
//			if (m.isAnnotationPresent(GetHandler.class)){
//				GetHandler an = m.getAnnotation(GetHandler.class);
//				handler.put(an.variable(), m);
//			}
//		}
//	}

	private TraCIGetResponse response(TraCIDataType responseDataType, Object responseData){
		TraCIGetResponse res = new TraCIGetResponse(
				new StatusResponse(TraCICmd.GET_PERSON_VALUE, TraCIStatusResponse.OK, ""),
				TraCICmd.RESPONSE_GET_PERSON_VALUE);
		res.setResponseDataType(responseDataType);
		res.setResponseData(responseData);
		return res;
	}

	@GetHandler(
		commandIdentifier = TraCICmd.GET_PERSON_VALUE,
		returnType = TraCIDataType.STRING_LIST,
		variable = TraCIPersonVar.ID_LIST,
		clientCommandName = "getIDList")
	protected TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager){
		// elementIdentifier ignored.
		remoteManager.accessState((manager, state) -> {
			List<String> data = state.getTopography().getPedestrianDynamicElements()
					.getElements()
					.stream()
					.map(p -> Integer.toString(p.getId()))
					.collect(Collectors.toList());
			TraCIGetResponse res = response(TraCIDataType.STRING_LIST, data);
			cmd.setResponse(res);
		});

		return cmd;
	}

	@GetHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			returnType = TraCIDataType.INTEGER,
			variable = TraCIPersonVar.COUNT,
			clientCommandName = "getIDCount")
	protected TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState((manager, state) -> {
			int numPeds = state.getTopography().getPedestrianDynamicElements().getElements().size();
			cmd.setResponse(response(TraCIDataType.INTEGER, numPeds));
		});

		return cmd;

	}

	@GetHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			returnType = TraCIDataType.DOUBLE,
			variable = TraCIPersonVar.SPEED,
			clientCommandName = "getSpeed")
	protected TraCICommand process_getSpeed(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState((manager, state) -> {
			double speed = state.getTopography()
					.getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()))
					.getVelocity().getLength();
			cmd.setResponse(response(TraCIDataType.DOUBLE, speed));
		});

		return cmd;
	}

	@GetHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			returnType = TraCIDataType.POS_2D,
			variable = TraCIPersonVar.POS_2D,
			clientCommandName = "getPosition2D")
	protected TraCICommand process_getPosition(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState((manager, state) -> {
			VPoint pos = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()))
					.getPosition();
			cmd.setResponse(response(TraCIDataType.POS_2D, pos));
		});

		return cmd;
	}


	@GetHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			returnType = TraCIDataType.DOUBLE,
			variable = TraCIPersonVar.LENGTH,
			clientCommandName = "getLength")
	protected TraCICommand process_getLength(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState((manager, state) -> {
			double pedLength = state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()))
					.getRadius() *2;
			cmd.setResponse(response(TraCIDataType.DOUBLE, pedLength));
		});

		return cmd;
	}


	@GetHandler(
			commandIdentifier = TraCICmd.GET_PERSON_VALUE,
			returnType = TraCIDataType.DOUBLE,
			variable = TraCIPersonVar.WIDTH,
			clientCommandName = "getWidth")
	protected TraCICommand process_getWidth(TraCIGetCommand cmd, RemoteManager remoteManager){

		remoteManager.accessState((manager, state) -> {
			double pedWidth= state.getTopography().getPedestrianDynamicElements()
					.getElement(Integer.parseInt(cmd.getElementIdentifier()))
					.getRadius() *2;
			cmd.setResponse(response(TraCIDataType.DOUBLE, pedWidth));
		});

		return cmd;
	}


	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager){
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		TraCIPersonVar var = TraCIPersonVar.fromId(getCmd.getVariableIdentifier());

//		Method m = handler.getOrDefault(var, null);
//		if(m == null){
//			return process_NotImplemented(getCmd, remoteManager);
//		} else {
//			try {
//				return (TraCIGetCommand) m.invoke(this, getCmd, remoteManager);
//			} catch (IllegalAccessException | InvocationTargetException e) {
//				e.printStackTrace();
//			}
//			return process_UnknownCommand(getCmd, remoteManager);
//		}

		switch (var){
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager);
			case COUNT:
				return process_getIDCount(getCmd, remoteManager);
			case SPEED:
				return process_getSpeed(getCmd, remoteManager);
			case POS_2D:
				return process_getPosition(getCmd, remoteManager);
			case LENGTH:
				return process_getLength(getCmd, remoteManager);
			case WIDTH:
				return process_getWidth(getCmd, remoteManager);
			case WAITING_TIME:
			case POS_3D:
			case ANGLE:
			case ROAD_ID:
			case TYPE:
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
