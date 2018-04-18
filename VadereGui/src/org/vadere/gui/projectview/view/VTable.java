package org.vadere.gui.projectview.view;

import org.apache.commons.lang3.ArrayUtils;
import org.vadere.gui.projectview.control.ActionSeeDiscardChanges;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.VadereTableModelSorted;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class VTable<D> extends JTable {

	private static final long serialVersionUID = 1L;

	public ProjectViewModel projectViewModel;

	public VTable(final VadereTableModelSorted<D> model) {
		super(model);
	}

	public void setDeleteAction(AbstractAction deleteAction) {
		getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "delete");
		getActionMap().put("delete", deleteAction);
	}

	public void setProjectViewModel(ProjectViewModel projectViewModel) { // TODO [priority=low] [task=refactoring] breaking mvc pattern?
		this.projectViewModel = projectViewModel;
	}

	public void setPopupMenus(final JPopupMenu popupMenu, final JPopupMenu multiselectionPopup) {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (VTable.this.isEnabled() && SwingUtilities.isRightMouseButton(event)) {
					int row = VTable.this.rowAtPoint(event.getPoint());

					if (!ArrayUtils.contains(VTable.this.getSelectedRows(), row)) { // If the right-click did not happen on one of the selected rows -> select only this row before proceeding with right-click action. This is the behavior how one would expect it (windows explorer does it that way), otherwise accidental deletions could happen
						VTable.this.setRowSelectionInterval(row, row);
					}

					if (projectViewModel != null) { // only the scenarioTable has a projectViewModel, not the outputTable
						if (projectViewModel.selectedScenariosContainChangedOnes(VTable.this.getSelectedRows())) {
							setDiffRelatedMenuItemsEnabled(popupMenu, true);
							setDiffRelatedMenuItemsEnabled(multiselectionPopup, true);
						} else {
							setDiffRelatedMenuItemsEnabled(popupMenu, false);
							setDiffRelatedMenuItemsEnabled(multiselectionPopup, false);
						}
					}

					if (VTable.this.getSelectedRowCount() > 1) {
						multiselectionPopup.show(VTable.this, event.getX(), event.getY());
					} else {
						popupMenu.show(VTable.this, event.getX(), event.getY());
					}
				}
			}
		});
	}

	private void setDiffRelatedMenuItemsEnabled(JPopupMenu popup, boolean enabled) {
		for (Component comp : popup.getComponents())
			if (((JMenuItem) comp).getAction().getClass() == ActionSeeDiscardChanges.class)
				comp.setEnabled(enabled);
	}

	@Override
	public VadereTableModelSorted<D> getModel() {
		return (VadereTableModelSorted<D> )super.getModel();
	}
}
