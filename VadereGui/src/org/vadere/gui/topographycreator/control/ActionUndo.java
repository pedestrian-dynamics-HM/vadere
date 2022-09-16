package org.vadere.gui.topographycreator.control;


import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.logging.Logger;

import javax.swing.*;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

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

	public ActionUndo(final String name, final String iconPath,String shortDescription, UndoManager undoManager, final TopographyAction action) {
		super(name,new ImageIcon(Resources.class.getResource(iconPath)));
		putValue(SHORT_DESCRIPTION, Messages.getString(shortDescription));
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
