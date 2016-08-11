package org.vadere.gui.topographycreator.control;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.undo.UndoManager;

/**
 * Action: redo the last action.
 * 
 * 
 */
public class ActionRedo extends AbstractAction {
	private static final long serialVersionUID = 4975524648404524891L;
	private final UndoManager undoManager;
	private final TopographyAction action;

	public ActionRedo(final String name, final ImageIcon icon, final UndoManager undoManager,
			final TopographyAction action) {
		super(name, icon);
		this.undoManager = undoManager;
		this.action = action;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		undoManager.redo();
		action.actionPerformed(arg0);
	}
}
