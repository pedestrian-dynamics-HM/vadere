package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.ActionResizeTopographyBoundDialog;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;

/**
 * @author Benedikt Zoennchen
 */
public class ActionResizeTopographyBound extends TopographyAction {

	private final TopographyAction action;
	private final UndoableEditSupport undoableEditSupport;

	public ActionResizeTopographyBound(String name, String iconPath,String shortDescription, IDrawPanelModel<?> panelModel,
									   TopographyAction action, final UndoableEditSupport undoSupport) {
		super(name,iconPath, shortDescription, panelModel);
		this.action = action;
		this.undoableEditSupport = undoSupport;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//set to Selection to to be sure no accidental changes are introduced
		action.actionPerformed(e);

		TopographyCreatorModel model = (TopographyCreatorModel) getScenarioPanelModel();
		ActionResizeTopographyBoundDialog dialog = new ActionResizeTopographyBoundDialog(model.getTopographyBound());

		if (dialog.getValue()){
			VRectangle oldBound = new VRectangle(model.getTopographyBound());
			VRectangle newBound = new VRectangle(dialog.getBound());
			model.setTopographyBound(newBound);
			undoableEditSupport.postEdit(new EditResizeTopographyBound(getScenarioPanelModel(), oldBound, newBound));
		}
		getScenarioPanelModel().notifyObservers();
	}
}
