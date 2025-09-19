package org.vadere.gui.topographycreator.control;

import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.util.config.VadereConfig;

import javax.swing.*;

/**
 * Each Action of the topographycreator gets the panelModel because each action change the
 * panelModel.
 * Actions are part of the controller of the mvc-pattern.
 * 
 * 
 */
public abstract class TopographyAction extends AbstractAction {
	private static final Resources RESOURCE = Resources.getInstance("global");
	private static final long serialVersionUID = 7643236418545161283L;
	private final IDrawPanelModel<?> panelModel;
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	public TopographyAction(final String name, final String iconPath, final IDrawPanelModel<?> panelModel) {
		super(name,new ImageIcon(Resources.class.getResource( iconPath)));
		this.panelModel = panelModel;
	}
	public TopographyAction(final String name, final String iconName, String shortDescription, final IDrawPanelModel<?> panelModel) {
		super(name, RESOURCE.getIconSVG(iconName, ICON_SIZE,ICON_SIZE));
		putValue(SHORT_DESCRIPTION, Localization.getString(shortDescription));
		this.panelModel = panelModel;
	}
	public TopographyAction(final String name, Icon icon, String shortDescription, final IDrawPanelModel<?> panelModel) {
		super(name, icon);
		putValue(SHORT_DESCRIPTION, Localization.getString(shortDescription));
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
