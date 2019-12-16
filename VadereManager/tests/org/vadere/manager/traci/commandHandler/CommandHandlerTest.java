package org.vadere.manager.traci.commandHandler;

import org.hamcrest.core.IsEqual;
import org.vadere.manager.traci.commands.TraCICommand;
import org.vadere.manager.traci.commands.TraCIGetCommand;
import org.vadere.manager.traci.respons.TraCIStatusResponse;
import org.vadere.manager.traci.writer.TraCIPacket;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class CommandHandlerTest {

    TraCICommand getFirstCommand(TraCIPacket packet){
        List<TraCICommand> cmds = packet.getCommands();
        assertThat("expected single command in Package",cmds.size(), IsEqual.equalTo(1));
        return cmds.get(0);
    }

    public void checkGET_OK(TraCICommand cmd){
        assertThat("command must be a TraCIGetCommand",cmd, instanceOf(TraCIGetCommand.class));
        TraCIGetCommand getCmd = (TraCIGetCommand)cmd;
        assertThat("Response must be OK",getCmd.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.OK));
    }

    public void checkGET_Err(TraCICommand cmd){
        assertThat("command must be a TraCIGetCommand",cmd, instanceOf(TraCIGetCommand.class));
        TraCIGetCommand getCmd = (TraCIGetCommand)cmd;
        assertThat("Response must be Err",getCmd.getResponse().getStatusResponse().getResponse(), equalTo(TraCIStatusResponse.ERR));
    }

    public void checkElementIdentifier(TraCIGetCommand cmd, String identifer){
        assertThat(cmd.getResponse().getElementIdentifier(), equalTo(identifer));
    }

    public void checkReturnValue(TraCIGetCommand cmd, Object data){
        assertThat(cmd.getResponse().getResponseData(), equalTo(data));
    }

    public void checkVariableIdentifier(TraCIGetCommand cmd, int identifier){
        assertThat(cmd.getResponse().getVariableIdentifier(), equalTo(identifier));
    }
}
