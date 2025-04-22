package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;

public class CarsPanel extends JPanel {
    private final Connection conn;
    private JTable table;
    private JTextField regNumberField, brandField, modelField, yearField;
    private JComboBox<String> ownerCombo;
    private int selectedCarId = -1;
    
    public CarsPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout());
        initUI();
        refreshTable();
        refreshOwnersCombo();
    }
    
    private void initUI() {
        // Форма за въвеждане
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        
        formPanel.add(new JLabel("Регистрационен номер:"));
        regNumberField = new JTextField();
        formPanel.add(regNumberField);
        
        formPanel.add(new JLabel("Марка:"));
        brandField = new JTextField();
        formPanel.add(brandField);
        
        formPanel.add(new JLabel("Модел:"));
        modelField = new JTextField();
        formPanel.add(modelField);
        
        formPanel.add(new JLabel("Година:"));
        yearField = new JTextField();
        formPanel.add(yearField);
        
        formPanel.add(new JLabel("Собственик:"));
        ownerCombo = new JComboBox<>();
        formPanel.add(ownerCombo);
        
        // Бутони
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton("Добави", this::addCar));
        buttonPanel.add(createButton("Обнови", this::updateCar));
        buttonPanel.add(createButton("Изтрий", this::deleteCar));
        buttonPanel.add(createButton("Изчисти", e -> clearForm()));
        
        // Таблица
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedCarId = (Integer) table.getValueAt(row, 0);
                    regNumberField.setText(table.getValueAt(row, 1).toString());
                    brandField.setText(table.getValueAt(row, 2).toString());
                    modelField.setText(table.getValueAt(row, 3).toString());
                    yearField.setText(table.getValueAt(row, 4).toString());
                    
                    // Намиране на собственика в комбо бокса
                    String ownerInfo = table.getValueAt(row, 5).toString();
                    for (int i = 0; i < ownerCombo.getItemCount(); i++) {
                        if (ownerCombo.getItemAt(i).contains(ownerInfo)) {
                            ownerCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });
        
        add(formPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.CENTER);
        add(new JScrollPane(table), BorderLayout.SOUTH);
    }
    
    private JButton createButton(String text, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        return button;
    }
    
    private void refreshTable() {
        try {
            String query = "SELECT c.CarID, c.RegistrationNumber, c.Brand, c.Model, c.Year, o.Name AS Owner " +
                          "FROM Cars c LEFT JOIN Owners o ON c.OwnerID = o.OwnerID";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            table.setModel(new CustomTableModel(rs));
        } catch (Exception e) {
            showError("Грешка при зареждане на автомобилите", e);
        }
    }
    
    public void refreshOwnersCombo() {
        ownerCombo.removeAllItems();
        try {
            String query = "SELECT OwnerID, Name FROM Owners";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                ownerCombo.addItem(rs.getInt("OwnerID") + " - " + rs.getString("Name"));
            }
        } catch (SQLException e) {
            showError("Грешка при зареждане на собствениците", e);
        }
    }
    
    private void addCar(ActionEvent e) {
        try {
            String query = "INSERT INTO Cars (RegistrationNumber, Brand, Model, Year, OwnerID) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, regNumberField.getText().trim());
            pstmt.setString(2, brandField.getText().trim());
            pstmt.setString(3, modelField.getText().trim());
            pstmt.setInt(4, Integer.parseInt(yearField.getText().trim()));
            
            String selectedOwner = (String) ownerCombo.getSelectedItem();
            int ownerId = Integer.parseInt(selectedOwner.split(" - ")[0]);
            pstmt.setInt(5, ownerId);
            
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
        } catch (Exception ex) {
            showError("Грешка при добавяне на автомобил", ex);
        }
    }
    
    private void updateCar(ActionEvent e) {
        if (selectedCarId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете автомобил за редактиране", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            String query = "UPDATE Cars SET RegistrationNumber=?, Brand=?, Model=?, Year=?, OwnerID=? WHERE CarID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, regNumberField.getText().trim());
            pstmt.setString(2, brandField.getText().trim());
            pstmt.setString(3, modelField.getText().trim());
            pstmt.setInt(4, Integer.parseInt(yearField.getText().trim()));
            
            String selectedOwner = (String) ownerCombo.getSelectedItem();
            int ownerId = Integer.parseInt(selectedOwner.split(" - ")[0]);
            pstmt.setInt(5, ownerId);
            
            pstmt.setInt(6, selectedCarId);
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
        } catch (Exception ex) {
            showError("Грешка при обновяване на автомобил", ex);
        }
    }
    
    private void deleteCar(ActionEvent e) {
        if (selectedCarId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете автомобил за изтриване", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Сигурни ли сте, че искате да изтриете този автомобил?", 
            "Потвърждение", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String query = "DELETE FROM Cars WHERE CarID=?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, selectedCarId);
                pstmt.executeUpdate();
                refreshTable();
                clearForm();
            } catch (SQLException ex) {
                showError("Грешка при изтриване на автомобил", ex);
            }
        }
    }
    
    private void clearForm() {
        regNumberField.setText("");
        brandField.setText("");
        modelField.setText("");
        yearField.setText("");
        if (ownerCombo.getItemCount() > 0) {
            ownerCombo.setSelectedIndex(0);
        }
        selectedCarId = -1;
    }
    
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, 
            message + ": " + e.getMessage(), 
            "Грешка", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}