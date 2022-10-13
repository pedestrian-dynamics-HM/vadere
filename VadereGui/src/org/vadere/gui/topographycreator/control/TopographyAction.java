package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;

import javax.swing.*;

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

	public TopographyAction(final String name, final String iconPath, final IDrawPanelModel<?> panelModel) {
		super(name,new ImageIcon(Resources.class.getResource( iconPath)));
		this.panelModel = panelModel;
	}
	public TopographyAction(final String name, final String iconPath, String shortDescription, final IDrawPanelModel<?> panelModel) {
		super(name, new ImageIcon(Resources.class.getResource(iconPath)));
		putValue(SHORT_DESCRIPTION, Messages.getString(shortDescription));
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
