package org.vadere.simulator.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Before;
import org.junit.Test;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.io.TextOutOfNodeException;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestShapeToJson {
	private Topography topography;

	@Before
	public void setUp() throws Exception {
		VPolygon polygon = GeometryUtils.polygonFromPoints2D(new VPoint(0, 0),
				new VPoint(10, 0), new VPoint(10, 10), new VPoint(0, 10));

		topography = new Topography();
		topography.addObstacle(new Obstacle(new AttributesObstacle(1, polygon)));
	}

	@Test
	public void testShapeToJson() throws IOException, TextOutOfNodeException {
		assertEquals(topography, JsonConverter.deserializeTopography(JsonConverter.serializeTopography(topography)));
	}

}
