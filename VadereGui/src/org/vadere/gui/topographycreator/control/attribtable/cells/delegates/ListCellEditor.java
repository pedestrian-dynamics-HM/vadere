package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.control.attribtable.JCollapsablePanel;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.ItemColumnEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.ButtonColumnRenderer;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.ItemColumnRenderer;
import org.vadere.gui.topographycreator.control.attribtable.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

public class ListCellEditor extends ChildObjectCellEditor {
    private final HashMap<String, Constructor<? extends AttributeEditor>> editorConstructors;

    private final HashMap<String, JComponent> editorInstances;
    ArrayList<EditorDelegate> delegateList;
    JTable itemTable;
    DefaultTableModel itemModel;

    public ListCellEditor(JAttributeTable parentTranslator, Object fieldOwner, Field field, TopographyCreatorModel model, JPanel contentPanel) {
        super(parentTranslator, fieldOwner, field, model, contentPanel);
        this.editorConstructors = new HashMap<>();
        this.editorInstances = new HashMap<>();
        addTypeEditor(String.class, TextEditCellEditor.class);
        addTypeEditor(Integer.class, SpinnerCellEditor.class);
        addTypeEditor(Double.class, DoubleSpinnerCellEditor.class);
        addTypeEditor(Boolean.class, CheckBoxCellEditor.class);
        addTypeEditor(ArrayList.class, ListCellEditor.class);
    }

    private static AttributeTableModel initializeNewTableModel() {
        return new AttributeTableModel();
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

    @Override
    protected void initialize() {

        this.itemTable = new JTable();
        this.itemModel = new DefaultTableModel();
        this.delegateList = new ArrayList<>();
        this.itemTable.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                itemTable.getColumnModel().getColumn(0).setPreferredWidth(itemTable.getWidth() - 26);
                itemTable.getColumnModel().getColumn(1).setPreferredWidth(26);
            }
        });
        this.contentPanel.add(new JCollapsablePanel("ArrayList", JCollapsablePanel.Style.HEADER));
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

    public void updateInternalModel(Object value) {
        this.contentPanel.removeAll();
        this.itemModel = new DefaultTableModel();
        //this.itemTable = new JTable();

        this.delegateList.clear();
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

        itemTable.addMouseListener(new MouseAdapter() {


            @Override
            public void mouseClicked(MouseEvent e) {
                int column = itemTable.getColumnModel().getColumnIndexAtX(e.getX()); // get the coloum of the button
                int row = e.getY() / itemTable.getRowHeight(); //get the row of the button
                System.out.println(column + " " + row);

                if (column == 1) {
                    if (row < itemModel.getRowCount() - 1) {
                        ((ArrayList<?>) objectInstance).remove(row);
                    } else {
                        ((ArrayList<Integer>) objectInstance).add(0);
                    }
                    updateModel(objectInstance);
                    updateInternalModel(objectInstance);
                    contentPanel.revalidate();
                    contentPanel.repaint();
                }
            }
        });
        contentPanel.add(itemTable, BorderLayout.CENTER);
        parentTranslator.revalidate();
        parentTranslator.repaint();
    }

    public void itemTableSetupRenderer() {
        itemTable.getColumn("list").setCellEditor(new ItemColumnEditor(delegateList));
        itemTable.getColumn("list").setCellRenderer(new ItemColumnRenderer(delegateList));
        itemTable.getColumn("btn").setCellRenderer(new ButtonColumnRenderer());
        itemTable.getColumn("list").setPreferredWidth(100);
        itemTable.getColumn("btn").setPreferredWidth(5);

        //itemTable.getColumn("btn").getCellEditor().
    }

    public void addTypeEditor(Class type, Class<? extends AttributeEditor> clazz) {
        if (!this.editorConstructors.containsKey(type)) {
            Constructor<? extends AttributeEditor> constructor = null;
            try {
                constructor = clazz.getDeclaredConstructor(JAttributeTable.class, Object.class, Field.class, TopographyCreatorModel.class, JPanel.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(type.getName(), constructor);
        }
    }

    public void updateInternal(ArrayList<?> fieldModel, TopographyCreatorModel topoModel) {
        var activeTable = initializeNewTableSection();
        var activeModel = initializeNewTableModel();
        var type = fieldModel.getClass().getGenericSuperclass().getClass();
        var typeName = type.getName();

        Constructor constructor = null;
        try {
            if (!this.editorConstructors.containsKey(typeName)) {
                if (type.isEnum())
                    constructor = ComboBoxCellEditor.class.getDeclaredConstructor(JAttributeTable.class, Object.class, Field.class, TopographyCreatorModel.class, JPanel.class);
                else if (Modifier.isAbstract(type.getModifiers()))
                    constructor = AbstractTypeCellEditor.class.getDeclaredConstructor(JAttributeTable.class, Object.class, Field.class, TopographyCreatorModel.class, JPanel.class);
                else
                    constructor = ChildObjectCellEditor.class.getDeclaredConstructor(JAttributeTable.class, Object.class, Field.class, TopographyCreatorModel.class, JPanel.class);
            } else {
                constructor = this.editorConstructors.get(type.getName());
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        for (int row = 0; row < fieldModel.size(); row++) {

            JPanel subPanel = new JPanel(new GridBagLayout());
            subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());

            AttributeEditor component = null;
            try {
                component = (AttributeEditor) constructor.newInstance(this, fieldOwner, field, topoModel, subPanel);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            this.editorInstances.put(field.getName(), component);
            if (type.isEnum()) {
                ((JComboBox) this.editorInstances.get(field.getName())).setModel(new DefaultComboBoxModel(type.getEnumConstants()));
            }
            //create new Table if subPanel got populated
            activeModel.addRow(field);
            if (subPanel.getComponentCount() > 0) {
                activeTable.setModel(activeModel);
                contentPanel.add(activeTable);
                contentPanel.add(subPanel);
                activeTable = initializeNewTableSection();
                activeModel = initializeNewTableModel();
            }
        }
        if (activeModel.getRowCount() > 0) {
            activeTable.setModel(activeModel);
            contentPanel.add(activeTable);
        }
    }

    private JTable initializeNewTableSection() {
        var activeTable = new JTable();
        activeTable.setRowHeight(28);
        activeTable.setIntercellSpacing(new Dimension(0, 4));
        activeTable.setBackground(UIManager.getColor("Panel.background"));
        return activeTable;
    }
}
