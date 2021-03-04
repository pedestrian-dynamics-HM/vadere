package org.vadere.manager.traci.commandHandler;

import org.hamcrest.core.IsEqual;
import org.vadere.manager.traci.CmdType;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.commands.TraCISetCommand;
import org.vadere.manager.traci.commands.control.TraCIGetVersionCommand;
import org.vadere.manager.traci.response.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class CommandHandlerTest {

	TraCICommand getFirstCommand(TraCIPacket packet) {
		List<TraCICommand> cmds = packet.getCommands();
		assertThat("expected single command in Package", cmds.size(), IsEqual.equalTo(1));
		return cmds.get(0);
	}

	// GET
	public void checkGET_OK(TraCICommand cmd) {
		assertThat("command must be a TraCIGetCommand", cmd, instanceOf(TraCIGetCommand.class));
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;
		assertThat("Response must be OK", getCmd.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
	}

	public void checkGETVersion_OK(TraCICommand cmd) {
		assertThat("command must be a TraCIGetVersionCommand", cmd, instanceOf(TraCIGetVersionCommand.class));
		TraCIGetVersionCommand getVersionCommand = (TraCIGetVersionCommand) cmd;
		assertThat("Response must be OK", getVersionCommand.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
	}

	public void checkGET_Err(TraCICommand cmd) {
		assertThat("command must be a TraCIGetCommand", cmd, instanceOf(TraCIGetCommand.class));
		TraCIGetCommand getCmd = (TraCIGetCommand) cmd;
		assertThat("Response must be Err", getCmd.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.ERR));
	}

	public void checkElementIdentifier(TraCIGetCommand cmd, String identifer) {
		assertThat(cmd.getResponse().getElementIdentifier(), equalTo(identifer));
	}

	public void checkReturnValue(TraCIGetCommand cmd, Object data) {
		assertThat(cmd.getResponse().getResponseData(), equalTo(data));
	}

	public void checkVariableIdentifier(TraCIGetCommand cmd, int identifier) {
		assertThat(cmd.getResponse().getVariableIdentifier(), equalTo(identifier));
	}

	public void checkReturnType(TraCIGetCommand cmd, TraCIDataType type) {
		assertThat(cmd.getResponse().getResponseDataType(), equalTo(type));
	}
	// GET



	// SET
	public void checkSET_OK(TraCICommand cmd) {
		assertThat("command must be a TraCISetCommand", cmd, instanceOf(TraCISetCommand.class));
		TraCISetCommand setCmd = (TraCISetCommand) cmd;
		assertThat("Response must be OK", setCmd.getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
	}

	public void checkSET_Err(TraCICommand cmd) {
		assertThat("command must be a TraCISetCommand", cmd, instanceOf(TraCISetCommand.class));
		TraCISetCommand setCmd = (TraCISetCommand) cmd;
		assertThat("Response must be ERR", setCmd.getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.ERR));
	}

	public void checkElementId(TraCISetCommand cmd, String identifer) {
		assertThat(cmd.getElementId(), equalTo(identifer));
	}

	public void checkVariableId(TraCISetCommand cmd, int identifier) {
		assertThat(cmd.getVariableId(), equalTo(identifier));
	}

	public void checkVariableType(TraCISetCommand cmd, TraCIDataType type) {
		assertThat(cmd.getReturnDataType(), equalTo(type));
	}

	public void checkVariableValue(TraCISetCommand cmd, Object data) {
		assertThat(cmd.getVariableValue(), equalTo(data));
	}
	// SET



	// Compound checks
	public void testGetValue(TraCICommand ret, int varID, TraCIDataType varType, String elementID, Object retVal) {
		TraCIGetCommand getRet = (TraCIGetCommand) ret;
		checkVariableIdentifier(getRet, varID);
		checkReturnType(getRet, varType);
		checkReturnValue(getRet, retVal);
		checkElementIdentifier(getRet, elementID);
	}

	public void testSetValue(TraCICommand ret, int varID, TraCIDataType varType, String elementID, Object val) {
		TraCISetCommand setRet = (TraCISetCommand) ret;
		checkVariableId(setRet, varID);
		checkVariableType(setRet, varType);
		checkVariableValue(setRet, val);
		checkElementId(setRet, elementID);
	}

	public void testTraCICommand(TraCICommand ret, TraCICmd traciCmd, CmdType cmdType){
		assertThat(ret.getTraCICmd(), equalTo(traciCmd));
		assertThat(ret.getCmdType(), equalTo(cmdType));
	}
}
