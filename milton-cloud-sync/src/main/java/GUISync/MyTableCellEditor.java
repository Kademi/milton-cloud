/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUISync;

/**
 *
 * @author ibraheem
 */
import java.awt.Component;
import javax.swing.AbstractCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class MyTableCellEditor extends AbstractCellEditor implements TableCellEditor {

    JComponent component = new JLabel();

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int rowIndex, int vColIndex) {
        ((JLabel) this.component).setText("" + value);

        return this.component;
    }

    public Object getCellEditorValue() {
        return ((JLabel) this.component).getText();
    }
}
