package org.vadere.gui.topographycreator.view;

import org.vadere.gui.topographycreator.control.TopographyAction;
import org.vadere.gui.topographycreator.model.IDrawPanelModel;
import org.vadere.state.scenario.ScenarioElement;

import javax.swing.*;
import javax.swing.undo.UndoableEditSupport;
import java.awt.event.ActionEvent;

public class ActionSelectAllElements extends TopographyAction {

    private final UndoableEditSupport undoSupport;

    public ActionSelectAllElements(String name, ImageIcon icon, IDrawPanelModel panelModel,
                                 UndoableEditSupport undoSupport) {
        super(name, icon, panelModel);
        this.undoSupport = undoSupport;
    }

    public ActionSelectAllElements(String name, IDrawPanelModel panelModel, UndoableEditSupport undoSupport) {
        super(name, panelModel);
        this.undoSupport = undoSupport;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        getScenarioPanelModel().getObstacles().forEach(this::addIfNotContained);
        getScenarioPanelModel().getMeasurementAreas().forEach(this::addIfNotContained);
        System.out.println(getScenarioPanelModel().getSelectedElements().size());
        getScenarioPanelModel().showPrototypeShape();
    }

    private void addIfNotContained(ScenarioElement element) {
        if(!getScenarioPanelModel().getSelectedElements().contains(element)) {
            getScenarioPanelModel().getSelectedElements().add(element);
        }
    }
}
