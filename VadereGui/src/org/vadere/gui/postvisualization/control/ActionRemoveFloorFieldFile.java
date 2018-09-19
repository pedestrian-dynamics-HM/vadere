package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionRemoveFloorFieldFile extends ActionVisualization {

	private PostvisualizationModel model;

	public ActionRemoveFloorFieldFile(String name, PostvisualizationModel model) {
		super(name, model);
		this.model = model;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		model.setPotentialFieldContainer(null);
		model.notifyObservers();
	}
}
