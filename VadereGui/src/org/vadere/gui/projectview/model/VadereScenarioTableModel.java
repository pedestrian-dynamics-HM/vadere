package org.vadere.gui.projectview.model;


import org.vadere.gui.components.utils.Messages;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.logging.Logger;

/**
 * The table panelModel used in the table that displays
 * {@link org.vadere.simulator.projects.Scenario}s.
 *
 */
public class VadereScenarioTableModel extends VadereTableModelSorted<VadereScenarioTableModel.VadereDisplay> {

	public static class VadereDisplay {
		public final Scenario scenarioRM;
		public final VadereState state;

		public VadereDisplay(final Scenario scenarioRM, final VadereState state) {
			this.scenarioRM = scenarioRM;
			this.state = state;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			VadereDisplay that = (VadereDisplay) o;

			return scenarioRM != null ? scenarioRM.equals(that.scenarioRM) : that.scenarioRM == null;
		}

		@Override
		public int hashCode() {
			return scenarioRM != null ? scenarioRM.hashCode() : 0;
		}
	}

	private static Logger logger = Logger.getLogger(VadereScenarioTableModel.class);
	private static final long serialVersionUID = 1145206652000839654L;

	VadereScenarioTableModel() {
		super(new String[] {
				Messages.getString("ProjectView.testTable.test.text"),
				Messages.getString("ProjectView.testTable.state.text")}, 0,
				(v1, v2) -> v1.scenarioRM.getName().compareTo(v2.scenarioRM.getName()));
	}

	@Override
	public synchronized void init(final VadereProject project) {
		super.init(project);
		setColumnCount(2);

		// remove all rows
		setRowCount(0);

		project.getScenarios()
				.forEach(scenario -> insertValue(new VadereDisplay(scenario, VadereState.INITIALIZED)));
	}

	public synchronized boolean replace(final Scenario oldValue, final VadereDisplay newValue) {
		if (remove(oldValue)) {
			insertValue(newValue);
			fireTableDataChanged();
			return true;
		}
		return false;
	}

	@Override
	public synchronized int indexOfRow(final VadereDisplay value) {
		return indexOfRow(value.scenarioRM);
	}

	public synchronized int indexOfRow(final Scenario value) {
		for (int i = 0; i < getRowCount(); i++)
			if (((Scenario) getValueAt(i, 0)).getName().equals(value.getName()))
				return i;
		throw new RuntimeException("Can't find row in scenarioTable corresponding to scenario " + value.getName()); // if we'd return -1 as before, this throws in-thread errors that are difficult to debug because of the lack of a stacktrace!
	}

	public synchronized boolean remove(final Scenario value) {
		return super.remove(new VadereDisplay(value, VadereState.INITIALIZED));
	}

	@Override
	public void insertRow(int row, VadereDisplay value) {
		insertRow(row, new Object[] {
				value.scenarioRM,
				value.state.toString()
		});
	}

	/**
	 * Cells are not directly editable.
	 */
	@Override
	public synchronized boolean isCellEditable(final int row, final int col) {
		return false;
	}
}
