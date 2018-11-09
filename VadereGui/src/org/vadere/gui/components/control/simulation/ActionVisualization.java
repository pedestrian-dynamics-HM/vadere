package org.vadere.gui.components.control.simulation;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionVisualization extends AbstractAction {
	protected final SimulationModel<? extends DefaultSimulationConfig> model;

	public ActionVisualization(final String name, Icon icon, final SimulationModel<? extends DefaultSimulationConfig> model) {
		super(name, icon);
		this.model = model;
	}

	public ActionVisualization(final String name, final SimulationModel<? extends DefaultSimulationConfig> model) {
		this(name, null, model);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		model.notifyObservers();
	}
}
