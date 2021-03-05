package org.vadere.manager.traci.commands.get;

import org.vadere.state.traci.TraCICommandCreationException;
import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;
import org.vadere.manager.traci.commands.TraCIGetCommand;

import java.util.ArrayList;

public class TraCIGetDistanceCommand extends TraCIGetCommand {

	private ArrayList<String> point;


	public TraCIGetDistanceCommand(TraCICmd traCICmd, int variableIdentifier, String elementIdentifier) {
		super(traCICmd, variableIdentifier, elementIdentifier);
	}

	public TraCIGetDistanceCommand(TraCIGetCommand getCmd) {
		super(getCmd.getTraCICmd(), getCmd.getVariableIdentifier(), getCmd.getElementIdentifier());
		getCmd.getCmdBuffer().ensureBytes(1);
		TraCIDataType dataType = TraCIDataType.fromId(getCmd.getCmdBuffer().readUnsignedByte());
		if (!dataType.equals(TraCIDataType.STRING_LIST)) {
			throw new TraCIException("expected ArrayList<String> in TraCIGetDistanceCommand");
		}
		this.point = (ArrayList<String>) getCmd.getCmdBuffer().readStringList();
		if (!(this.point.size() == 2)) {
			throw new TraCIException("expected ArrayList<String> of size 2 in TraCIGetDistanceCommand");
		}
	}

	static public TraCIGetDistanceCommand create(TraCIGetCommand getCmd) {
		if (getCmd.getTraCICmd().equals(TraCICmd.GET_POLYGON) &&
				getCmd.getVariableIdentifier() == PolygonVar.DISTANCE.id) {
			return new TraCIGetDistanceCommand(getCmd);
		} else {
			throw new TraCICommandCreationException("cannot create TraCIGetDistanceCommand from %s", getCmd.toString());
		}
	}

	public ArrayList<String> getPoint() {
		return point;
	}

	public void setPoint(ArrayList<String> point) {
		this.point = point;
	}

}
