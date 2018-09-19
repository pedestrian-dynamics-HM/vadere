package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import javax.swing.Icon;

import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionPlay extends ActionVisualization {

	private final PostvisualizationModel model;

	public ActionPlay(final String name, final Icon icon, final PostvisualizationModel model) {
		super(name, icon, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Player.getInstance(model).start();
		/*
		 * synchronized (model) {
		 * model.notifyAll();
		 * }
		 */
	}
}
