package org.vadere.simulator.io;

import org.junit.Before;
import org.junit.Test;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTeleporter;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Teleporter;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import com.fasterxml.jackson.core.JsonProcessingException;

public class TestScenarioToJson {
	private static final AttributesTopography attributesTopography = new AttributesTopography();
	private static final String scenarioJson = "{\"attributes\":{\"bounds\":{"
			+ "\"x\":" + attributesTopography.getBounds().getX()
			+ ",\"y\":" + attributesTopography.getBounds().getY()
			+ ",\"width\":" + attributesTopography.getBounds().getWidth()
			+ ",\"height\":" + attributesTopography.getBounds().getHeight()
			+ "}"
			+ ",\"boundingBoxWidth\":" + attributesTopography.getBoundingBoxWidth()
			+ ",\"bounded\":" + attributesTopography.isBounded() + "}"
			+ ",\"obstacles\":[{\"shape\":{\"type\":\"POLYGON\",\"points\":[{\"x\":0.0,\"y\":0.0},{\"x\":10.0,\"y\":0.0},{\"x\":10.0,\"y\":10.0},{\"x\":0.0,\"y\":10.0}]},\"id\":1}]"
			+ ",\"stairs\":[]" + ",\"targets\":[]" + ",\"sources\":[]" + ",\"dynamicElements\":[]"
			+ ",\"pedestrians\":[]"
			+ ",\"teleporter\":{\"shift\":{\"x\":0.0,\"y\":0.0},\"position\":{\"x\":0.0,\"y\":0.0}}}";
	private Topography scenario;

	@Before
	public void setUp() throws Exception {
		VPolygon polygon = GeometryUtils.polygonFromPoints2D(new VPoint(0, 0),
				new VPoint(10, 0), new VPoint(10, 10), new VPoint(0, 10));

		scenario = new Topography();

		AttributesObstacle attributes = new AttributesObstacle(1, polygon);
		scenario.addObstacle(new Obstacle(attributes));

		AttributesTeleporter attributesTeleporter = new AttributesTeleporter();
		scenario.setTeleporter(new Teleporter(attributesTeleporter));
	}

	@Test
	public void testScenarioToJson() throws JsonProcessingException {

	/*	String jsonString = JsonConverter.serializeTopography(scenario);
		Assert.assertEquals("Scenario json is not correct.", scenarioJson, jsonString);
	*/
	}

	@Test
	public void testInversion() {
		/*
		 * JsonElement jsonElement = JsonSerializerTopography
		 * .topographyToJson(scenario);
		 * Topography newScenario = JsonSerializerTopography
		 * .topographyFromJson(jsonElement);
		 * 
		 * Assert.assertEquals(
		 * "scenarioToJson is not the inverse of scenarioFromJson",
		 * scenario, newScenario);
		 */
	}
}
