package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.state.scenario.ScenarioElement;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

public class AttributePage extends JScrollPane implements ISelectScenarioElementListener, Observer {
   private Observable observable;
    public AttributePage(JComponent component,Object object){
        super(component);
        this.observable = new Observable();

        var rootComponent = (JPanel)component.getComponent(0);

        for(var comp : rootComponent.getComponents()){
            if ( comp instanceof Observer){
                observable.addObserver((Observer) comp);
            }
        }
    }
    @Override
    public void selectionChange(ScenarioElement scenarioElement) {

    }

    @Override
    public void update(Observable o, Object arg) {
        System.out.println("page");
        observable.notifyObservers(arg);
    }
}
