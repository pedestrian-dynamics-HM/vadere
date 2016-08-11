package org.vadere.gui.postvisualization.control;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.control.DefaultSelectionMode;
import org.vadere.gui.components.control.DrawVoronoiDiagramMode;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ActionSwapSelectionMode extends ActionVisualization {

	private static Logger logger = LogManager.getLogger(ActionSwapSelectionMode.class);

	public ActionSwapSelectionMode(final String name, final PostvisualizationModel model) {
		super(name, model);
	}

	public ActionSwapSelectionMode(final String name, Icon icon, final PostvisualizationModel model) {
		super(name, icon, model);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (model.getMouseSelectionMode() instanceof DrawVoronoiDiagramMode) {
			model.setMouseSelectionMode(new DefaultSelectionMode(model));
		} else {
			model.setMouseSelectionMode(new DrawVoronoiDiagramMode(model));
		}
		super.actionPerformed(e);
	}

}
