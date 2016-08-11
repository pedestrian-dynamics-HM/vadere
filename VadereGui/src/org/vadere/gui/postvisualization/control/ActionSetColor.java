package org.vadere.gui.postvisualization.control;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JColorChooser;
import javax.swing.JPanel;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public abstract class ActionSetColor extends ActionVisualization {
	private JPanel coloredPanel;

	public ActionSetColor(final String name, final PostvisualizationModel model, final JPanel coloredPanel) {
		super(name, model);
		this.coloredPanel = coloredPanel;
	}

	@Override
	public void actionPerformed(final ActionEvent event) {
		Color color = JColorChooser.showDialog(null, "Choose Color", coloredPanel.getBackground());
		if (color != null) {
			coloredPanel.setBackground(color);
			saveColor(color);
			model.notifyObservers();
		}
		super.actionPerformed(event);
	}

	protected abstract void saveColor(final Color color);

}
