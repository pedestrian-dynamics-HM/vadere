package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.Attributes;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

import static org.vadere.gui.topographycreator.utils.Layouts.initGridBagConstraint;

public class AttributeTableContainer extends JPanel implements ISelectScenarioElementListener, Observer,AttributeTranslator {
    AttributeTableView attrView;
    JTextPane helpView;
    private final NotifyContext ctx = new NotifyContext(this.getClass());

    ScenarioElement selectedElement;

    TopographyCreatorModel panelModel;


    public  AttributeTableContainer(final TopographyCreatorModel defaultModel){
        super(new GridBagLayout());
        attrView = new AttributeTableView(this,defaultModel);
        helpView = AttributeHelpView.getInstance();
        this.panelModel = defaultModel;

        var minimalHelpViewSize = new Dimension(1,Toolkit.getDefaultToolkit().getScreenSize().height/10);

        helpView.setMinimumSize(minimalHelpViewSize);
        helpView.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));

        var gbcPage = initGridBagConstraint(1.0);
        var gbcHelp = initGridBagConstraint(0.2);

        this.add(attrView,gbcPage);
        this.add(helpView,gbcHelp);

        defaultModel.addSelectScenarioElementListener(this);
        defaultModel.addObserver(this);
    }
    @Override
    public void selectionChange(ScenarioElement scenarioElement) {
        this.selectedElement = scenarioElement;
        if(scenarioElement==null)
            attrView.selectionChange(null);
        else
            attrView.selectionChange(scenarioElement.getAttributes());
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof NotifyContext) {
            var ctx = (NotifyContext) arg;
            if (!this.getClass().isAssignableFrom(ctx.getNotifyContext())) {
                if (panelModel.getSelectedElement() != null)
                    attrView.updateView(panelModel.getSelectedElement().getAttributes());
                else
                    attrView.updateView(null);
            }
        }
    }

    public void updateModel(Attributes attributes){
        selectedElement.setAttributes(attributes);
        panelModel.getScenario().updateCurrentStateSerialized();
        panelModel.setElementHasChanged(selectedElement);
        panelModel.notifyObservers(ctx);
        System.out.println("Updated: "+panelModel.getSelectedElement().getAttributes());
    }

}
