package org.vadere.gui.projectview.model;

import java.io.File;
import java.util.List;

import org.vadere.gui.components.utils.Messages;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.IOOutput;

public class OutputFileTableModel extends VadereTableModelSorted<File> {

	private static final long serialVersionUID = 134253986682827818L;

	OutputFileTableModel() {
		super(new String[] {Messages.getString("ProjectView.OutputTable.label")}, 0, (f1, f2) -> f1.getName().compareTo(f2.getName()));
	}

	@Override
	public void init(final VadereProject project) {
		super.init(project);
		List<File> outputFileNames = project.getProjectOutput().getAllOutputDirs();
		setColumnCount(1);
		setRowCount(0);
		outputFileNames.forEach(p -> insertValue(p));
		fireTableDataChanged();
	}

	@Override
	public void insertRow(final int row, final File value) {
		insertRow(row, new Object[] {value});
	}

	@Override
	public synchronized boolean isCellEditable(final int row, final int col) {
		return false;
	}
}
