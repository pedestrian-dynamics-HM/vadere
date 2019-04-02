package org.vadere.gui.topographycreator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.MeasurementArea;
import org.vadere.state.scenario.Obstacle;

import java.util.List;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class EditSubtractMeasurementArea extends AbstractUndoableEdit {

    private final IDrawPanelModel panelModel;
    private final List<MeasurementArea> beforeMeasurementAreaList;
    private final List<MeasurementArea> afterMeasurementAreaList;

    public EditSubtractMeasurementArea(final IDrawPanelModel panelModel,
                              @NotNull final List<MeasurementArea> beforeMeasurementAreaList,
                              @NotNull final List<MeasurementArea> afterMeasurementAreaList) {
        this.panelModel = panelModel;
        this.beforeMeasurementAreaList = beforeMeasurementAreaList;
        this.afterMeasurementAreaList = afterMeasurementAreaList;
    }

    @Override
    public void undo() throws CannotUndoException {
        panelModel.removeMeasurementAreaIf(predicate -> true); //remove all
        beforeMeasurementAreaList.forEach(panelModel::addShape);
        panelModel.notifyObservers();
    }

    @Override
    public boolean canUndo() {
        return true;
    }

    @Override
    public void redo() throws CannotRedoException {
        panelModel.removeMeasurementAreaIf(predicate -> true); //remove all
        afterMeasurementAreaList.forEach(panelModel::addShape);
        panelModel.notifyObservers();
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public String getPresentationName() {
        return "substract obstacles from measurementarea";
    }
}
