package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;

public class OwnersPanel extends JPanel {
    private final Connection conn;
    private JTable table;
    private JTextField nameField, phoneField, emailField;
    private int selectedOwnerId = -1;
    
    public OwnersPanel(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout());
        initUI();
        refreshTable();
    }
    
    private void initUI() {
        // Форма за въвеждане
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        
        formPanel.add(new JLabel("Име:"));
        nameField = new JTextField();
        formPanel.add(nameField);
        
        formPanel.add(new JLabel("Телефон:"));
        phoneField = new JTextField();
        formPanel.add(phoneField);
        
        formPanel.add(new JLabel("Имейл:"));
        emailField = new JTextField();
        formPanel.add(emailField);
        
        // Бутони
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(createButton("Добави", this::addOwner));
        buttonPanel.add(createButton("Обнови", this::updateOwner));
        buttonPanel.add(createButton("Изтрий", this::deleteOwner));
        buttonPanel.add(createButton("Изчисти", e -> clearForm()));
        
        // Таблица
        table = new JTable();
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    selectedOwnerId = (Integer) table.getValueAt(row, 0);
                    nameField.setText(table.getValueAt(row, 1).toString());
                    phoneField.setText(table.getValueAt(row, 2) != null ? table.getValueAt(row, 2).toString() : "");
                    emailField.setText(table.getValueAt(row, 3) != null ? table.getValueAt(row, 3).toString() : "");
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
            String query = "SELECT OwnerID, Name, Phone, Email FROM Owners";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            table.setModel(new CustomTableModel(rs));
        } catch (Exception e) {
            showError("Грешка при зареждане на собствениците", e);
        }
    }
    
    private void addOwner(ActionEvent e) {
        try {
            String query = "INSERT INTO Owners (Name, Phone, Email) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, nameField.getText().trim());
            setNullableString(pstmt, 2, phoneField.getText().trim());
            setNullableString(pstmt, 3, emailField.getText().trim());
            
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
            
            // Обновяваме комбобокса с собственици в CarsPanel
            if (this.getTopLevelAncestor() instanceof JFrame) {
                JFrame frame = (JFrame) this.getTopLevelAncestor();
                for (Component comp : frame.getContentPane().getComponents()) {
                    if (comp instanceof JTabbedPane) {
                        JTabbedPane tabs = (JTabbedPane) comp;
                        for (int i = 0; i < tabs.getTabCount(); i++) {
                            Component tabComp = tabs.getComponentAt(i);
                            if (tabComp instanceof CarsPanel) {
                                ((CarsPanel) tabComp).refreshOwnersCombo();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            showError("Грешка при добавяне на собственик", ex);
        }
    }
    
    private void updateOwner(ActionEvent e) {
        if (selectedOwnerId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете собственик за редактиране", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            String query = "UPDATE Owners SET Name=?, Phone=?, Email=? WHERE OwnerID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, nameField.getText().trim());
            setNullableString(pstmt, 2, phoneField.getText().trim());
            setNullableString(pstmt, 3, emailField.getText().trim());
            pstmt.setInt(4, selectedOwnerId);
            
            pstmt.executeUpdate();
            refreshTable();
            clearForm();
            
            // Обновяваме комбобокса с собственици в CarsPanel
            if (this.getTopLevelAncestor() instanceof JFrame) {
                JFrame frame = (JFrame) this.getTopLevelAncestor();
                for (Component comp : frame.getContentPane().getComponents()) {
                    if (comp instanceof JTabbedPane) {
                        JTabbedPane tabs = (JTabbedPane) comp;
                        for (int i = 0; i < tabs.getTabCount(); i++) {
                            Component tabComp = tabs.getComponentAt(i);
                            if (tabComp instanceof CarsPanel) {
                                ((CarsPanel) tabComp).refreshOwnersCombo();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            showError("Грешка при обновяване на собственик", ex);
        }
    }
    
    private void deleteOwner(ActionEvent e) {
        if (selectedOwnerId == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете собственик за изтриване", 
                "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Проверка дали собственика има регистрирани автомобили
        if (hasAssociatedCars(selectedOwnerId)) {
            JOptionPane.showMessageDialog(this, 
                "Не можете да изтриете собственик, който има регистрирани автомобили!\n" +
                "Първо премахнете или пренасочете автомобилите му.", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Сигурни ли сте, че искате да изтриете този собственик?", 
            "Потвърждение", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String query = "DELETE FROM Owners WHERE OwnerID=?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, selectedOwnerId);
                pstmt.executeUpdate();
                refreshTable();
                clearForm();
            } catch (SQLException ex) {
                showError("Грешка при изтриване на собственик", ex);
            }
        }
    }
    
    private boolean hasAssociatedCars(int ownerId) {
        try {
            String query = "SELECT COUNT(*) FROM Cars WHERE OwnerID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, ownerId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private void setNullableString(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value == null || value.trim().isEmpty()) {
            pstmt.setNull(index, Types.VARCHAR);
        } else {
            pstmt.setString(index, value);
        }
    }
    
    private void clearForm() {
        nameField.setText("");
        phoneField.setText("");
        emailField.setText("");
        selectedOwnerId = -1;
    }
    
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, 
            message + ": " + e.getMessage(), 
            "Грешка", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}