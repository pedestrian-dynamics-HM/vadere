package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.Attributes;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

import static org.vadere.gui.topographycreator.utils.Layouts.initGridBagConstraint;

public class AttributeTableContainer extends JPanel implements ISelectScenarioElementListener {
    AttributeTableView attrView;

    JTextPane helpView;

    public  AttributeTableContainer(final TopographyCreatorModel defaultModel){
        super(new GridBagLayout());
        attrView = new AttributeTableView(defaultModel);
        helpView = AttributeHelpView.getInstance();


        var minimalHelpViewSize = new Dimension(1,Toolkit.getDefaultToolkit().getScreenSize().height/10);

        helpView.setMinimumSize(minimalHelpViewSize);
        helpView.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));

        var gbcPage = initGridBagConstraint(1.0);
        var gbcHelp = initGridBagConstraint(0.2);

        this.add(attrView,gbcPage);
        this.add(helpView,gbcHelp);

        defaultModel.addSelectScenarioElementListener(this);
    }
    @Override
    public void selectionChange(ScenarioElement scenarioElement) {
        if(scenarioElement!= null){
            var fields = scenarioElement.getClass().getDeclaredFields();
            for(var field : fields){
                if(Attributes.class.isAssignableFrom(field.getType())){
                    attrView.selectionChange(scenarioElement,field);
                    return;
                }
            }
            throw new IllegalStateException("Every ScenarioElement should have only one field inheriting from Attributes");
        }
        attrView.selectionChange(null,null);
    }
}
