package org.vadere.simulator.models.seating;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.simulator.projects.io.TextOutOfNodeException;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.scenario.Et423Geometry;
import org.vadere.state.scenario.Topography;

public class TestTopographyAndModelBuilder {
	
	private Topography topography;

	public TestTopographyAndModelBuilder() {
		topography = createTestTopography();
	}

	public Topography getTopography() {
		return topography;
	}

	public TrainModel getTrainModel() {
		final Topography topography = createTestTopography();
		return new TrainModel(topography, new Et423Geometry());
	}
	
	public SeatingModel getSeatingModel() {
		final SeatingModel model = new SeatingModel();
		final List<Attributes> attributes = Collections.singletonList(new AttributesSeating());
		model.initialize(attributes, topography, null, new Random());
		return model;
	}

	private Topography createTestTopography() {
		try {
			@SuppressWarnings("resource")
			final String json = new Scanner(TestTopographyAndModelBuilder.class.getResourceAsStream("/data/test-train-topography.json"), "UTF-8").useDelimiter("\\A").next();
			return JsonConverter.deserializeTopography(json);
		} catch (IOException | TextOutOfNodeException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}


}
