package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public class ReportsPanel extends JPanel {
    private final Connection conn;
    private JTable table;
    private JComboBox<String> ownerCombo;
    private JComboBox<String> carCombo;
    private JTextField dateFromField;
    private JTextField dateToField;
    private JTextField minCostField;
    private JTextField maxCostField;
    private Map<String, Integer> ownerNameToIdMap = new HashMap<>();
    private Map<String, Integer> carInfoToIdMap = new HashMap<>();
    
    public ReportsPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout());
        initUI();
        refreshOwnerCombo();
        refreshCarCombo();
    }
    
    private void initUI() {
        // Панел с критерии за търсене
        JPanel criteriaPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        
        criteriaPanel.add(new JLabel("Собственик:"));
        ownerCombo = new JComboBox<>();
        ownerCombo.addItem("Всички"); // Базова стойност
        criteriaPanel.add(ownerCombo);
        
        criteriaPanel.add(new JLabel("Автомобил:"));
        carCombo = new JComboBox<>();
        carCombo.addItem("Всички");
        criteriaPanel.add(carCombo);
        
        criteriaPanel.add(new JLabel("От дата (гггг-мм-дд):"));
        dateFromField = new JTextField();
        criteriaPanel.add(dateFromField);
        
        criteriaPanel.add(new JLabel("До дата (гггг-мм-дд):"));
        dateToField = new JTextField();
        criteriaPanel.add(dateToField);
        
        criteriaPanel.add(new JLabel("Минимална стойност:"));
        minCostField = new JTextField();
        criteriaPanel.add(minCostField);
        
        criteriaPanel.add(new JLabel("Максимална стойност:"));
        maxCostField = new JTextField();
        criteriaPanel.add(maxCostField);
        
        // Бутон за търсене
        JButton searchButton = new JButton("Търси");
        searchButton.addActionListener(this::searchRepairs);
        
        // Таблица с резултати
        table = new JTable();
        
        // Подреждане на компонентите
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(criteriaPanel, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.SOUTH);
        
        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
    
    public void refreshOwnerCombo() {
        ownerCombo.removeAllItems();
        ownerCombo.addItem("Всички");
        ownerNameToIdMap.clear();
        
        try {
            String query = "SELECT OwnerID, Name FROM Owners ORDER BY Name";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String name = rs.getString("Name");
                ownerCombo.addItem(name);
                ownerNameToIdMap.put(name, rs.getInt("OwnerID"));
            }
        } catch (SQLException e) {
            showError("Грешка при зареждане на собствениците", e);
        }
    }
    
    public void refreshCarCombo() {
        carCombo.removeAllItems();
        carCombo.addItem("Всички");
        
        try {
            String query = "SELECT CarID, Brand, Model, RegistrationNumber FROM Cars ORDER BY Brand, Model";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                String carInfo = rs.getString("Brand") + " " + rs.getString("Model") + 
                               " (" + rs.getString("RegistrationNumber") + ")";
                carCombo.addItem(carInfo);
                carInfoToIdMap.put(carInfo, rs.getInt("CarID"));
            }
        } catch (SQLException e) {
            showError("Грешка при зареждане на автомобилите", e);
        }
    }
    
    private boolean validateDate(String dateStr) {
        if (dateStr.isEmpty()) return true;
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    private boolean validateCost(String costStr) {
        if (costStr.isEmpty()) return true;
        try {
            new BigDecimal(costStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private void searchRepairs(ActionEvent e) {
        try {
            // Валидация на входните данни
            if (!validateDate(dateFromField.getText())) {
                showError("Невалиден формат на начална дата (очакван формат: гггг-мм-дд)", null);
                return;
            }
            
            if (!validateDate(dateToField.getText())) {
                showError("Невалиден формат на крайна дата (очакван формат: гггг-мм-дд)", null);
                return;
            }
            
            if (!validateCost(minCostField.getText())) {
                showError("Невалидна минимална стойност (очаква се число)", null);
                return;
            }
            
            if (!validateCost(maxCostField.getText())) {
                showError("Невалидна максимална стойност (очаква се число)", null);
                return;
            }
            
            // Подготвяне на SQL заявка с параметри
            StringBuilder query = new StringBuilder(
                "SELECT r.RepairID, o.Name AS Owner, " +
                "c.Brand + ' ' + c.Model + ' (' + c.RegistrationNumber + ')' AS Car, " +
                "r.Description, r.Cost, CONVERT(VARCHAR, r.Date, 23) AS Date " +
                "FROM Repairs r " +
                "JOIN Cars c ON r.CarID = c.CarID " +
                "JOIN Owners o ON c.OwnerID = o.OwnerID " +
                "WHERE 1=1");
            
            List<Object> parameters = new ArrayList<>();
            
            // Филтър по собственик (само ако не е избрано "Всички")
            if (ownerCombo.getSelectedIndex() > 0) {
                String selectedOwner = (String) ownerCombo.getSelectedItem();
                int ownerId = ownerNameToIdMap.get(selectedOwner);
                query.append(" AND o.OwnerID = ?");
                parameters.add(ownerId);
            }
            
            // Филтър по автомобил (само ако не е избрано "Всички")
            if (carCombo.getSelectedIndex() > 0) {
                String selectedCar = (String) carCombo.getSelectedItem();
                int carId = carInfoToIdMap.get(selectedCar);
                query.append(" AND c.CarID = ?");
                parameters.add(carId);
            }
            
            // Филтър по дата (само ако е попълнено)
            if (!dateFromField.getText().isEmpty()) {
                query.append(" AND r.Date >= ?");
                parameters.add(java.sql.Date.valueOf(dateFromField.getText().trim()));
            }
            
            if (!dateToField.getText().isEmpty()) {
                query.append(" AND r.Date <= ?");
                parameters.add(java.sql.Date.valueOf(dateToField.getText().trim()));
            }
            
            // Филтър по стойност (само ако е попълнено)
            if (!minCostField.getText().isEmpty()) {
                query.append(" AND r.Cost >= ?");
                parameters.add(new BigDecimal(minCostField.getText().trim()));
            }
            
            if (!maxCostField.getText().isEmpty()) {
                query.append(" AND r.Cost <= ?");
                parameters.add(new BigDecimal(maxCostField.getText().trim()));
            }
            
            query.append(" ORDER BY r.Date DESC");
            
            // Изпълнение на заявката
            PreparedStatement pstmt = conn.prepareStatement(query.toString());
            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            table.setModel(new CustomTableModel(rs));
            
        } catch (Exception ex) {
            showError("Грешка при търсене на ремонти", ex);
        }
    }
    
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, 
            message + (e != null ? ": " + e.getMessage() : ""), 
            "Грешка", JOptionPane.ERROR_MESSAGE);
        if (e != null) {
            e.printStackTrace();
        }
    }
}