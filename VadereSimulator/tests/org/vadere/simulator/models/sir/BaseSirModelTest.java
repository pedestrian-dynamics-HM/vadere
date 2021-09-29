package org.vadere.simulator.models.sir;

import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributeSIR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseSirModelTest {

	public List<Attributes> getSimpleState() {
		ArrayList<Attributes> attrList = new ArrayList<>();
		var att = new AttributeSIR();
		att.setInitialR(2.0);
		ArrayList<Integer> ids = new ArrayList<>(Arrays.asList(1, 2, 3));
		att.setInfectionZoneIds(ids);
		attrList.add(att);

		return attrList;
	}

}
