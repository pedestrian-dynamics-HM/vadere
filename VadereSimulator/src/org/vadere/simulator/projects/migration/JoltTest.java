package org.vadere.simulator.projects.migration;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.Diffy;
import com.bazaarvoice.jolt.JsonUtils;

import java.util.List;

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

	public void run2() {


		List chainrSpecJson = JsonUtils.classpathToList("/transfrom_v2_to_v3.json");
		Chainr chainr = Chainr.fromSpec(chainrSpecJson);

		Object inputJson = JsonUtils.classpathToObject("/scenario_test.json");

		Object transformedOutput = chainr.transform(inputJson);

		String in = JsonUtils.toPrettyJsonString(inputJson);
		String out = JsonUtils.toPrettyJsonString(transformedOutput);
		System.out.println(out);

		String[] inarr = in.split(System.getProperty("line.separator"));
		String[] outarr = out.split(System.getProperty("line.separator"));

		Diffy diffy = new Diffy();
		Diffy.Result res = diffy.diff(inputJson, transformedOutput);
		System.out.println(res.toString());

		//todo leere Attribute
		//
	}
}
