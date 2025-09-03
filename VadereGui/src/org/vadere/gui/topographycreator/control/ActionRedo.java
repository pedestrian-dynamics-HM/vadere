package org.vadere.gui.topographycreator.control;


import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Action: redo the last action.
 * 
 * 
 */
public class ActionRedo extends AbstractAction {
	private static final long serialVersionUID = 4975524648404524891L;
	private final UndoManager undoManager;
	private final TopographyAction action;
	private static final Logger logger = Logger.getLogger(ActionRedo.class);
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

	public ActionRedo(final UndoManager undoManager,final TopographyAction action) {
		super("redo",Resources.getInstance("global").getIconSVG(ResourceStrings.ICONS_REDO_ICON_PNG,ICON_SIZE,ICON_SIZE));
		putValue(SHORT_DESCRIPTION, Localization.getString(ResourceStrings.TOPOGRAPHY_CREATOR_BTN_REDO_TOOLTIP));
		this.undoManager = undoManager;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			undoManager.redo();
		} catch (CannotRedoException e) {
			logger.debug("Cannot redo! List of edits is empty!");
			Toolkit.getDefaultToolkit().beep();
		}
		action.actionPerformed(arg0);
	}
}
