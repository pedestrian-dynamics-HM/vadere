package org.vadere.manager.traci.commandHandler;

import org.vadere.annotation.traci.client.TraCIApi;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.annotation.PolygonHandler;
import org.vadere.manager.traci.commandHandler.annotation.PolygonHandlers;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.state.scenario.Obstacle;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handel GET/SET/SUB {@link org.vadere.manager.traci.commands.TraCICommand}s for the Polygon API
 */
@TraCIApi(
		name = "PolygonAPI",
		nameShort = "poly",
		singleAnnotation = PolygonHandler.class,
		multipleAnnotation = PolygonHandlers.class,
		cmdEnum = TraCICmd.class,
		varEnum = PolygonVar.class
)

public class PolygonCommandHandler  extends CommandHandler<PolygonVar>{

	public static PolygonCommandHandler instance;


	static {
		instance = new PolygonCommandHandler();
	}

	public PolygonCommandHandler() {
		super();
		init(PolygonHandler.class, PolygonHandlers.class);
	}

	@Override
	protected void init_HandlerSingle(Method m) {
		PolygonHandler an = m.getAnnotation(PolygonHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		PolygonHandler[] ans = m.getAnnotation(PolygonHandlers.class).value();
		for(PolygonHandler a : ans){
			putHandler(a.cmd(), a.var(), m);
		}
	}


	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData){
		return  responseOK(responseDataType, responseData, TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_GET_POLYGON);
	}

	public TraCIGetResponse responseERR(String err){
		return responseERR(err, TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_GET_POLYGON);
	}

	public boolean checkIfObstacleExists(Obstacle obstacle, TraCIGetCommand cmd){
		if (obstacle == null) {
			cmd.setResponse(responseERR(CommandHandler.ELEMENT_ID_NOT_FOUND));
			return false;
		}
		return true;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.ID_LIST, name = "getIDList", ignoreElementId = true)
	public TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			List<String> ret = state.getTopography().getObstacles()
					.stream()
					.map(o -> Integer.toString(o.getId()))
					.collect(Collectors.toList());
			cmd.setResponse(responseOK(traCIVar.returnType, ret));
		});

		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.COUNT, name = "getIDCount", ignoreElementId = true)
	public TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			int ret = state.getTopography().getObstacles().size();
			cmd.setResponse(responseOK(traCIVar.returnType, ret));
		});

		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.TYPE, name = "getType")
	public TraCICommand process_getType(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseOK(traCIVar.returnType, "building"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.SHAPE, name = "getShape")
	public TraCICommand process_getShape(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Optional<Obstacle> obstacle = state.getTopography().getObstacles().stream()
					.filter(o-> cmd.getElementIdentifier().equals(Integer.toString(o.getId())))
					.findFirst();
			if (checkIfObstacleExists(obstacle.get(), cmd))
				cmd.setResponse(responseOK(traCIVar.returnType, obstacle.get().getShape().getPath()));

		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.COLOR, name = "getColor")
	public TraCICommand process_getColor(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseOK(traCIVar.returnType, Color.BLACK));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.POS_2D, name = "getPosition2D")
	public TraCICommand process_getPosition2D(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		remoteManager.accessState((manager, state) -> {
			Optional<Obstacle> obstacle = state.getTopography().getObstacles().stream()
					.filter(o-> cmd.getElementIdentifier().equals(Integer.toString(o.getId())))
					.findFirst();
			if (checkIfObstacleExists(obstacle.get(), cmd))
				cmd.setResponse(responseOK(traCIVar.returnType, obstacle.get().getShape().getCentroid()));

		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.IMAGE_FILE, name = "getImageFile")
	public TraCICommand process_getImageFile(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.WIDTH, name = "getImageWidth")
	public TraCICommand process_getImageWidth(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.HEIGHT, name = "getImageHeight")
	public TraCICommand process_getImageHeight(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.ANGLE, name = "getImageAngle")
	public TraCICommand process_getImageAngle(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar){
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager){
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_SUB_POLYGON_VALUE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		PolygonVar var = PolygonVar.fromId(getCmd.getVariableIdentifier());

		switch (var){
			case ID_LIST:
				return process_getIDList(getCmd,remoteManager, var);
			case COUNT:
				return process_getIDCount(getCmd,remoteManager, var);
			case TYPE:
				return process_getType(getCmd,remoteManager, var);
			case SHAPE:
				return process_getShape(getCmd,remoteManager, var);
			case COLOR:
				return process_getColor(getCmd,remoteManager, var);
			case POS_2D:
				return process_getPosition2D(getCmd,remoteManager, var);
			case IMAGE_FILE:
			case WIDTH:
			case HEIGHT:
			case ANGLE:
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager) {
		// do nothing just say ok...
		((TraCISetCommand) cmd).setOK();
		return cmd;
	}

}
