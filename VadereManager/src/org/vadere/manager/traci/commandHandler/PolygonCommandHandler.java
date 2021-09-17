package org.vadere.manager.traci.commandHandler;

import org.jfree.data.json.impl.JSONObject;
import org.vadere.annotation.traci.client.TraCIApi;
import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.state.traci.TraCICommandCreationException;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.annotation.PolygonHandler;
import org.vadere.manager.traci.commandHandler.annotation.PolygonHandlers;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.get.TraCIGetDistanceCommand;
import org.vadere.manager.traci.response.TraCIGetResponse;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.types.ScenarioElementType;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handel GET/SET/SUB {@link org.vadere.manager.traci.commands.TraCICommand}s for the Polygon API In
 * vadere, polygons are obstacles.
 */
@TraCIApi(
		name = "PolygonAPI",
		nameShort = "poly",
		singleAnnotation = PolygonHandler.class,
		multipleAnnotation = PolygonHandlers.class,
		cmdEnum = TraCICmd.class,
		varEnum = PolygonVar.class,
		var = "V_POLYGON",
		cmdGet = 0xa8,
		cmdSet = 0xc8,
		cmdSub = 0xd8,
		cmdResponseSub = 0xe8,
		cmdCtx = 0x88,
		cmdResponseCtx = 0x98
)

public class PolygonCommandHandler extends CommandHandler<PolygonVar> {

