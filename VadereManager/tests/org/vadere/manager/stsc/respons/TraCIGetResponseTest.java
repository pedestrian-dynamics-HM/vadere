package org.vadere.manager.stsc.respons;

import org.junit.Test;
import org.vadere.manager.commandHandler.TraCIPersonVar;
import org.vadere.manager.stsc.TraCICmd;
import org.vadere.manager.stsc.TraCIDataType;
import org.vadere.manager.stsc.reader.TraCICommandBuffer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TraCIGetResponseTest {


	@Test
	public void createFromBuffer(){
		byte[] data = new byte[]{(byte)TraCIPersonVar.COUNT.id, 0,0,0,2, 65, 65, (byte)TraCIDataType.INTEGER.identifier, 0,0,0,78};
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