package carPark;

import javax.swing.table.AbstractTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class CustomTableModel extends AbstractTableModel {
    private final List<Object[]> data;
    private final String[] columnNames;
    
    public CustomTableModel(ResultSet rs) throws Exception {
        this.data = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Заглавия на колони
        columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnLabel(i + 1);
        }
        
        // Данни
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            data.add(row);
        }
    }
    
    @Override
    public int getRowCount() { return data.size(); }
    
    @Override
    public int getColumnCount() { return columnNames.length; }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex)[columnIndex];
    }
    
    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }
}
