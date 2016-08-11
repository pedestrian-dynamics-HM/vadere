package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionRemoveFloorFieldFile extends ActionVisualization {

	public ActionRemoveFloorFieldFile(String name, PostvisualizationModel model) {
		super(name, model);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		model.setPotentialFieldContainer(null);
		model.notifyObservers();
	}
}
