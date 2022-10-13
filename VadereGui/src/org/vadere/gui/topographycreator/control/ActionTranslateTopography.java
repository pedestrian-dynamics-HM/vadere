package org.vadere.gui.topographycreator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.ActionTranslateTopographyDialog;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

/**
 * @author Benedikt Zoennchen
 */
public class ActionTranslateTopography extends TopographyAction {

	private final TopographyAction action;
	private final UndoableEditSupport undoableEditSupport;

	public ActionTranslateTopography(String name,
	                                 String iconPath,
									 String shortDescription,
	                                 @NotNull IDrawPanelModel<?> panelModel,
	                                 @NotNull TopographyAction action,
	                                 @NotNull final UndoableEditSupport undoSupport) {
		super(name, iconPath, shortDescription, panelModel);
		this.action = action;
		this.undoableEditSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		action.actionPerformed(e);

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		Rectangle2D.Double topographyBound = model.getTopographyBound();
		ActionTranslateTopographyDialog dialog = new ActionTranslateTopographyDialog(topographyBound.getMinX(), topographyBound.getMinY());

		if (dialog.getValue()){
			double xOld = topographyBound.getMinX();
			double yOld = topographyBound.getMinY();
			double x = dialog.getX();
			double y = dialog.getY();

			TopographyCreatorModel topographyCreatorModel = (TopographyCreatorModel) getScenarioPanelModel();
			topographyCreatorModel.translateTopography(x, y);

			VRectangle viewportBound = new VRectangle(model.getViewportBound()).translate(new VPoint(x - xOld, y - yOld));
			topographyCreatorModel.setViewportBound(viewportBound);
			undoableEditSupport.postEdit(new EditTranslateTopography(topographyCreatorModel, xOld, yOld, x, y));
		}
		getScenarioPanelModel().notifyObservers();
	}
}
