package org.vadere.simulator.models.seating;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.vadere.simulator.models.seating.trainmodel.TrainModel;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesSeating;
import org.vadere.state.scenario.Et423Geometry;
import org.vadere.state.scenario.Topography;
import org.vadere.state.scenario.TrainGeometry;
import org.vadere.state.util.StateJsonConverter;

public class TestTopographyAndModelBuilder {
	
	private static final String TEST_TRAIN_TOPOGRAPHY_RESOURCE = "/data/test-train-topography.json";

	public static final int nEntranceAreas = 12;
	public static final int nCompartments = nEntranceAreas + 1; // includes 2 half-compartments
	public static final int nCompartmentTargets = nCompartments; // includes 2 targets from half-compartments
	public static final int nSeatGroups = nCompartments * 4 - 4;
	public static final int nSeats = nSeatGroups * 4;
	public static final int nSeatRows = nCompartments * 4 - 4;
	// not the sum of the counts from the --stop options (when generating the scenario)
	public static final int nSources = 48;
	public static final int nSourcesLeft = 24;
	public static final int nSourcesRight = 24;

	private Topography topography;

	public TestTopographyAndModelBuilder() {
		topography = createTestTopography();
	}

	public Topography getTopography() {
		return topography;
	}

	public TrainModel getTrainModel() {
		final Topography topography = createTestTopography();
		return new TrainModel(topography, getTrainGeometry());
	}
	
	public SeatingModel getSeatingModel() {
		final SeatingModel model = new SeatingModel();
		final List<Attributes> attributes = Collections.singletonList(new AttributesSeating());
		model.initialize(attributes, new Domain(topography), null, new Random());
		return model;
	}

	private Topography createTestTopography() {
		try {
			@SuppressWarnings("resource")
			final String json = new Scanner(TestTopographyAndModelBuilder.class.getResourceAsStream(TEST_TRAIN_TOPOGRAPHY_RESOURCE), "UTF-8").useDelimiter("\\A").next();
			return StateJsonConverter.deserializeTopography(json);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public TrainGeometry getTrainGeometry() {
		return new Et423Geometry();
	}

}
