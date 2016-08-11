package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processors.AttributesDensityTest;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;

public class PedestrianDensityTest extends AbstractProcessor implements ModelTest {

	private PedestrianDensityProcessor pedestrianDensityProcessor;
	private AttributesDensityTest attributes;

	@Expose
	private final Table table;

	@Expose
	private int lastStep;

	@Expose
	private Set<String> allSupportedColumns;

	@Expose
	private boolean success;

	@Expose
	private double lastMean;
	private String errorMessage;
	private double errorTime;

	public PedestrianDensityTest(final PedestrianDensityProcessor pedestrianDensityProcessor,
			final AttributesDensityTest attributes) {
		super(new Table("densityTest"));
		this.pedestrianDensityProcessor = pedestrianDensityProcessor;
		this.pedestrianDensityProcessor.addColumnNames(pedestrianDensityProcessor.getAllColumnNames());
		this.attributes = attributes;
		this.table = getTable();
		this.lastStep = 0;
		this.success = true;
		this.errorTime = -1;

		allSupportedColumns = new HashSet<>();
		allSupportedColumns.addAll(Arrays.asList(pedestrianDensityProcessor.getAllColumnNames()));
		allSupportedColumns.addAll(Arrays.asList(getTable().getColumnNames()));
	}

	public PedestrianDensityTest(final PedestrianDensityProcessor pedestrianDensityProcessor) {
		this(pedestrianDensityProcessor, new AttributesDensityTest());
	}

	public PedestrianDensityTest() {
		this(new PedestrianDensityProcessor(new PedestrianPositionProcessor(), new DensityVoronoiGeoProcessor()));
	}

	@Override
	public String[] getAllColumnNames() {
		return allSupportedColumns.toArray(new String[] {});
	}

	@Override
	public Table preLoop(final SimulationState state) {
		this.success = true;
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(final SimulationState state) {
		Table densityTable = pedestrianDensityProcessor.postUpdate(state);

		if (lastStep != state.getStep()) {
			ListIterator<Row> rowIterator = densityTable.listMapIterator();

			double currentMean = 0;

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				double density = (Double) row.getEntry(pedestrianDensityProcessor.getDensityType());
				currentMean += density;
				if (density > attributes.getMaxDensity()) {
					errorMessage = ("Density is too high (" + density + " > " + attributes.getMaxDensity() + ")");
					success = false;
				} else if (density < attributes.getMinDensity()) {
					errorMessage = ("Density is too low (" + density + " < " + attributes.getMinDensity() + ")");
					success = false;
				} else if (lastMean > attributes.getMaxMeanDensity()) {
					errorMessage = ("Density mean of last step was too high (" + lastMean + " > "
							+ attributes.getMaxMeanDensity() + ")");
					success = false;
				} else if (lastMean < attributes.getMinMeanDensity()) {
					errorMessage = ("Density mean of last step was too low (" + lastMean + " < "
							+ attributes.getMinMeanDensity() + ")");
					success = false;
				}

				if (success == false && errorTime < 0) {
					errorTime = state.getSimTimeInSec();
				}
			}
			currentMean = currentMean / densityTable.size();
			lastMean = currentMean;
		}

		lastStep = state.getStep();

		return table;
	}

	@Override
	public Table postLoop(SimulationState state) {

		if (table.isEmpty()) {
			if (attributes.isExpectFailure() && success) {
				success = false;
				errorMessage = "Failure: Density is ok even though it was expected not to be.";
			}

			if (success) {
				table.addRow();
				table.addColumnEntry("densityTest", "Density ok.");
				table.addRow();
				table.addColumnEntry("densityTest", "SUCCESS");
			} else {
				table.addRow();
				table.addColumnEntry("densityTest", errorMessage);
				if (errorTime >= 0) {
					table.addRow();
					table.addColumnEntry("densityTest", errorTime);
				}
				table.addRow();
				table.addColumnEntry("densityTest", "FAILURE");
			}
		}

		return table;
	}

	@Override
	public boolean isSucceeded() {
		return success;
	}

	@Override
	public Attributes getAttributes() {
		return attributes;
	}
}
