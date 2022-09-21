package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.reflections.Reflections;
import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.AttributesScenarioElement;

import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Observable;
import java.util.Observer;

import static org.vadere.gui.topographycreator.control.attribtable.util.Layouts.initGridBagConstraint;

public class AttributeTableContainer extends JPanel implements ISelectScenarioElementListener, Observer, Revalidatable {
    AttributeTableView attrView;
    JTextPane helpView;
    private final NotifyContext ctx = new NotifyContext(this.getClass());

    ScenarioElement selectedElement;
    TopographyCreatorModel panelModel;


    public AttributeTableContainer(final TopographyCreatorModel defaultModel) {
        super(new GridBagLayout());
        attrView = new AttributeTableView(this);
        helpView = AttributeHelpView.getInstance();

        this.panelModel = defaultModel;

        var minimalHelpViewSize = new Dimension(1, Toolkit.getDefaultToolkit().getScreenSize().height / 10);

        helpView.setMinimumSize(minimalHelpViewSize);
        helpView.setBorder(new LineBorder(UIManager.getColor("Component.borderColor")));

        helpView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                helpView.setPreferredSize(new Dimension(100, 100));
            }
        });

        var gbcPage = initGridBagConstraint(1.0);
        var gbcHelp = initGridBagConstraint(0.0);

        this.add(new JScrollPane(attrView), gbcPage);
        this.add(helpView, gbcHelp);

        var registeredClasses = new Reflections("org.vadere")
                .getSubTypesOf(AttributesScenarioElement.class);
        for(var clazz : registeredClasses){
            attrView.buildPageFor(clazz);
        }



        defaultModel.addSelectScenarioElementListener(this);
        defaultModel.addObserver(this);
    }

    private static boolean isAnElementSelected(ScenarioElement scenarioElement) {
        return scenarioElement != null;
    }

    private static boolean isNoElementSelected(ScenarioElement scenarioElement) {
        return scenarioElement == null;
    }

    @Override
    public void selectionChange(ScenarioElement scenarioElement) {
        this.selectedElement = scenarioElement;
        if (isNoElementSelected(scenarioElement))
            attrView.clear();
        if (isAnElementSelected(scenarioElement)) {
            var attributes = scenarioElement.getAttributes();
            attrView.selectionChange(attributes);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof NotifyContext) {
            var ctx = (NotifyContext) arg;
            if (AttributeTableContainer.class.isAssignableFrom(ctx.getNotifyContext())) {
                return;
            }
        }
        try {
            if (panelModel.getSelectedElement() != null)
                attrView.updateModel(panelModel.getSelectedElement().getAttributes());
        } catch (TreeException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void revalidateObjectStructure(Object object) {
        var element = panelModel.getSelectedElement();
        element.setAttributes((Attributes) object);
        panelModel.setElementHasChanged(element);
        panelModel.notifyObservers(ctx);
    }
}
