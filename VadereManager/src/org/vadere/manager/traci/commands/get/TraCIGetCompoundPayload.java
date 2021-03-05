package org.vadere.manager.traci.commands.get;

import org.vadere.state.traci.TraCIException;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.state.traci.CompoundObject;

public class TraCIGetCompoundPayload extends TraCIGetCommand {

	private CompoundObject data;

	public TraCIGetCompoundPayload(TraCICmd traCICmd, int variableIdentifier, String elementIdentifier) {
		super(traCICmd, variableIdentifier, elementIdentifier);
		data = null;
	}

	public TraCIGetCompoundPayload(TraCIGetCommand c) {
		super(c.getTraCICmd(), c.getVariableIdentifier(), c.getElementIdentifier());
		// expecting a CompoundObject thus check if DataType Byte is present.
		c.getCmdBuffer().ensureBytes(1);
		TraCIDataType dType = TraCIDataType.fromId(c.getCmdBuffer().readUnsignedByte());
		if (!dType.equals(TraCIDataType.COMPOUND_OBJECT)){
			throw new TraCIException("expected Compound Object in GetCommand.");
		}
		this.data = c.getCmdBuffer().readCompoundObject();
	}

	public CompoundObject getData() {
		return data;
	}

	public void setData(CompoundObject data) {
		this.data = data;
	}
}
