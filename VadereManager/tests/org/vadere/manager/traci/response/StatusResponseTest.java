package org.vadere.manager.traci.response;

import org.junit.Test;
import org.vadere.manager.traci.TraCICmd;

import java.nio.ByteBuffer;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class StatusResponseTest {




	@Test
	public void createFromByteBuffer() {
		//length removed
		byte[] data = new byte[]{(byte)TraCICmd.GET_VERSION.id, (byte)TraCIStatusResponse.OK.id, 0, 0, 0, 0};

		StatusResponse r = StatusResponse.createFromByteBuffer(ByteBuffer.wrap(data));

		assertThat(r.getCmdIdentifier(), equalTo(TraCICmd.GET_VERSION));
		assertThat(r.getResponse(), equalTo(TraCIStatusResponse.OK));
		assertThat(r.getDescription(), equalTo(""));
	}

	@Test
	public void getCmdIdentifier() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		assertThat(r.getCmdIdentifier(), equalTo(TraCICmd.GET_EDGE_VALUE));
	}

	@Test
	public void setCmdIdentifier() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		r.setCmdIdentifier(TraCICmd.SEND_FILE);
		assertThat(r.getCmdIdentifier(), equalTo(TraCICmd.SEND_FILE));
	}

	@Test
	public void getResponse() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		assertThat(r.getResponse(), equalTo(TraCIStatusResponse.ERR));
	}

	@Test
	public void setResponse() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		r.setResponse(TraCIStatusResponse.NOT_IMPLEMENTED);
		assertThat(r.getResponse(), equalTo(TraCIStatusResponse.NOT_IMPLEMENTED));
	}

	@Test
	public void getDescription() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		assertThat(r.getDescription(), equalTo("Test"));
	}

	@Test
	public void setDescription() {
		StatusResponse r = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		r.setDescription("Test2");
		assertThat(r.getDescription(), equalTo("Test2"));
	}

	@Test
	public void equals1() {
		StatusResponse r1 = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		StatusResponse r2 = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.ERR, "Test");
		StatusResponse r3 = new StatusResponse(TraCICmd.GET_EDGE_VALUE, TraCIStatusResponse.OK, "Test");

		assertThat(r1, equalTo(r2));
		assertThat(r1, not(equalTo(r3)));
		assertThat(r2, not(equalTo(r3)));
	}

}