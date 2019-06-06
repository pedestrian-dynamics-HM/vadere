package org.vadere.manager.stsc.commands;

import org.vadere.manager.TraCIException;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.TraCIPacket;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import java.util.EnumSet;

public class TraCIGetCommand extends TraCICommand {

	protected int variableId;
	protected String elementId;

	// response
	private TraCICmd responseIdentifier;
	private TraCIDataType responseDataType;
	private Object responseData;

	public TraCIGetCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
		super(traCICmd);
		EnumSet<TraCICmd> set;
		try{
			variableId = cmdBuffer.reader.readUnsignedByte();
			elementId = cmdBuffer.reader.readString();
		} catch (Exception e){
			throw TraCIException.cmdErr(traCICmd, e);
		}
	}


	public int getVariableId() {
		return variableId;
	}

	public String getElementId() {
		return elementId;
	}

	public TraCIDataType getResponseDataType() {
		return responseDataType;
	}

	public void setResponseDataType(TraCIDataType responseDataType) {
		this.responseDataType = responseDataType;
	}

	public Object getResponseData() {
		return responseData;
	}

	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}

	public TraCICmd getResponseIdentifier() {
		return responseIdentifier;
	}

	public void setResponseIdentifier(TraCICmd responseIdentifier) {
		this.responseIdentifier = responseIdentifier;
	}

	public TraCIGetCommand addResponseIdentifier(TraCICmd val){
		setResponseIdentifier(val);
		return this;
	}

	public TraCIGetCommand addResponseVariableType(TraCIDataType val){
		setResponseDataType(val);
		return this;
	}

	public TraCIGetCommand addResponseData(Object val){
		setResponseData(val);
		return this;
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		if (NOK_response != null)
			return NOK_response;
		else
			return TraCIPacket.create().wrapGetResponse(this);
	}
}
