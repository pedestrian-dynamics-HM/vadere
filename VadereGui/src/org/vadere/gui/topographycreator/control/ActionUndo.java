package org.vadere.gui.topographycreator.control;


import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

import static org.vadere.gui.components.utils.ResourceStrings.ICONS_UNDO_ICON_PNG;

/**
 * Action: Undo the last action.
 * 
 * 
 */
public class ActionUndo extends AbstractAction {

	private static final long serialVersionUID = 6022031098257929748L;
	private final UndoManager undoManager;
	private final TopographyAction action;
	private static final Logger logger = Logger.getLogger(ActionUndo.class);
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	public ActionUndo(UndoManager undoManager, final TopographyAction action) {
		super("undo",Resources.getInstance("global").getIconSVG(ICONS_UNDO_ICON_PNG,ICON_SIZE,ICON_SIZE));
		putValue(SHORT_DESCRIPTION, Localization.getString(ResourceStrings.TOPOGRAPHY_CREATOR_BTN_UNDO_TOOLTIP));
		this.undoManager = undoManager;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			undoManager.undo();
		} catch (CannotUndoException e) {
			logger.debug("Cannot undo! List of edits is empty!");
			Toolkit.getDefaultToolkit().beep();
		}

		action.actionPerformed(arg0);
	}

}
