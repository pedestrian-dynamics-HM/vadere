package org.vadere.gui.topographycreator.control;


import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.ActionTranslateTopographyDialog;

import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;

/**
 * @author Benedikt Zoennchen
 */
public class ActionTranslateElements extends TopographyAction {

	private final TopographyAction action;
	private final UndoableEditSupport undoableEditSupport;

	public ActionTranslateElements(final String name,
	                               final String iconPath,
								   String shortDescription,
	                               @NotNull final IDrawPanelModel<?> panelModel,
	                               @NotNull final TopographyAction action,
	                               @NotNull final UndoableEditSupport undoSupport) {
		super(name, iconPath, shortDescription, panelModel);
		this.action = action;
		this.undoableEditSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		action.actionPerformed(e);

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		ActionTranslateTopographyDialog dialog = new ActionTranslateTopographyDialog(0, 0);

		if (dialog.getValue()){
			double x = dialog.getX();
			double y = dialog.getY();

			TopographyCreatorModel topographyCreatorModel = (TopographyCreatorModel) getScenarioPanelModel();
			topographyCreatorModel.translateElements(x, y);
			undoableEditSupport.postEdit(new EditTranslateElements(topographyCreatorModel, x, y));
		}
		getScenarioPanelModel().notifyObservers();
	}
}
