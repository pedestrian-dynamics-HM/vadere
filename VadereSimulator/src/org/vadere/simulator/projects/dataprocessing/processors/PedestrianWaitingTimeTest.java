package org.vadere.simulator.projects.dataprocessing.processors;

import com.google.gson.annotations.Expose;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

import org.vadere.simulator.control.SimulationState;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.processors.AttributesWaitingTimeTest;
import org.vadere.util.data.Row;
import org.vadere.util.data.Table;

public class PedestrianWaitingTimeTest extends AbstractProcessor implements ModelTest {

	private PedestrianWaitingTimeProcessor pedestrianWaitingTimeProcessor;
	private AttributesWaitingTimeTest attributes;

	@Expose
	private final Table table;

	@Expose
	private int lastStep;

	@Expose
	private Set<String> allSupportedColumns;

	@Expose
	private boolean success;

	@Expose
	private double errorTime;

	public PedestrianWaitingTimeTest(final PedestrianWaitingTimeProcessor pedestrianWaitingTimeProcessor,
			final AttributesWaitingTimeTest attributes) {
		super(new Table("waitingTimeTest"));
		this.pedestrianWaitingTimeProcessor = pedestrianWaitingTimeProcessor;
		this.pedestrianWaitingTimeProcessor.addColumnNames(pedestrianWaitingTimeProcessor.getAllColumnNames());
		this.attributes = attributes;
		this.table = getTable();
		this.success = true;
		this.errorTime = -1;

		allSupportedColumns = new HashSet<>();
		allSupportedColumns.addAll(Arrays.asList(getTable().getColumnNames()));
	}

	public PedestrianWaitingTimeTest(final PedestrianWaitingTimeProcessor pedestrianWaitingTimeProcessor) {
		this(pedestrianWaitingTimeProcessor, new AttributesWaitingTimeTest());
	}

	public PedestrianWaitingTimeTest() {
		this(new PedestrianWaitingTimeProcessor());
	}

	@Override
	public String[] getAllColumnNames() {
		return allSupportedColumns.toArray(new String[] {});
	}

	@Override
	public Table preLoop(final SimulationState state) {
		pedestrianWaitingTimeProcessor.preLoop(state);
		this.success = true;
		this.lastStep = state.getStep();
		return super.preLoop(state);
	}

	@Override
	public Table postUpdate(SimulationState state) {
		pedestrianWaitingTimeProcessor.postUpdate(state);
		return super.postUpdate(state);
	}

	@Override
	public Table postLoop(final SimulationState state) {
		Table densityTable = pedestrianWaitingTimeProcessor.postLoop(state);

		String failReason = "";

		if (table.isEmpty() || lastStep != state.getStep()) {
			table.clear();
			ListIterator<Row> rowIterator = densityTable.listMapIterator();
			double totalWaitingTime = 0;
			int waiters = 0;

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				double waitingTime = (Double) row.getEntry("totalWaitingTime");
				totalWaitingTime += waitingTime;
				waiters++;
				if (waitingTime > attributes.getMaxWaitingTime()) {
					success = false;
					failReason = "maximum time exceeded";
				}
				if (waitingTime < attributes.getMinWaitingTime()) {
					success = false;
					failReason = "lower than minimum time";
				}
				if (success == false && errorTime < 0) {
					errorTime = waitingTime;
				}
			}

			if (totalWaitingTime / waiters > attributes.getMaxWaitingTimeMean()) {
				success = false;
				failReason = "mean time exceeded";
			}
			if (totalWaitingTime / waiters < attributes.getMinWaitingTimeMean()) {
				success = false;
				failReason = "lower than minimum mean time";
			}

			table.addRow();

			if (attributes.isExpectFailure() && success) {
				success = false;
				table.addColumnEntry("waitingTimeTest",
						"Failure: Waiting time ok even though it was expected not to be.");
			} else if (attributes.isExpectFailure() && !success) {
				success = true;
				table.addColumnEntry("waitingTimeTest", "Waiting time not ok, as expected. Reason: " + failReason);
			} else {
				if (success) {
					table.addColumnEntry("waitingTimeTest", "Maximum waiting time ok.");
				} else {
					table.addColumnEntry("waitingTimeTest", "Waiting time limits exceeded. Reason: " + failReason);
				}
			}

			table.addRow();
			if (success) {
				table.addColumnEntry("waitingTimeTest", "SUCCESS");
			} else {
				if (errorTime >= 0) {
					table.addColumnEntry("waitingTimeTest", errorTime);
					table.addRow();
				}
				table.addColumnEntry("waitingTimeTest", "FAILURE");
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
