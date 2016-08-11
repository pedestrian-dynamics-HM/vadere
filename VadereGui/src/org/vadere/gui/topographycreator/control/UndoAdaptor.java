package org.vadere.gui.topographycreator.control;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * Part of the command-pattern (undo/redo-logic)
 * 
 * 
 */
public class UndoAdaptor implements UndoableEditListener {
	private final UndoManager undoManager;

	public UndoAdaptor(UndoManager undoManager) {
		this.undoManager = undoManager;
	}

	@Override
	public void undoableEditHappened(UndoableEditEvent evt) {
		UndoableEdit edit = evt.getEdit();
		undoManager.addEdit(edit);
	}
}
