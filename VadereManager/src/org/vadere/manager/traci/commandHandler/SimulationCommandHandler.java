package org.vadere.manager.traci.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.annotation.SimulationHandler;
import org.vadere.manager.traci.commandHandler.annotation.SimulationHandlers;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.compoundobjects.CompoundObject;
import org.vadere.manager.traci.compoundobjects.TargetChangerData;
import org.vadere.manager.traci.compoundobjects.WaitingAreaData;
import org.vadere.manager.traci.respons.TraCIGetResponse;
import org.vadere.state.attributes.scenario.AttributesMeasurementArea;
import org.vadere.state.attributes.scenario.AttributesTargetChanger;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.TargetChanger;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;

/**
 * Handel GET/SET/SUB {@link org.vadere.manager.traci.commands.TraCICommand}s for the Simulation API
 */
public class SimulationCommandHandler  extends CommandHandler<SimulationVar>{

	public static SimulationCommandHandler instance;

	static {
		instance = new SimulationCommandHandler();
	}

	private SimulationCommandHandler(){
		super();
		init(SimulationHandler.class, SimulationHandlers.class);
	}


	@Override
	protected void init_HandlerSingle(Method m) {
		SimulationHandler an = m.getAnnotation(SimulationHandler.class);
		putHandler(an.cmd(), an.var(), m);
	}

	@Override
	protected void init_HandlerMult(Method m) {
		SimulationHandler[] ans = m.getAnnotation(SimulationHandlers.class).value();
		for(SimulationHandler a : ans){
			putHandler(a.cmd(), a.var(), m);
		}
	}

	public TraCIGetResponse responseOK(TraCIDataType responseDataType, Object responseData){
		return responseOK(responseDataType, responseData, TraCICmd.GET_SIMULATION_VALUE, TraCICmd.RESPONSE_GET_SIMULATION_VALUE);
	}

	public TraCIGetResponse responseERR(TraCIDataType responseDataType, Object responseData){
		return responseOK(responseDataType, responseData, TraCICmd.GET_SIMULATION_VALUE, TraCICmd.RESPONSE_GET_SIMULATION_VALUE);
	}

	public TraCICommand process_getNetworkBound(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			Rectangle2D.Double  rec = state.getTopography().getBounds();

			VPoint lowLeft = new VPoint(rec.getMinX(), rec.getMinY());
			VPoint highRight = new VPoint(rec.getMaxX(), rec.getMaxX());
			ArrayList<VPoint> polyList = new ArrayList<>();
			polyList.add(lowLeft);
			polyList.add(highRight);
			cmd.setResponse(responseOK(traCIVar.type, polyList));
		});

		return cmd;
	}

	public TraCICommand process_getSimTime(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		remoteManager.accessState((manager, state) -> {
			// BigDecimal to ensure correct comparison in omentpp
			BigDecimal time = BigDecimal.valueOf(state.getSimTimeInSec());
			cmd.setResponse(responseOK(traCIVar.type, time.setScale(1, RoundingMode.HALF_UP).doubleValue()));
		});

		return cmd;
	}

	public  TraCICommand process_getVehiclesStartTeleportIDs(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.type, new ArrayList<>()));
		return cmd;
	}

	public  TraCICommand process_getVehiclesEndTeleportIDs(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.type, new ArrayList<>()));
		return cmd;
	}

	public  TraCICommand process_getVehiclesStartParkingIDs(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.type, new ArrayList<>()));
		return cmd;
	}

	public  TraCICommand process_getVehiclesStopParkingIDs(TraCIGetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){

		cmd.setResponse(responseOK(traCIVar.type, new ArrayList<>()));
		return cmd;
	}

	@SimulationHandler(cmd = TraCICmd.SET_SIMULATION_STATE, var = SimulationVar.ADD_TARGET_CHANGER, name = "createTargetChanger")
	public TraCICommand process_addTargetChanger(TraCISetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){
		TargetChangerData data = (TargetChangerData) cmd.getVariableValue();
		remoteManager.accessState((manager, state) -> {
			AttributesTargetChanger attr = new AttributesTargetChanger(
					data.getPointsAsVPolygon(),
					data.getIdAsInt(),
					data.getReachDist(),
					data.getNextTargetAsInt(),
					data.getProb()
			);
			TargetChanger tc = new TargetChanger(attr);
			state.getTopography().addTargetChanger(tc);
			cmd.setOK();
		});

		return cmd;
	}

	@SimulationHandler(cmd = TraCICmd.SET_SIMULATION_STATE, var = SimulationVar.ADD_WAITING_AREA, name = "createWaitingArea")
	public TraCICommand process_addWaitingArea(TraCISetCommand cmd, RemoteManager remoteManager, SimulationVar traCIVar){
		WaitingAreaData data = (WaitingAreaData) cmd.getVariableValue();
		remoteManager.accessState((manager, state) -> {
			AttributesMeasurementArea attr = new AttributesMeasurementArea(
					data.getIdAsInt(),
					data.getPointsAsVPolygon()
			);
			MeasurementArea ma = new MeasurementArea(attr);
			state.getTopography().getMeasurementAreas().add(ma);
			cmd.setOK();
		});

		return cmd;
	}

	public TraCICommand processValueSub(TraCICommand rawCmd, RemoteManager remoteManager){
		return processValueSub(rawCmd, remoteManager, this::processGet,
				TraCICmd.GET_SIMULATION_VALUE, TraCICmd.RESPONSE_SUB_SIMULATION_VALUE);
	}

	public TraCICommand processGet(TraCICommand rawCmd, RemoteManager remoteManager){

		TraCIGetCommand cmd = (TraCIGetCommand) rawCmd;
		SimulationVar var = SimulationVar.fromId(cmd.getVariableIdentifier());
		switch (var){
			case NETWORK_BOUNDING_BOX_2D:
				return process_getNetworkBound(cmd, remoteManager, var);
			case CURR_SIM_TIME:
				return process_getSimTime(cmd, remoteManager, var);
			case VEHICLES_START_TELEPORT_IDS:
				return process_getVehiclesStartTeleportIDs(cmd, remoteManager, var);
			case VEHICLES_END_TELEPORT_IDS:
				return process_getVehiclesEndTeleportIDs(cmd, remoteManager, var);
			case VEHICLES_START_PARKING_IDS:
				return process_getVehiclesStartParkingIDs(cmd, remoteManager, var);
			case VEHICLES_STOP_PARKING_IDS:
				return process_getVehiclesStopParkingIDs(cmd, remoteManager, var);

			default:
				return process_NotImplemented(cmd, remoteManager);
		}
	}

	public TraCICommand processSet(TraCICommand cmd, RemoteManager remoteManager) {
		return process_NotImplemented(cmd, remoteManager);

	}

}
