package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarsPanel extends JPanel {
    private RepairsPanel repairsPanel;
    private ReportsPanel reportsPanel;
    private final Connection conn;
    private JTable table;
    private JTextField regNumberField, brandField, modelField, yearField;
    private JComboBox<String> ownerCombo;
    private int selectedCarId = -1;
    private Set<String> existingRegNumbers = new HashSet<>();
    private Map<String, Integer> ownerNameToIdMap = new HashMap<>();

    public CarsPanel(Connection conn, RepairsPanel repairsPanel, ReportsPanel reportsPanel) {
        this.conn = conn;
        this.repairsPanel = repairsPanel;
        this.reportsPanel = reportsPanel;
        setLayout(new BorderLayout());
        initUI();
        refreshTable();
        refreshOwnersCombo();
        loadExistingRegNumbers();
    }

    private void initUI() {
        // Форма за въвеждане
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));

        formPanel.add(new JLabel("Регистрационен номер*:"));
        regNumberField = new JTextField();
        formPanel.add(regNumberField);

        formPanel.add(new JLabel("Марка*:"));
        brandField = new JTextField();
        formPanel.add(brandField);

        formPanel.add(new JLabel("Модел*:"));
        modelField = new JTextField();
        formPanel.add(modelField);

        formPanel.add(new JLabel("Година*:"));
        yearField = new JTextField();
        yearField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                if (text.isEmpty()) return true; 
                try {
                    int year = Integer.parseInt(text);
                    return year >= 1886 && year <= Year.now().getValue(); 
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        });
        formPanel.add(yearField);

        formPanel.add(new JLabel("Собственик*:"));
        ownerCombo = new JComboBox<>();
        ownerCombo.addItem("Избери"); 
        formPanel.add(ownerCombo);

        // Панел за бутони с отстояние
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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
                    CustomTableModel model = (CustomTableModel) table.getModel();
                    selectedCarId = model.getOwnerIdAt(row);
                    
                    regNumberField.setText(table.getValueAt(row, 0).toString());
                    brandField.setText(table.getValueAt(row, 1).toString());
                    modelField.setText(table.getValueAt(row, 2).toString());
                    yearField.setText(table.getValueAt(row, 3).toString());

                    String ownerName = table.getValueAt(row, 4).toString();
                    for (int i = 0; i < ownerCombo.getItemCount(); i++) {
                        if (ownerCombo.getItemAt(i).contains(ownerName)) {
                            ownerCombo.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });

        // Централен панел с BoxLayout: вертикално подреждане
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        add(centerPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    private void loadExistingRegNumbers() {
        try {
            String query = "SELECT RegistrationNumber FROM Cars";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                existingRegNumbers.add(rs.getString("RegistrationNumber").toUpperCase());
            }
        } catch (SQLException e) {
            showError("Грешка при зареждане на регистрационните номера", e);
        }
    }

    private boolean validateCarData() {
        // Валидация на регистрационен номер
        String regNumber = regNumberField.getText().trim();
        if (regNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Регистрационният номер е задължителен!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            regNumberField.requestFocus();
            return false;
        }

        // Проверка за уникалност на регистрационния номер (само при добавяне)
        if (selectedCarId == -1 && existingRegNumbers.contains(regNumber.toUpperCase())) {
            JOptionPane.showMessageDialog(this, "Автомобил с този регистрационен номер вече съществува!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            regNumberField.requestFocus();
            return false;
        }

        // Валидация на марка
        if (brandField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Марката е задължителна!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            brandField.requestFocus();
            return false;
        }

        // Валидация на модел
        if (modelField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Моделът е задължителен!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            modelField.requestFocus();
            return false;
        }

        // Валидация на година
        try {
            int year = Integer.parseInt(yearField.getText().trim());
            if (year < 1886 || year > Year.now().getValue()) {
                JOptionPane.showMessageDialog(this, 
                    String.format("Годината трябва да е между 1886 и %d!", Year.now().getValue()), 
                    "Грешка", JOptionPane.ERROR_MESSAGE);
                yearField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Моля, въведете валидна година!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            yearField.requestFocus();
            return false;
        }

        // Валидация на собственик
        if (ownerCombo.getSelectedIndex() <= 0) { // 0 е "Избери"
            JOptionPane.showMessageDialog(this, "Моля, изберете собственик!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            ownerCombo.requestFocus();
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
        ownerCombo.addItem("Избери");
        ownerNameToIdMap.clear();
        
        try {
            String query = "SELECT OwnerID, Name FROM Owners";
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

    private void addCar(ActionEvent e) {
        try {
            if (!validateCarData()) {
                return;
            }

            String query = "INSERT INTO Cars (RegistrationNumber, Brand, Model, Year, OwnerID) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query);
            String regNumber = regNumberField.getText().trim();
            pstmt.setString(1, regNumber);
            pstmt.setString(2, brandField.getText().trim());
            pstmt.setString(3, modelField.getText().trim());
            pstmt.setInt(4, Integer.parseInt(yearField.getText().trim()));
            
            // Трябва да намерим ownerId по избраното име
            String selectedOwner = (String) ownerCombo.getSelectedItem();
            int ownerId = getOwnerIdByName(selectedOwner);
            pstmt.setInt(5, ownerId);
            
            pstmt.executeUpdate();
            existingRegNumbers.add(regNumber.toUpperCase());
            refreshTable();
            this.repairsPanel.refreshCarCombo();
            this.reportsPanel.refreshCarCombo();
            clearForm();
        } catch (Exception ex) {
            showError("Грешка при добавяне на автомобил", ex);
        }
    }

    // Помощен метод за намиране на ownerId по име
    private int getOwnerIdByName(String name) throws SQLException {
        String query = "SELECT OwnerID FROM Owners WHERE Name = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, name);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("OwnerID");
        }
        throw new SQLException("Собственик с име '" + name + "' не е намерен");
    }

    private void updateCar(ActionEvent e) {
        if (selectedCarId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете автомобил за редактиране", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            if (!validateCarData()) {
                return;
            }

            String query = "UPDATE Cars SET RegistrationNumber=?, Brand=?, Model=?, Year=?, OwnerID=? WHERE CarID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            String newRegNumber = regNumberField.getText().trim();
            pstmt.setString(1, newRegNumber);
            pstmt.setString(2, brandField.getText().trim());
            pstmt.setString(3, modelField.getText().trim());
            pstmt.setInt(4, Integer.parseInt(yearField.getText().trim()));
            
            String selectedOwner = (String) ownerCombo.getSelectedItem();
            int ownerId = ownerNameToIdMap.get(selectedOwner);
            pstmt.setInt(5, ownerId);
            
            pstmt.setInt(6, selectedCarId);
            pstmt.executeUpdate();
            
            loadExistingRegNumbers();
            refreshTable();
            this.repairsPanel.refreshCarCombo();
            this.reportsPanel.refreshCarCombo();
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
                // Вземане на регистрационния номер преди изтриване
                String regNumber = "";
                String selectQuery = "SELECT RegistrationNumber FROM Cars WHERE CarID=?";
                PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
                selectStmt.setInt(1, selectedCarId);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    regNumber = rs.getString("RegistrationNumber");
                }
                
                // Изтриване на автомобила
                String deleteQuery = "DELETE FROM Cars WHERE CarID=?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, selectedCarId);
                deleteStmt.executeUpdate();
                
                // Премахване от кеша
                existingRegNumbers.remove(regNumber.toUpperCase());
                
                refreshTable();
                this.repairsPanel.refreshCarCombo();
                this.reportsPanel.refreshCarCombo();
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
        ownerCombo.setSelectedIndex(0); 
        selectedCarId = -1;
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, 
            message + ": " + e.getMessage(), 
            "Грешка", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}