	public static PolygonCommandHandler instance;
	private static Logger logger = Logger.getLogger(PersonCommandHandler.class);

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
		for (PolygonHandler a : ans) {
			putHandler(a.cmd(), a.var(), m);
		}
	}


	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData) {
		return responseOK(responseDataType, responseData, TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_GET_POLYGON);
	}

	public TraCIGetResponse responseERR(String err) {
		return responseERR(err, TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_GET_POLYGON);
	}

	public boolean checkIfScenarioElementExists(ScenarioElement se, TraCIGetCommand cmd) {
		if (se == null) {
			cmd.setResponse(responseERR(CommandHandler.ELEMENT_ID_NOT_FOUND));
			logger.debugf("Polygon: %s not found.", cmd.getElementIdentifier());
			return false;
		}
		return true;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.TOPOGRAPHY_BOUNDS, name = "getTopographyBounds", ignoreElementId = true)
	public TraCICommand process_getTopographyBounds(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		remoteManager.accessState((manager, state) -> {
			Rectangle2D.Double bounds = state.getTopography().getBounds();
			List<Double> dRet = List.of(bounds.getX(), bounds.getY(), bounds.getHeight(), bounds.getWidth());
			List<String> ret = dRet.stream().map(p -> Double.toString(p)).collect(Collectors.toList());
			cmd.setResponse(responseOK(traCIVar.type, ret));
		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.ID_LIST, name = "getIDList", ignoreElementId = true)
	public TraCICommand process_getIDList(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {

		remoteManager.accessState((manager, state) -> {
			List<String> ret = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.map(p -> Integer.toString(p.getId()))
					.collect(Collectors.toList());
			cmd.setResponse(responseOK(traCIVar.type, ret));
		});

		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.COUNT, name = "getIDCount", ignoreElementId = true)
	public TraCICommand process_getIDCount(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {

		remoteManager.accessState((manager, state) -> {
			int ret = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.collect(Collectors.toList())
					.size();
			cmd.setResponse(responseOK(traCIVar.type, ret));
		});

		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.TYPE, name = "getType")
	public TraCICommand process_getType(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		int elementID = Integer.parseInt(cmd.getElementIdentifier());
		remoteManager.accessState((manager, state) -> {
			ScenarioElement se = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.filter(p -> p.getId() == elementID)
					.findFirst().orElse(null);
			if (checkIfScenarioElementExists(se, cmd))
				cmd.setResponse(responseOK(traCIVar.type, se.getType().toString()));
		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.SHAPE, name = "getShape")
	public TraCICommand process_getShape(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		int elementID = Integer.parseInt(cmd.getElementIdentifier());
		remoteManager.accessState((manager, state) -> {
			ScenarioElement se = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.filter(p -> p.getId() == elementID)
					.findFirst().orElse(null);
			if (checkIfScenarioElementExists(se, cmd))
				cmd.setResponse(responseOK(traCIVar.type, se.getShape().getPath()));
		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.CENTROID, name = "getCentroid")
	public TraCICommand process_getCentroid(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		int elementID = Integer.parseInt(cmd.getElementIdentifier());
		remoteManager.accessState((manager, state) -> {
			ScenarioElement se = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.filter(p -> p.getId() == elementID)
					.findFirst().orElse(null);
			TraCIGetResponse res;
			if (checkIfScenarioElementExists(se, cmd))
				cmd.setResponse(responseOK(traCIVar.type, se.getShape().getCentroid()));
		});
		return cmd;
	}




	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.DISTANCE, name = "getDistance", dataTypeStr = "ArrayList<String>")
	public TraCICommand process_getDistance(TraCIGetCommand rawCmd, RemoteManager remoteManager) {
		TraCIGetDistanceCommand cmd = TraCIGetDistanceCommand.create(rawCmd);
		int elementID = Integer.parseInt(cmd.getElementIdentifier());
		String x = cmd.getPoint().get(0);
		String y = cmd.getPoint().get(1);
		VPoint point = new VPoint(Double.parseDouble(x), Double.parseDouble(y));
		remoteManager.accessState((manager, state) -> {
			ScenarioElement se = state.getTopography().getAllScenarioElements()
					.stream()
					.filter(p -> p.getType() != ScenarioElementType.PEDESTRIAN)
					.filter(p -> p.getId() == elementID)
					.findFirst().orElse(null);
			TraCIGetResponse res;
			if (checkIfScenarioElementExists(se, cmd))
				cmd.setResponse(responseOK(PolygonVar.DISTANCE.type,
						new ArrayList<>(List.of(Double.toString(se.getShape().distance(point))))));
		});
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.COLOR, name = "getColor")
	public TraCICommand process_getColor(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseOK(traCIVar.type, Color.BLACK));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.POSITION, name = "getPosition2D")
	public TraCICommand process_getPosition2D(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.IMAGEFILE, name = "getImageFile")
	public TraCICommand process_getImageFile(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.WIDTH, name = "getImageWidth")
	public TraCICommand process_getImageWidth(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.HEIGHT, name = "getImageHeight")
	public TraCICommand process_getImageHeight(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}

	@PolygonHandler(cmd = TraCICmd.GET_POLYGON, var = PolygonVar.ANGLE, name = "getImageAngle")
	public TraCICommand process_getImageAngle(TraCIGetCommand cmd, RemoteManager remoteManager, PolygonVar traCIVar) {
		cmd.setResponse(responseERR("Not Implemented"));
		return cmd;
	}


	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager) {
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_POLYGON, TraCICmd.RESPONSE_SUB_POLYGON_VALUE);
	}

	public TraCICommand processGet(TraCICommand cmd, RemoteManager remoteManager) {
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;

		PolygonVar var = PolygonVar.fromId(getCmd.getVariableIdentifier());

		switch (var) {
			case TOPOGRAPHY_BOUNDS:
				return process_getTopographyBounds(getCmd, remoteManager, var);
			case ID_LIST:
				return process_getIDList(getCmd, remoteManager, var);
			case COUNT:
				return process_getIDCount(getCmd, remoteManager, var);
			case TYPE:
				return process_getType(getCmd, remoteManager, var);
			case SHAPE:
				return process_getShape(getCmd, remoteManager, var);
			case CENTROID:
				return process_getCentroid(getCmd, remoteManager, var);
			case COLOR:
				return process_getColor(getCmd, remoteManager, var);
			case POSITION:
				return process_getPosition2D(getCmd, remoteManager, var);
			case DISTANCE:
				return process_getDistance(getCmd, remoteManager);
			case IMAGEFILE:
			case WIDTH:
			case HEIGHT:
			case ANGLE:
			default:
				return process_UnknownCommand(getCmd, remoteManager);
		}
	}
}
