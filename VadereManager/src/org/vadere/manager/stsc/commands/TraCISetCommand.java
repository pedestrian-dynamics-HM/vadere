package org.vadere.manager.stsc.commands;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICommandBuffer;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.TraCIPacket;

public class TraCISetCommand extends TraCICommand{

	protected int variableId;
	protected String elementId;
	protected TraCIDataType returnDataType;
	protected Object variableValue;


	public TraCISetCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		try{
			variableId = cmdBuffer.reader.readUnsignedByte();
			elementId = cmdBuffer.reader.readString();
			int dataTye = cmdBuffer.reader.readUnsignedByte();
			returnDataType = TraCIDataType.fromId(dataTye);
			if (returnDataType.isUnknown())
				throw new TraCIException("unknown TraCIDataType found in command: " + dataTye);
			variableValue = cmdBuffer.reader.readTypeValue(returnDataType);
		} catch (Exception e){
			throw TraCIException.cmdErr(traCICmd, e);
		}
	}

	public Object getVariableValue(){
		return variableValue;
	}

	public int getVariableId() {
		return variableId;
	}

	public String getElementId() {
		return elementId;
	}

	public TraCIDataType getReturnDataType() {
		return returnDataType;
	}

	@Override
	public TraCIPacket handleCommand(TraCIPacket response) {
		return null;
	}
}
