package org.vadere.gui.postvisualization.view;

import javax.swing.*;

import org.vadere.gui.components.control.IMode;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.view.ScaleablePanel;

import java.util.Observable;
import java.util.Observer;

/**
 * The panel which will draw the simulation steps.
 * 
 */
public class ScenarioPanel extends ScaleablePanel implements Observer {

	private static Resources resources = Resources.getInstance("postvisualization");
	private static final long serialVersionUID = 3772313433958735043L;
	private PostvisualizationRenderer renderer;

	/** a reference copy of the selection mode of the panelModel. */
	private IMode selectionMode = null;

	public ScenarioPanel(final PostvisualizationRenderer renderer, final JScrollPane scoScrollPane) {
		super(renderer.getModel(), renderer, scoScrollPane);
		this.renderer = renderer;
	}

	private void setMouseSelectionMode(final IMode selectionMode) {
		if (selectionMode != null && !selectionMode.equals(this.selectionMode)) {
			removeMouseListener(this.selectionMode);
			removeMouseMotionListener(this.selectionMode);
			removeMouseWheelListener(this.selectionMode);

			addMouseListener(selectionMode);
			addMouseMotionListener(selectionMode);
			addMouseWheelListener(selectionMode);
			this.selectionMode = selectionMode;
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		setMouseSelectionMode(renderer.getModel().getMouseSelectionMode());
		SwingUtilities.invokeLater(() -> repaint());
	}
}
