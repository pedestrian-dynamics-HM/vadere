package org.vadere.gui.components.control.simulation;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;

public abstract class ActionSetColor extends ActionVisualization {
	private JPanel coloredPanel;

	public ActionSetColor(final String name, final SimulationModel<? extends DefaultSimulationConfig> model, final JPanel coloredPanel) {
		super(name, model);
		this.coloredPanel = coloredPanel;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		Color color = JColorChooser.showDialog(coloredPanel.getParent(), "Choose Color", coloredPanel.getBackground());
		if (color != null) {
			coloredPanel.setBackground(color);
			saveColor(color);
			model.notifyObservers();
		}
		super.actionPerformed(event);
	}

	protected abstract void saveColor(final Color color);

}
