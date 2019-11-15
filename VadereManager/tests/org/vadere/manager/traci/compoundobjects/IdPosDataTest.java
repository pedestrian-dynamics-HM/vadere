package org.vadere.manager.traci.compoundobjects;

import org.junit.Test;
import org.vadere.util.geometry.shapes.VPoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class IdPosDataTest {

	@Test
	public void IdPosDataTestBuilderTest(){
		CompoundObject o = CompoundObjectBuilder.createIdPosData("5", "3.3", "4.4");
		IdPosData p = new IdPosData(o);

		assertThat(p.getId(), equalTo("5"));
		assertThat(p.getPos(), equalTo(new VPoint(3.3, 4.4)));
	}

}