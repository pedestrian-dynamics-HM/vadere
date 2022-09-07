package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.control.ListMouseListener;
import org.vadere.gui.topographycreator.control.celleditor.ItemColumnEditor;
import org.vadere.gui.topographycreator.control.cellrenderer.ButtonColumnRenderer;
import org.vadere.gui.topographycreator.control.cellrenderer.EditorDelegate;
import org.vadere.gui.topographycreator.control.cellrenderer.ItemColumnRenderer;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ListCellEditor extends ChildObjectCellEditor {

    ArrayList<EditorDelegate> delegateList;
    JTable itemTable;
    DefaultTableModel itemModel;

    public ListCellEditor(JAttributeTable parentTranslator, Attributes fieldOwner, Field field, TopographyCreatorModel model, JPanel contentPanel) {
        super(parentTranslator, fieldOwner, field, model, contentPanel);
    }

    @Override
    protected void initialize() {
        this.itemTable = new JTable();
        this.itemModel = new DefaultTableModel();
        this.delegateList = new ArrayList<>();
    }

    @Override
    public void modelChanged(Object value) {
        updateInternalModel(value);
    }

    @Override
    protected void createInternalPropertyPane(Object newObject, TopographyCreatorModel model) {
        updateInternalModel(newObject);
    }

    @Override
    protected void constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        super.constructNewObject();
    }


    public void updateInternalModel(Object value) {
        this.itemModel = new DefaultTableModel();
        objectInstance = value;
        var classType = objectInstance.getClass().getGenericSuperclass().getClass();
        //get editortype from registry
        ((ArrayList) objectInstance).forEach(e -> {
            itemModel.addRow(listRow(e));
            delegateList.add(new EditorDelegate(itemTable, new JSpinner()));
        });
        itemModel.addRow(lastRow());
        delegateList.add(new EditorDelegate(itemTable));

        itemModel.setColumnIdentifiers(columnIdentifiers());
        itemTable.setModel(itemModel);
        itemTable.setRowHeight(26);
        itemTableSetupRenderer();
        itemTable.addMouseListener(new ListMouseListener(itemTable, itemModel, contentPanel));
        contentPanel.add(itemTable, BorderLayout.CENTER);
        parentTranslator.revalidate();
        parentTranslator.repaint();
    }

    public <T> Object[] listRow(T e) {
        return new Object[]{e, null};
    }

    public Object[] lastRow() {
        return new Object[]{null, null};
    }

    public Object[] columnIdentifiers() {
        return new Object[]{"list", "btn"};
    }

    public void itemTableSetupRenderer() {
        itemTable.getColumn("list").setCellEditor(new ItemColumnEditor(delegateList));
        itemTable.getColumn("list").setCellRenderer(new ItemColumnRenderer(delegateList));
        itemTable.getColumn("btn").setCellRenderer(new ButtonColumnRenderer());
        itemTable.getColumn("btn").setPreferredWidth(5);
        //itemTable.getColumn("btn").getCellEditor().
    }
}
