package org.vadere.gui.topographycreator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.Obstacle;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * @author Benedikt Zoennchen
 */
public class EditMergeObstacles extends AbstractUndoableEdit {

	private final IDrawPanelModel panelModel;
	private final List<Obstacle> beforeObstacleList;
	private final List<Obstacle> afterObstacleList;

	public EditMergeObstacles(final IDrawPanelModel panelModel,
	                          @NotNull final List<Obstacle> beforeObstacleList,
	                          @NotNull final List<Obstacle> afterObstacleList) {
		this.panelModel = panelModel;
		this.beforeObstacleList = beforeObstacleList;
		this.afterObstacleList = afterObstacleList;
	}

	@Override
	public void undo() throws CannotUndoException {
		panelModel.removeObstacleIf(obstacle -> true);
		beforeObstacleList.stream().forEach(obstacle -> panelModel.addShape(obstacle));
		panelModel.notifyObservers();
	}

	@Override
	public void redo() throws CannotRedoException {
		panelModel.removeObstacleIf(obstacle -> true);
		afterObstacleList.stream().forEach(obstacle -> panelModel.addShape(obstacle));
		panelModel.notifyObservers();
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public boolean canRedo() {
		return true;
	}

	@Override
	public String getPresentationName() {
		return "cup obstacles";
	}
}
