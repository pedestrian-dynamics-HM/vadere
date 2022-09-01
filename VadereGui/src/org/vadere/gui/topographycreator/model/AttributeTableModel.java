package org.vadere.gui.topographycreator.model;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Vector;

/**
 * AttributeTableModel is needed for using JTable components
 */
public class AttributeTableModel implements TableModel {

    private DefaultTableModel model;
    public static final String PropertyString = "Properties";
    public static final String ValuesString = "Values";
    public static final int PropertiesIndex = 0;
    public static final int ValuesIndex = 1;



    public AttributeTableModel(){
        this.model = new DefaultTableModel();
        this.model.addColumn(PropertyString);
        this.model.addColumn(ValuesString);
    }

    public AttributeTableModel(List<Field> modelData){
        this.model = new DefaultTableModel();
        this.model.addColumn(PropertyString);
        this.model.addColumn(ValuesString);
        this.addRows(new Vector<>(modelData));
    }

    @Override
    public int getRowCount() {
        return this.model.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == PropertiesIndex){
            return PropertyString;
        }
        if (columnIndex == ValuesIndex){
            return ValuesString;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return this.model.getColumnClass(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // only the values column is supposed to be editable
        if (columnIndex > PropertiesIndex) {
            return this.model.isCellEditable(rowIndex, columnIndex);
        }
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return this.model.getValueAt(rowIndex,columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != ValuesIndex){
            throw new UnsupportedOperationException();
        }
        this.model.setValueAt(aValue,rowIndex,columnIndex);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        this.model.addTableModelListener(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        this.model.removeTableModelListener(l);
    }

    public void addRows(Vector<Field> rowData){
        for ( var elem : rowData) {
            this.model.addRow(new Object[]{elem, elem.getType()});
        }
    }

    public void addRow(Field row){
        this.model.addRow(new Object[]{row, row.getType()});
    }
}
