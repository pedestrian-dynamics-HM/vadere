package org.vadere.manager.traci.commands;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.reader.TraCICommandBuffer;
import org.vadere.manager.traci.response.StatusResponse;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;
import org.vadere.state.traci.TraCIDataType;

/**
 * Sub class of {@link TraCICommand} which represents a set request to some API.
 *
 * <p>An API in this context is for instance the Person(GET/SET), Simulation(GET/SET/SUB)
 *
 * <p>Command Structure
 *
 * <p>[ cmdIdentifier(based on API) ] [ variableId ] [ elementId] [ dataTypeId ] [ data ]
 *
 * <p>- cmdIdentifier(based on API): see {@link TraCICmd} enum GET_**** - variableId: Id for the
 * var. The numbers may be the same between different APIs see {@link PersonVar} enum - elementId:
 * String based id for the object (i.e. a pedestrianId) - dataTypeId: see {@link TraCIDataType} -
 * data: data to be returned.
 *
 * <p>see {@link org.vadere.manager.traci.commandHandler.PersonCommandHandler} for execution
 * handing.
 */
public class TraCISetCommand extends TraCICommand {

  protected int variableId;
  protected String elementId;
  protected TraCIDataType returnDataType;
  protected Object variableValue;

  private StatusResponse statusResponse;

  public TraCISetCommand(TraCICmd traCICmd, TraCICommandBuffer cmdBuffer) {
    super(traCICmd);
    variableId = cmdBuffer.readUnsignedByte();
    elementId = cmdBuffer.readString();
    returnDataType = TraCIDataType.fromId(cmdBuffer.readUnsignedByte());
    variableValue = cmdBuffer.readTypeValue(returnDataType);
  }

  public static TraCIPacket build(
      TraCICmd commandIdentifier,
      String elementIdentifier,
      int variableIdentifier,
      TraCIDataType dataType,
      Object data) {
    return TraCIPacket.create()
        .wrapCommand(commandIdentifier, elementIdentifier, variableIdentifier, dataType, data);
  }

  public Object getVariableValue() {
    return variableValue;
  }

  public int getVariableId() {
    return variableId;
  }

  public int getVariableIdentifier() {
    return variableId;
  }

  public String getElementId() {
    return elementId;
  }

  public TraCIDataType getReturnDataType() {
    return returnDataType;
  }

  public StatusResponse getStatusResponse() {
    return statusResponse;
  }

  public TraCISetCommand setErr(String desc) {
    statusResponse = new StatusResponse(traCICmd, TraCIStatusResponse.ERR, desc);
    return this;
  }

  public TraCISetCommand setOK(String descr) {
    statusResponse = new StatusResponse(traCICmd, TraCIStatusResponse.OK, descr);
    return this;
  }

  public TraCISetCommand setOK() {
    return setOK("");
  }

  @Override
  public TraCIPacket buildResponsePacket() {
    return TraCIPacket.create().addStatusResponse(statusResponse);
  }
}
