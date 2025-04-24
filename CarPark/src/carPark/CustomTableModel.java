package carPark;

import javax.swing.table.AbstractTableModel;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class CustomTableModel extends AbstractTableModel {
    private final List<Object[]> data;
    private final String[] columnNames;
    private final List<Integer> ownerIds;  // Списък за съхранение на ID-тата

    public CustomTableModel(ResultSet rs) throws Exception {
        this.data = new ArrayList<>();
        this.ownerIds = new ArrayList<>();  // Инициализираме списъка за ID-та
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // Заглавия на колони (без ID)
        columnNames = new String[columnCount - 1];  
        for (int i = 2; i <= columnCount; i++) {  // Започваме от 2, за да пропуснем първата колона (ID)
            columnNames[i - 2] = metaData.getColumnLabel(i);
        }

        // Данни
        while (rs.next()) {
            // Запазваме ID-то в отделен списък
            ownerIds.add(rs.getInt(1));
            
            Object[] row = new Object[columnCount - 1];  // Намаляваме броя на колоните с 1
            for (int i = 2; i <= columnCount; i++) {  // Започваме от 2, за да пропуснем първата колона (ID)
                row[i - 2] = rs.getObject(i);
            }
            data.add(row);
        }
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data.get(rowIndex)[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    // Метод за получаване на ID на собственика от таблицата
    public int getOwnerIdAt(int rowIndex) {
        return ownerIds.get(rowIndex);  // Връщаме ID от списъка
    }
}