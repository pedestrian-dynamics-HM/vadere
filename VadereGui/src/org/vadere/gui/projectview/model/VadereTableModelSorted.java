package org.vadere.gui.projectview.model;

import javax.swing.table.DefaultTableModel;

import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.data.SortedList;

import java.util.Comparator;

public abstract class VadereTableModelSorted<T> extends DefaultTableModel {

	private static final long serialVersionUID = 1L;

	private SortedList<T> rows;

	public VadereTableModelSorted(final Object[] columnNames, final int rowCount, Comparator<T> comparator) {
		super(columnNames, rowCount);
		this.rows = new SortedList<>(comparator);
	}

	public synchronized void init(final VadereProject project) {
		this.rows.clear();
	}

	public synchronized boolean replace(final T oldValue, final T newValue) {
		if (remove(oldValue)) {
			insertValue(newValue);
			return true;
		}
		return false;
	}

	public synchronized boolean remove(final T value) {
		int i = indexOfRow(value);
		if (i == -1) {
			return false;
		}
		safeRemove(i);
		fireTableDataChanged();
		return true;
	}

	public synchronized T getValue(final int rowIndex) {
		return rows.get(rowIndex);
	}

	public synchronized int indexOfRow(final T value) {
		return rows.indexOf(value);
	}

	public abstract void insertRow(int row, T value);

	@Override
	public void removeRow(int row) {
		safeRemove(row);
	}

	public void removeRows(final int[] rows) {
		for(int i=0; i<rows.length; i++){
			removeRow(rows[i]-i);
		}
	}

	public synchronized void insertValue(final T value) {
		int index = rows.findPrecessor(value);
		rows.add(value);
		insertRow(index, value);
		fireTableDataChanged();
	}

	private void safeRemove(final int row) {
		rows.remove(row);
		super.removeRow(row);
	}
}
