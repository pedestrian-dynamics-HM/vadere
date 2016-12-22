package org.vadere.gui.projectview.view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.simulator.projects.Scenario;

import java.awt.*;

public class ScenarioTableRenderer extends DefaultTableCellRenderer {

	private ProjectViewModel model;

	public ScenarioTableRenderer(ProjectViewModel model) {
		super();
		this.model = model;
	}

	@Override
	public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected,
			final boolean hasFocus, final int row, final int column) {
		if (column == 0)
			super.getTableCellRendererComponent(table, ((Scenario) value).getDisplayName(), isSelected,
					hasFocus, row, column);
		else
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		return this;
	}

}
