/**
 * 
 */
package lyl.MyGIS;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.swing.table.FeatureCollectionTableModel;

/**
 * @author
 *
 */
@SuppressWarnings("serial")
public class EditableModel extends AbstractTableModel{

	private String[] columnNames;
	private Object[][] data;

	public EditableModel(FeatureCollectionTableModel fctModel) {
		
		int row = fctModel.getRowCount();
		int col = fctModel.getColumnCount();
		columnNames = new String[col];
		for (int c = 0; c < col; c++) {
			columnNames[c] = fctModel.getColumnName(c);
		}
		
		data = new Object[row][col];
		for (int r = 0; r < row; r++) {
			for (int c = 0; c<col;c++) {
				data[r][c] = fctModel.getValueAt(r, c);
			}
		}
		
		
	}

	public int getIdCol() {
		int idCol = findColumn("FeatureIdentifer");
		return idCol;
	}

	public boolean isCellEditable(int row, int col) {

		if (col == getIdCol()) {
			return false;
		} else {
			return true;
		}
	}

	public int getColumnCount() {
		return columnNames.length;
	}

	public int getRowCount() {
		return data.length;
	}

	public String getColumnName(int col) {
		return columnNames[col];
	}

	public Object getValueAt(int row, int col) {
		return data[row][col];
	}

	public Class getColumnClass(int c) {
		return getValueAt(0, c).getClass();
	}



	/*
	 * Don't need to implement this method unless your table's data can change.
	 */
	public void setValueAt(Object value, int row, int col) {
		data[row][col] = value;
		fireTableCellUpdated(row, col);
	}

}
