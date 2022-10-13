package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTableView;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VCircle;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VShapeCellEditor extends AttributeEditor implements ViewListener {

    private static Resources resources = Resources.getInstance("global");
    private final JToggleButton rectButton;
    private final JToggleButton polyButton;
    private final JToggleButton circButton;
    private AttributeTableView view;

    private GridBagConstraints gbc;

    @Override
    public List<Component> getInputComponent() {
        return Collections.emptyList();
    }

    public VShapeCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel, initialValue);
        initializeGridBagConstraint();
        view = new AttributeTableView(this);
        view.buildPageFor(JacksonObjectMapper.VRectangleStore.class);
        view.buildPageFor(JacksonObjectMapper.VCircleStore.class);
        view.buildPageFor(JacksonObjectMapper.VPolygon2DStore.class);


        var toolBar = new JToolBar();
        var color = UIManager.getColor("Component.borderColor");
        var border = new LineBorder(color,1);
        toolBar.setBorder(border);
        var group = new ButtonGroup();
        rectButton = new JToggleButton(resources.getIcon("vshape_rect.png",16,16));
        rectButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                var rect = new VRectangle();
                onModelChanged(rect);
                try {
                    model.getValueNode().setValue(rect);
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        polyButton = new JToggleButton(resources.getIcon("vshape_poly.png",16,16));

        polyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                var poly = new VPolygon();
                onModelChanged(poly);
                try {
                    model.getValueNode().setValue(poly);
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        circButton = new JToggleButton(resources.getIcon("vshape_circ.png",16,16));

        circButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                var circ = new VCircle();
                onModelChanged(circ);
                try {
                    model.getValueNode().setValue(circ);
                } catch (NoSuchFieldException ex) {
                    throw new RuntimeException(ex);
                } catch (IllegalAccessException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        toolBar.add(rectButton);
        toolBar.add(polyButton);
        toolBar.add(circButton);
        group.add(rectButton);
        group.add(polyButton);
        group.add(circButton);
        this.add(toolBar);
        contentPanel.add(view, gbc);
    }

    @Override
    protected void initialize(Object initialValue) {

    }

    @Override
    protected void onModelChanged(Object object) {
        Object wrapper = null;
       if(object.getClass().isAssignableFrom(VRectangle.class)){
           wrapper = new JacksonObjectMapper.VRectangleStore((VRectangle)object);
           rectButton.setSelected(true);
       }
       else if(object.getClass().isAssignableFrom(VCircle.class)){
           wrapper = new JacksonObjectMapper.VCircleStore((VCircle)object);
           circButton.setSelected(true);
       }
       else if(object.getClass().isAssignableFrom(VPolygon.class)){
           wrapper = new JacksonObjectMapper.VPolygon2DStore((VPolygon) object);
           polyButton.setSelected(true);

       }else {
           throw new RuntimeException(object+ " not editable with VShapeCellEditor.");
        }
        view.selectionChange(wrapper);
        try {
            view.updateModel(wrapper);
        } catch (TreeException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(1, 1, 1, 1);
    }

    @Override
    public void viewChanged(Object object) {
        try {
            model.getValueNode().setValue(((JacksonObjectMapper.VShapeStore)object).newInstance());
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
