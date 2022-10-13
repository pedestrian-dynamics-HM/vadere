package org.vadere.gui.components.view;

import javax.swing.*;

public class ScenarioToolBarSection {

    private final Action[] actions;

    public ScenarioToolBarSection(Action...actions){
        this.actions = actions;
    }

    public Action[] getActions() {
        return actions;
    }
}
