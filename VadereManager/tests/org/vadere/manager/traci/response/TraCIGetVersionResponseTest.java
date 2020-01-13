package org.vadere.manager.traci.response;

import org.junit.Test;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.reader.TraCICommandBuffer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TraCIGetVersionResponseTest {


	@Test
	public void createFromBuffer(){
		byte[] data = new byte[]{0,0,0,34, 0,0,0,2, 65, 65};
		TraCIGetVersionResponse response = new TraCIGetVersionResponse(
				new StatusResponse(TraCICmd.GET_VERSION, TraCIStatusResponse.OK, ""),
				TraCICommandBuffer.wrap(data)
		);

		assertThat(response.getStatusResponse(), equalTo(new StatusResponse(TraCICmd.GET_VERSION, TraCIStatusResponse.OK, "")));
		assertThat(response.getResponseIdentifier(), equalTo(TraCICmd.GET_VERSION));
		assertThat(response.getVersionString(), equalTo("AA"));
		assertThat(response.getVersionId(), equalTo(34));
	}

}