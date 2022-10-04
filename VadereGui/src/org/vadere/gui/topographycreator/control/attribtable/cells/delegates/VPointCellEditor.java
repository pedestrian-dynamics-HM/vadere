package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.util.geometry.shapes.VPoint;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;

public class VPointCellEditor extends AttributeEditor {

    private JSpinner xSpinner;
    private JSpinner ySpinner;
    private VPoint pointBuffer;

    @Override
    public java.util.List<Component> getInputComponent() {
        return Arrays.asList(xSpinner, ySpinner);
    }

    public VPointCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel,initialValue);
    }

    @Override
    protected void initialize(Object initialValue) {
        pointBuffer = new VPoint(0.0, 0.0);
        this.xSpinner = new JSpinner();
        this.ySpinner = new JSpinner();
        this.xSpinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.01));
        this.ySpinner.setModel(new SpinnerNumberModel(0.0, -1000.0, 1000.0, 0.01));
        this.xSpinner.setEditor(new JSpinner.NumberEditor(xSpinner, "0.0"));
        this.ySpinner.setEditor(new JSpinner.NumberEditor(ySpinner, "0.0"));
        JFormattedTextField xtxt = ((JSpinner.NumberEditor) xSpinner.getEditor()).getTextField();
        ((NumberFormatter) xtxt.getFormatter()).setAllowsInvalid(false);
        JFormattedTextField ytxt = ((JSpinner.NumberEditor) ySpinner.getEditor()).getTextField();
        ((NumberFormatter) ytxt.getFormatter()).setAllowsInvalid(false);
        if(initialValue!=null) {
            this.xSpinner.setValue(((VPoint) initialValue).getX());
            this.ySpinner.setValue(((VPoint) initialValue).getY());
        }
        var xContentPanel = new JPanel(new BorderLayout());
        var yContentPanel = new JPanel(new BorderLayout());
        var xLable = new JLabel("x");
        var yLable = new JLabel("y");
        var border = new EmptyBorder(0, 8, 0, 8);
        xLable.setBorder(border);
        yLable.setBorder(border);
        xContentPanel.add(xLable, BorderLayout.WEST);
        xContentPanel.add(xSpinner, BorderLayout.EAST);
        yContentPanel.add(yLable, BorderLayout.WEST);
        yContentPanel.add(ySpinner, BorderLayout.EAST);
        this.add(xContentPanel, BorderLayout.WEST);
        this.add(yContentPanel, BorderLayout.EAST);
        this.xSpinner.addChangeListener(e -> {
            pointBuffer = new VPoint((Double) xSpinner.getValue(), pointBuffer.getY());
            updateModel(pointBuffer);
        });
        this.ySpinner.addChangeListener(e -> {
            pointBuffer = new VPoint(pointBuffer.getX(), (Double) ySpinner.getValue());
            updateModel(pointBuffer);
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                super.componentMoved(e);
                resizeEditor(xContentPanel, yContentPanel, xLable, yLable);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
                resizeEditor(xContentPanel, yContentPanel, xLable, yLable);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                resizeEditor(xContentPanel, yContentPanel, xLable, yLable);

            }
        });

    }

    private void resizeEditor(JPanel xContentPanel, JPanel yContentPanel, JLabel xLable, JLabel yLabel) {
        var dim = new Dimension(getWidth() / 2, getHeight());
        xContentPanel.setPreferredSize(dim);
        yContentPanel.setPreferredSize(dim);
        //dim = new Dimension(getWidth()/4,getHeight());
        var spinnerWidth = xContentPanel.getWidth() - xLable.getWidth();
        dim = new Dimension(spinnerWidth, getHeight());
        xSpinner.setPreferredSize(dim);
        ySpinner.setPreferredSize(dim);

        //
        xLable.revalidate();
        yLabel.revalidate();
        xSpinner.revalidate();
        ySpinner.revalidate();
        xLable.repaint();
        yLabel.repaint();
        xSpinner.repaint();
        ySpinner.repaint();
        xContentPanel.revalidate();
        xContentPanel.repaint();
        yContentPanel.revalidate();
        yContentPanel.repaint();
        revalidate();
        repaint();
    }

    @Override
    protected void onModelChanged(Object object) {
        pointBuffer = (VPoint) object;
        this.xSpinner.setValue(pointBuffer.getX());
        this.ySpinner.setValue(pointBuffer.getY());

        xSpinner.revalidate();
        ySpinner.revalidate();
        xSpinner.repaint();
        ySpinner.repaint();
    }
}
