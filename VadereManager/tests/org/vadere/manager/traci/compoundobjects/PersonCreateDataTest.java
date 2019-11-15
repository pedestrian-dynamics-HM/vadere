package org.vadere.manager.traci.compoundobjects;

import org.junit.Test;
import org.vadere.manager.TraCIException;
import org.vadere.manager.traci.TraCIDataType;
import org.vadere.util.geometry.shapes.VPoint;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PersonCreateDataTest {

	@Test
	public void PersonCreateDataBuilderTest(){
		CompoundObject o = CompoundObjectBuilder.createPerson("5", "3.3", "4.4", "5", "7");
		PersonCreateData p = new PersonCreateData(o);

		assertThat(p.getId(), equalTo("5"));
		assertThat(p.getPos(), equalTo(new VPoint(3.3, 4.4)));
		ArrayList<Integer> s = new ArrayList<>();
		s.add(5);
		s.add(7);
		assertThat(p.getTargetsAsInt(), equalTo(s));
	}

	@Test(expected = TraCIException.class)
	public void PersonCreateDataErrTest(){
		CompoundObject o = CompoundObjectBuilder.builder()
				.rest()
				.add(TraCIDataType.POS_2D)
				.build("5.5", "4.4");
		PersonCreateData p = new PersonCreateData(o);
	}

}