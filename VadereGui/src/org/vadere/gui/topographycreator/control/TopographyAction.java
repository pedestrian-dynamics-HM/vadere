package org.vadere.gui.topographycreator.control;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;

/**
 * Each Action of the topographycreator gets the panelModel because each action change the
 * panelModel.
 * Actions are part of the controller of the mvc-pattern.
 * 
 * 
 */
public abstract class TopographyAction extends AbstractAction {

	private static final long serialVersionUID = 7643236418545161283L;
	private final IDrawPanelModel<?> panelModel;

	public TopographyAction(final String name, final ImageIcon icon, final IDrawPanelModel<?> panelModel) {
		super(name, icon);
		this.panelModel = panelModel;
	}

	public TopographyAction(final String name, final IDrawPanelModel<?> panelModel) {
		super(name);
		this.panelModel = panelModel;
	}

	protected IDrawPanelModel<?> getScenarioPanelModel() {
		return panelModel;
	}
}
