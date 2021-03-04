package org.vadere.manager.traci.commands.get;

import org.vadere.state.traci.TraCICommandCreationException;
import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.variables.SimulationVar;
import org.vadere.manager.traci.commands.TraCIGetCommand;

public class TraCIGetCacheHashCommand extends TraCIGetCommand {

	private String file; // file content


	public TraCIGetCacheHashCommand(TraCICmd traCICmd, int variableIdentifier, String elementIdentifier) {
		super(traCICmd, variableIdentifier, elementIdentifier);
		this.file = "";
	}

	public TraCIGetCacheHashCommand(TraCIGetCommand getCmd) {
		super(getCmd.getTraCICmd(), getCmd.getVariableIdentifier(), getCmd.getElementIdentifier());
		getCmd.getCmdBuffer().ensureBytes(1);
		TraCIDataType dataType = TraCIDataType.fromId(getCmd.getCmdBuffer().readUnsignedByte());
		if (!dataType.equals(TraCIDataType.STRING)) {
			throw new TraCIException("expected String value in TraCIGetCacheHashCommand");
		}
		this.file = getCmd.getCmdBuffer().readString();
	}

	static public TraCIGetCacheHashCommand create(TraCIGetCommand getCmd) {
		if (getCmd.getTraCICmd().equals(TraCICmd.GET_SIMULATION_VALUE) &&
				getCmd.getVariableIdentifier() == SimulationVar.CACHE_HASH.id) {

			return new TraCIGetCacheHashCommand(getCmd);
		} else {
			throw new TraCICommandCreationException("cannot create TraCIGetCacheHashCommand from %s", getCmd.toString());
		}
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

}
