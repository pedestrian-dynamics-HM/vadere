package org.vadere.manager.traci.response;

import org.junit.Test;
import org.vadere.manager.traci.commandHandler.variables.PersonVar;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.state.traci.TraCIDataType;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TraCIGetResponseTest {


	@Test
	public void createFromBuffer(){
		byte[] data = new byte[]{(byte) PersonVar.COUNT.id, 0,0,0,2, 65, 65, (byte)TraCIDataType.INTEGER.id, 0,0,0,78};
		TraCIGetResponse response = new TraCIGetResponse(
				new StatusResponse(TraCICmd.GET_PERSON_VALUE, TraCIStatusResponse.OK, ""),
				TraCICmd.RESPONSE_GET_PERSON_VALUE,
				TraCICommandBuffer.wrap(data)
		);

		assertThat(response.getStatusResponse(), equalTo(new StatusResponse(TraCICmd.GET_PERSON_VALUE, TraCIStatusResponse.OK, "")));
		assertThat(response.getResponseIdentifier(), equalTo(TraCICmd.RESPONSE_GET_PERSON_VALUE));
		assertThat(response.getElementIdentifier(), equalTo("AA"));
		assertThat(response.getResponseData(), equalTo(78));
		assertThat(response.getResponseDataType(), equalTo(TraCIDataType.INTEGER));
	}

}