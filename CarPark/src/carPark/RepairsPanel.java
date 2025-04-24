package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;

public class RepairsPanel extends JPanel {
    private final Connection conn;
    private JTable table;
    private JComboBox<String> carCombo;
    private JTextArea descriptionArea;
    private JTextField costField;
    private JTextField dateField;
    private int selectedRepairId = -1;
    private Map<String, Integer> carInfoToIdMap = new HashMap<>();
    
    public RepairsPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout());
        initUI();
        refreshTable();
        refreshCarCombo();
    }
    
    private void initUI() {
        // Панел за формата с падинг
        JPanel formWrapper = new JPanel(new BorderLayout());
        formWrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ред 1 - Автомобил
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Автомобил*:"), gbc);
        gbc.gridx = 1;
        carCombo = new JComboBox<>();
        carCombo.addItem("Избери"); // Базова стойност
        formPanel.add(carCombo, gbc);

        // Ред 2 - Описание
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Описание:"), gbc);
        gbc.gridx = 1;
        descriptionArea = new JTextArea(3, 20);
        formPanel.add(new JScrollPane(descriptionArea), gbc);

        // Ред 3 - Стойност
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Стойност*:"), gbc);
        gbc.gridx = 1;
        costField = new JTextField();
        costField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (text.isEmpty()) return true; // ще се валидира при submit
                try {
                    new BigDecimal(text);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        formPanel.add(costField, gbc);

        // Ред 4 - Дата
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Дата* (гггг-мм-дд):"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        dateField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (text.isEmpty()) return true; // ще се валидира при submit
                try {
                    LocalDate date = LocalDate.parse(text);
                    return !date.isAfter(LocalDate.now());
                } catch (DateTimeParseException e) {
                    return false;
                }
            }
        });
        formPanel.add(dateField, gbc);

        formWrapper.add(formPanel, BorderLayout.CENTER);

        // Панел с бутони
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(createButton("Добави", this::addRepair));
        buttonPanel.add(createButton("Обнови", this::updateRepair));
        buttonPanel.add(createButton("Изтрий", this::deleteRepair));
        buttonPanel.add(createButton("Изчисти", e -> clearForm()));
        formWrapper.add(buttonPanel, BorderLayout.SOUTH);

        // Таблица с ремонти
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                CustomTableModel model = (CustomTableModel) table.getModel();
                selectedRepairId = model.getOwnerIdAt(table.getSelectedRow()); // Взимаме repairId от скритата колона
                
                descriptionArea.setText(table.getValueAt(table.getSelectedRow(), 1).toString());
                costField.setText(table.getValueAt(table.getSelectedRow(), 2).toString());
                dateField.setText(table.getValueAt(table.getSelectedRow(), 3).toString());

                String carInfo = table.getValueAt(table.getSelectedRow(), 0).toString();
                carCombo.setSelectedItem(carInfo);
            }
        });

        setLayout(new BorderLayout(10, 10));
        add(formWrapper, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private boolean validateRepairData() {
        // Валидация на автомобил
        if (carCombo.getSelectedIndex() <= 0) { // 0 е "Избери"
            JOptionPane.showMessageDialog(this, "Моля, изберете автомобил!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            carCombo.requestFocus();
            return false;
        }

        // Валидация на стойност
        if (costField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Стойността е задължителна!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            costField.requestFocus();
            return false;
        }
        
        try {
            BigDecimal cost = new BigDecimal(costField.getText().trim());
            if (cost.compareTo(BigDecimal.ZERO) < 0) {
                JOptionPane.showMessageDialog(this, "Стойността не може да бъде отрицателна!", 
                    "Грешка", JOptionPane.ERROR_MESSAGE);
                costField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Моля, въведете валидна стойност!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            costField.requestFocus();
            return false;
        }

        // Валидация на дата
        if (dateField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Датата е задължителна!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            dateField.requestFocus();
            return false;
        }
        
        try {
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            if (date.isAfter(LocalDate.now())) {
                JOptionPane.showMessageDialog(this, "Датата не може да бъде в бъдещето!", 
                    "Грешка", JOptionPane.ERROR_MESSAGE);
                dateField.requestFocus();
                return false;
            }
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Моля, въведете валидна дата (гггг-мм-дд)!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            dateField.requestFocus();
            return false;
        }

        return true;
    }
    
    private JButton createButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }
    
    private void refreshTable() {
        try {
            String query = "SELECT r.RepairID, c.Brand + ' ' + c.Model + ' (' + c.RegistrationNumber + ')' AS Car, " +
                         "r.Description, r.Cost, CONVERT(VARCHAR, r.Date, 23) AS Date " +
                         "FROM Repairs r JOIN Cars c ON r.CarID = c.CarID " +
                         "ORDER BY r.Date DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            table.setModel(new CustomTableModel(rs));
        } catch (Exception e) {
            showError("Грешка при зареждане на ремонтите", e);
        }
    }
    
    public void refreshCarCombo() {
        carCombo.removeAllItems();
        carCombo.addItem("Избери"); // Базова стойност
        try {
            String query = "SELECT CarID, Brand, Model, RegistrationNumber FROM Cars ORDER BY Brand, Model";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                carCombo.addItem(rs.getString("Brand") + " " + 
                                 rs.getString("Model") + " (" + 
                                 rs.getString("RegistrationNumber") + ")");
                // Запазваме съответствието между текст и carId
                carInfoToIdMap.put(rs.getString("Brand") + " " + rs.getString("Model") + 
                                 " (" + rs.getString("RegistrationNumber") + ")", 
                                 rs.getInt("CarID"));
            }
        } catch (SQLException e) {
            showError("Грешка при зареждане на автомобилите", e);
        }
    }
    
    private void addRepair(ActionEvent e) {
        try {
            if (!validateRepairData()) {
                return;
            }
            
            String query = "INSERT INTO Repairs (CarID, Description, Cost, Date) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            
            String selectedCar = (String) carCombo.getSelectedItem();
            int carId = carInfoToIdMap.get(selectedCar);
            pstmt.setInt(1, carId);
            pstmt.setString(2, descriptionArea.getText().trim());
            pstmt.setBigDecimal(3, new BigDecimal(costField.getText().trim()));
            pstmt.setDate(4, java.sql.Date.valueOf(dateField.getText().trim()));
            
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
        } catch (Exception ex) {
            showError("Грешка при добавяне на ремонт", ex);
        }
    }

    private void updateRepair(ActionEvent e) {
        if (selectedRepairId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете ремонт за редактиране", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            if (!validateRepairData()) {
                return;
            }
            
            String query = "UPDATE Repairs SET CarID=?, Description=?, Cost=?, Date=? WHERE RepairID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            
            String selectedCar = (String) carCombo.getSelectedItem();
            int carId = carInfoToIdMap.get(selectedCar);
            pstmt.setInt(1, carId);
            pstmt.setString(2, descriptionArea.getText().trim());
            pstmt.setBigDecimal(3, new BigDecimal(costField.getText().trim()));
            pstmt.setDate(4, java.sql.Date.valueOf(dateField.getText().trim()));
            pstmt.setInt(5, selectedRepairId);
            
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
        } catch (Exception ex) {
            showError("Грешка при обновяване на ремонт", ex);
        }
    }
    
    private void deleteRepair(ActionEvent e) {
        if (selectedRepairId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете ремонт за изтриване", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Сигурни ли сте, че искате да изтриете този ремонт?", 
            "Потвърждение", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String query = "DELETE FROM Repairs WHERE RepairID=?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, selectedRepairId);
                pstmt.executeUpdate();
                refreshTable();
                clearForm();
            } catch (SQLException ex) {
                showError("Грешка при изтриване на ремонт", ex);
            }
        }
    }
    
    private void clearForm() {
        descriptionArea.setText("");
        costField.setText("");
        dateField.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        if (carCombo.getItemCount() > 0) {
            carCombo.setSelectedIndex(0); // Връщане към "Избери"
        }
        selectedRepairId = -1;
    }
    
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, 
            message + ": " + e.getMessage(), 
            "Грешка", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}