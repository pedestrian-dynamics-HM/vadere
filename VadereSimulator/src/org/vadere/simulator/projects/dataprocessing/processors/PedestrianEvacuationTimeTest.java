package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processors.AttributesEvacuationTimeTest;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;

public class PedestrianEvacuationTimeTest extends AbstractProcessor implements ModelTest {

	@Expose
	private PedestrianLastPositionProcessor pedestrianPositionProcessor;

	private AttributesEvacuationTimeTest attributes;

	@Expose
	private final Table table;

	@Expose
	private int lastStep;

	@Expose
	private Set<String> allSupportedColumns;

	@Expose
	private boolean success;

	private double errorTime;

	public PedestrianEvacuationTimeTest(final AttributesEvacuationTimeTest attributes) {
		super(new Table("evacuationTimeTest"));
		this.pedestrianPositionProcessor = new PedestrianLastPositionProcessor();
		this.pedestrianPositionProcessor.addColumnNames(pedestrianPositionProcessor.getAllColumnNames());
		this.attributes = attributes;
		this.table = getTable();
		this.success = true;
		this.errorTime = -1;

		allSupportedColumns = new HashSet<>();
		allSupportedColumns.addAll(Arrays.asList(getTable().getColumnNames()));
	}

	public PedestrianEvacuationTimeTest() {
		this(new AttributesEvacuationTimeTest());
	}

	@Override
	public String[] getAllColumnNames() {
		return allSupportedColumns.toArray(new String[] {});
	}

	@Override
	public Table preLoop(final SimulationState state) {
		pedestrianPositionProcessor.preLoop(state);
		this.success = true;
		this.lastStep = state.getStep();
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(SimulationState state) {
		pedestrianPositionProcessor.postUpdate(state);
		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {
		Table densityTable = pedestrianPositionProcessor.postLoop(state);

		if (table.isEmpty() || lastStep != state.getStep()) {
			table.clear();
			ListIterator<Row> rowIterator = densityTable.listMapIterator();
			double totalEvacTime = 0;
			int waiters = 0;
			String message = "";

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				double evacTime = (Double) row.getEntry("evacTime");
				totalEvacTime += evacTime;
				waiters++;
				if (success && !attributes.isExpectFailure() && evacTime > attributes.getMaxEvacuationTime()) {
					success = false;

					message += "Maximum evacuation time exceeded (" + evacTime + " > "
							+ attributes.getMaxEvacuationTime() + ") by at least one ped.";
				}
				if (success && !attributes.isExpectFailure() && evacTime < attributes.getMinEvacuationTime()) {
					success = false;

					message += "Minimum evacuation time too low (" + evacTime + " < "
							+ attributes.getMinEvacuationTime() + ") by at least one ped.";
				}

				if (success == false && errorTime < 0) {
					errorTime = evacTime;
				}
			}

			if (totalEvacTime / waiters > attributes.getMaxEvacuationTimeMean() && !attributes.isExpectFailure()) {
				success = false;
				message += "Max-Mean evacuation time exceeded (" + totalEvacTime / waiters + " > "
						+ attributes.getMaxEvacuationTimeMean() + ").";
			} else if (totalEvacTime / waiters < attributes.getMinEvacuationTimeMean()
					&& !attributes.isExpectFailure()) {
				success = false;
				message += "Min-Mean evacuation time too low (" + totalEvacTime / waiters + " < "
						+ attributes.getMinEvacuationTimeMean() + ").";
			} else {
				if (success) {
					message += "Maximum evacuation time ok for all pedestrians.";
				} else if (attributes.isExpectFailure()) {
					message += "Maximum evacuation time NOT ok for all pedestrians, but that was expected.";
				}
			}

			table.addRow();
			table.addColumnEntry("evacuationTimeTest", message);

			if (success) {
				table.addRow();
				table.addColumnEntry("evacuationTimeTest", "SUCCESS");
			} else {
				if (errorTime >= 0) {
					table.addRow();
					table.addColumnEntry("evacuationTimeTest", errorTime);
				}
				table.addRow();
				table.addColumnEntry("evacuationTimeTest", "FAILURE");
			}
		}

		lastStep = state.getStep();

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
