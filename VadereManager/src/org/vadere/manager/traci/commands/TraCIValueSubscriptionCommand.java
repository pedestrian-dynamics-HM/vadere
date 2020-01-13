package org.vadere.manager.traci.commands;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.response.TraCISubscriptionResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

import java.util.ArrayList;
import java.util.List;

public class TraCIValueSubscriptionCommand extends TraCICommand {

	private double beginTime;
	private double endTime;
	private String elementIdentifier;
	private int numberOfVariables;
	private List<Integer> variables;

	private List<TraCIGetCommand> getCommands;

	private TraCISubscriptionResponse response;


	protected TraCIValueSubscriptionCommand(TraCICmd traCICmd, TraCICommandBuffer buffer) {
		this(traCICmd);
		beginTime = buffer.readDouble();
		endTime = buffer.readDouble();
		elementIdentifier = buffer.readString();
		numberOfVariables = buffer.readUnsignedByte();

		for (int i = 0; i < numberOfVariables; i++) {
			int var = buffer.readUnsignedByte();
			variables.add(var);
		}
	}

	protected TraCIValueSubscriptionCommand(TraCICmd traCICmd) {
		super(traCICmd);
		variables = new ArrayList<>();
		getCommands = new ArrayList<>();
	}

	@Override
	public TraCIPacket buildResponsePacket() {
		return TraCIPacket.create().wrapValueSubscriptionCommand(response);
	}

	public double getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(double beginTime) {
		this.beginTime = beginTime;
	}

	public double getEndTime() {
		return endTime;
	}

	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}

	public String getElementIdentifier() {
		return elementIdentifier;
	}

	public void setElementIdentifier(String elementIdentifier) {
		this.elementIdentifier = elementIdentifier;
	}

	public int getNumberOfVariables() {
		return numberOfVariables;
	}

	public void setNumberOfVariables(int numberOfVariables) {
		this.numberOfVariables = numberOfVariables;
	}

	public List<Integer> getVariables() {
		return variables;
	}

	public void setVariables(List<Integer> variables) {
		this.variables = variables;
	}

	public TraCISubscriptionResponse getResponse() {
		return response;
	}

	public void setResponse(TraCISubscriptionResponse response) {
		this.response = response;
	}

	public List<TraCIGetCommand> getGetCommands() {
		return getCommands;
	}

	public void setGetCommands(List<TraCIGetCommand> getCommands) {
		this.getCommands = getCommands;
	}
}
