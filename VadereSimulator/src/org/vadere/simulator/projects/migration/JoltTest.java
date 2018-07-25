package org.vadere.simulator.projects.migration;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.vadere.state.attributes.models.*;
import org.vadere.state.attributes.scenario.AttributesCar;
import org.vadere.state.util.StateJsonConverter;

import java.util.Arrays;
import java.util.List;

import joptsimple.internal.Strings;


public class JoltTest {

	public static void main(String[] args) {
		new JoltTest().run2();
	}

	public void run() {

//		String s = getClass().getResourceAsStream("simpleTestSpec.json").toString();
//		System.out.println(s);
//		org.vadere.state.attributes.models.
		int i = 0;
		List chainrSpecJson = JsonUtils.classpathToList("/simpleTestSpec.json");
		Chainr chainr = Chainr.fromSpec(chainrSpecJson);

		Object inputJson = JsonUtils.classpathToObject("/testJson.json");

		Object transformedOutput = chainr.transform(inputJson);
		System.out.println(JsonUtils.toPrettyJsonString(transformedOutput));
	}

	public void attr() {
		Class[] attrs = new Class[]{AttributesBHM.class, AttributesBMM.class, AttributesCar.class, AttributesCGM.class, AttributesFloorField.class, AttributesGFM.class, AttributesGNM.class, AttributesOSM.class, AttributesOVM.class, AttributesParticles.class, AttributesPotentialCompact.class, AttributesPotentialCompactSoftshell.class, AttributesPotentialGNM.class, AttributesPotentialGNM.class, AttributesPotentialOSM.class, AttributesPotentialOSM.class, AttributesPotentialParticles.class, AttributesPotentialParticles.class, AttributesPotentialRingExperiment.class, AttributesPotentialRingExperiment.class, AttributesPotentialSFM.class, AttributesPotentialSFM.class, AttributesQueuingGame.class, AttributesReynolds.class, AttributesSFM.class,};

//		Arrays.stream(attrs).forEach(c -> System.out.println(c.getCanonicalName()));

		for (Class attr : attrs) {

			JsonNode x = null;
			try {
				x = StateJsonConverter.toJsonNode(attr.newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			try {
				System.out.print(attr.getCanonicalName());
				System.out.println(StateJsonConverter.serializeJsonNode(x));
				System.out.println(",");

			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}


	}


	public void run2() {


		List chainrSpecJson = JsonUtils.classpathToList("/transform_v0_to_v1.json");
		Chainr chainr = Chainr.fromSpec(chainrSpecJson);

		List identity = JsonUtils.classpathToList("/identity_v1.json");
		Chainr identityTransform = Chainr.fromSpec(identity);

		Object inputJson = JsonUtils.classpathToObject("/test.json");

		Object transformedOutput = chainr.transform(inputJson);

		String in = JsonUtils.toPrettyJsonString(inputJson);
		String out = JsonUtils.toPrettyJsonString(transformedOutput);
		System.out.println(out);

//		String[] inarr = in.split(System.getProperty("line.separator"));
//		String[] outarr = out.split(System.getProperty("line.separator"));
		System.out.println(Strings.repeat('#', 120));
		System.out.println(JsonUtils.toPrettyJsonString(identityTransform.transform(transformedOutput)));
		Diffy diffy = new Diffy();
		Diffy.Result res = diffy.diff(transformedOutput, identityTransform.transform(transformedOutput));
		System.out.println(res.toString());

		//todo leere Attribute
		//
	}
}
