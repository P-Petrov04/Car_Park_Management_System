package carPark;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.awt.event.ActionEvent;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

public class OwnersPanel extends JPanel {
    private ReportsPanel reportsPanel;
    private CarsPanel carsPanel;
    private final Connection conn;
    private JTable table;
    private JTextField nameField, phoneField, emailField;
    private int selectedOwnerId = -1;
    
    // Регулярни изрази за валидация
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{0,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    
    public OwnersPanel(Connection conn, ReportsPanel reportsPanel, CarsPanel carsPanel) {
        this.conn = conn;
        this.reportsPanel = reportsPanel;
        this.carsPanel = carsPanel;
        setLayout(new BorderLayout());
        initUI();
        refreshTable();
    }
    
    private void initUI() {
        // Форма за въвеждане
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));

        formPanel.add(new JLabel("Име*:"));
        nameField = new JTextField();
        formPanel.add(nameField);

        formPanel.add(new JLabel("Телефон:"));
        phoneField = new JTextField();
        phoneField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                return text.isEmpty() || PHONE_PATTERN.matcher(text).matches();
            }
        });
        formPanel.add(phoneField);

        formPanel.add(new JLabel("Имейл:"));
        emailField = new JTextField();
        emailField.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                String text = ((JTextField) input).getText();
                return text.isEmpty() || EMAIL_PATTERN.matcher(text).matches();
            }
        });
        formPanel.add(emailField);

        // Бутони
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
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
                    selectedOwnerId = ((CustomTableModel) table.getModel()).getOwnerIdAt(row);
                    nameField.setText(table.getValueAt(row, 0).toString()); 
                    phoneField.setText(table.getValueAt(row, 1) != null ? table.getValueAt(row, 1).toString() : "");
                    emailField.setText(table.getValueAt(row, 2) != null ? table.getValueAt(row, 2).toString() : "");
                }
            }
        });

        // Централен панел с вертикално подреждане
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalStrut(10));

        add(centerPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
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
    
    private boolean validateOwnerData() {
        // Валидация на име
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Името е задължително поле!", "Грешка", JOptionPane.ERROR_MESSAGE);
            nameField.requestFocus();
            return false;
        }
        
        // Валидация на телефон
        if (!phoneField.getText().isEmpty() && !PHONE_PATTERN.matcher(phoneField.getText()).matches()) {
            JOptionPane.showMessageDialog(this, "Телефонният номер трябва да съдържа само цифри (макс. 10)!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            phoneField.requestFocus();
            return false;
        }
        
        // Валидация на имейл
        if (!emailField.getText().isEmpty() && !EMAIL_PATTERN.matcher(emailField.getText()).matches()) {
            JOptionPane.showMessageDialog(this, "Моля, въведете валиден имейл адрес!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            emailField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private boolean checkForDuplicatePhoneOrEmail() throws SQLException {
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();
        
        StringBuilder query = new StringBuilder(
            "SELECT COUNT(*) FROM Owners WHERE OwnerID <> ? AND (");
        
        List<Object> parameters = new ArrayList<>();
        parameters.add(selectedOwnerId == -1 ? 0 : selectedOwnerId);
        
        boolean hasCondition = false;
        
        if (!phone.isEmpty()) {
            query.append("Phone = ?");
            parameters.add(phone);
            hasCondition = true;
        }
        
        if (!email.isEmpty()) {
            if (hasCondition) query.append(" OR ");
            query.append("Email = ?");
            parameters.add(email);
            hasCondition = true;
        }
        
        if (!hasCondition) {
            return false; 
        }
        
        query.append(")");
        
        PreparedStatement pstmt = conn.prepareStatement(query.toString());
        for (int i = 0; i < parameters.size(); i++) {
            pstmt.setObject(i + 1, parameters.get(i));
        }
        
        ResultSet rs = pstmt.executeQuery();
        if (rs.next() && rs.getInt(1) > 0) {
            JOptionPane.showMessageDialog(this, 
                "Съществува собственик със същия телефон или имейл!", 
                "Грешка", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        
        return false;
    }
    
    private void addOwner(ActionEvent e) {
        try {
            if (!validateOwnerData() || checkForDuplicatePhoneOrEmail()) {
                return;
            }
            
            String query = "INSERT INTO Owners (Name, Phone, Email) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, nameField.getText().trim());
            setNullableString(pstmt, 2, phoneField.getText().trim());
            setNullableString(pstmt, 3, emailField.getText().trim());
            
            pstmt.executeUpdate();
            refreshTable();
            this.reportsPanel.refreshOwnerCombo();
            this.carsPanel.refreshOwnersCombo();
            clearForm();
            
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
            if (!validateOwnerData() || checkForDuplicatePhoneOrEmail()) {
                return;
            }
            
            String query = "UPDATE Owners SET Name=?, Phone=?, Email=? WHERE OwnerID=?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, nameField.getText().trim());
            setNullableString(pstmt, 2, phoneField.getText().trim());
            setNullableString(pstmt, 3, emailField.getText().trim());
            pstmt.setInt(4, selectedOwnerId);
            
            pstmt.executeUpdate();
            refreshTable();
            this.reportsPanel.refreshOwnerCombo();
            this.carsPanel.refreshOwnersCombo();
            clearForm();
            
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
                this.reportsPanel.refreshOwnerCombo();
                this.carsPanel.refreshOwnersCombo();
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