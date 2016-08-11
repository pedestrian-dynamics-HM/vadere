package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.undo.UndoManager;

/**
 * Action: Undo the last action.
 * 
 * 
 */
public class ActionUndo extends AbstractAction {

	private static final long serialVersionUID = 6022031098257929748L;
	private final UndoManager undoManager;
	private final TopographyAction action;

	public ActionUndo(final String name, final ImageIcon icon, UndoManager undoManager, final TopographyAction action) {
		super(name, icon);
		this.undoManager = undoManager;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		undoManager.undo();
		action.actionPerformed(arg0);
	}

}
