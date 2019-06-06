package org.vadere.manager.commandHandler;

import org.vadere.manager.RemoteManager;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.commands.TraCICommand;
import org.vadere.util.logging.Logger;

import java.util.HashMap;

public class CommandExecutor {

	private static Logger logger = Logger.getLogger(CommandExecutor.class);

	private HashMap<Integer, TraCICmdHandler> cmdMap;
	// TODO: RemoteManager verwaltet die Simulation.
	// Sobald ein Simulationsschrit fertig ist, wird der RemoteManager über den
	// RemoteManagerListener informiert. Jetzt kann der org.vadere.simulator.control.SimulationState
	// ausgelesen werden.
	// vorghen:
	// 1. LOAD_Command: Laden der String Representation und erzeugen des Scenario Objekts
	// 2. erstellen von Subscriptions (später)
	// 3. SIM_STEP_Command:
	//    - wenn die Simulatoin noch nicht gestarter wurde starte simulation
	//    - erstes RemoteManagerListener kommt
	//    - Sende Antwort auf SIM_STEP_Command
	// 4. Warte auf GET/SET commands und beantworte dies über den RemoteManager
	// 5. nächste SIM_STEP_Command: Führe den nächsten Simulatoinsschrit aus
	//	      - remoteManager.currentSimulationRun.nextSimCommand(-1);
	// Soll der remoteManager mit in den TraCICmdHandler übergeben werden?
	//
	private RemoteManager remoteManager;

	public CommandExecutor() {
		remoteManager = new RemoteManager();
		cmdMap = new HashMap<>();
		cmdMap.put(TraCICmd.GET_VERSION.id, ControlCommandHandler.instance::process_getVersion);
		cmdMap.put(TraCICmd.LOAD.id, ControlCommandHandler.instance::process_load);
		cmdMap.put(TraCICmd.SIM_STEP.id, ControlCommandHandler.instance::process_simStep);
		cmdMap.put(TraCICmd.CLOSE.id, ControlCommandHandler.instance::process_close);
		cmdMap.put(TraCICmd.SEND_FILE.id, ControlCommandHandler.instance::process_load_file);
		cmdMap.put(TraCICmd.GET_PERSON_VALUE.id, PersonCommandHandler.instance::processGet);
		cmdMap.put(TraCICmd.SET_PERSON_STATE.id, PersonCommandHandler.instance::processSet);
	}

	public TraCIPacket execute(TraCICommand cmd){
		TraCICmdHandler handler = cmdMap.get(cmd.getTraCICmd().id);
		if (handler == null){
			logger.errorf("No CommandHandler found for command: %02X", cmd.getTraCICmd().id);
			return TraCIPacket.create().add_Err_StatusResponse(cmd.getTraCICmd().id, "ID not found.");
		}

		return handler.handel(cmd).buildResponsePacket();

	}
}
