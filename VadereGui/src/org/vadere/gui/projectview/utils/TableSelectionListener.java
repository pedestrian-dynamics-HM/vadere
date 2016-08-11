package org.vadere.gui.projectview.utils;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class TableSelectionListener implements ListSelectionListener {
	private JTable associatedTable;

	public TableSelectionListener(JTable associatedTable) {
		this.associatedTable = associatedTable;
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (!tableSelectionValueIsAdjustingOrNothingSelected(e)) {
			onSelect(e);
		}
	}

	public abstract void onSelect(ListSelectionEvent e);

	private boolean tableSelectionValueIsAdjustingOrNothingSelected(ListSelectionEvent e) {
		return e.getValueIsAdjusting() || associatedTable.getSelectedRow() == -1;
	}

}
