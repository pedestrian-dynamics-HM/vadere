package org.vadere.gui.postvisualization.control;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.vadere.gui.postvisualization.model.PostvisualizationModel;

public class ActionVisualization extends AbstractAction {
	protected final PostvisualizationModel model;

	public ActionVisualization(final String name, Icon icon, final PostvisualizationModel model) {
		super(name, icon);
		this.model = model;
	}

	public ActionVisualization(final String name, final PostvisualizationModel model) {
		this(name, null, model);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		model.notifyObservers();
	}
}
