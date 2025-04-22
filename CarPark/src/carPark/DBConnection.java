package carPark;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
    private static Connection conn;
    
    public static Connection getConnection() {
        if (conn == null) {
            try {
                String url = "jdbc:sqlserver://195.230.127.185:1433;databaseName=db2301321002;encrypt=true;trustServerCertificate=true;";
                String user = "stu2301321002";
                String password = "Petar2004petrov";
                
                conn = DriverManager.getConnection(url, user, password);
                System.out.println("Успешна връзка с базата данни!");
                initializeDatabase();
            } catch (SQLException e) {
                System.err.println("Грешка при връзка с базата данни:");
                e.printStackTrace();
            }
        }
        return conn;
    }
    
    private static void initializeDatabase() throws SQLException {
        // Създаване на таблици, ако не съществуват
        String createOwners = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Owners') "
                + "CREATE TABLE Owners ("
                + "OwnerID INT PRIMARY KEY IDENTITY(1,1), "
                + "Name NVARCHAR(100) NOT NULL, "
                + "Phone NVARCHAR(20), "
                + "Email NVARCHAR(100))";
        
        String createCars = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Cars') "
                + "CREATE TABLE Cars ("
                + "CarID INT PRIMARY KEY IDENTITY(1,1), "
                + "RegistrationNumber NVARCHAR(20) NOT NULL UNIQUE, "
                + "Brand NVARCHAR(50) NOT NULL, "
                + "Model NVARCHAR(50) NOT NULL, "
                + "Year INT, "
                + "OwnerID INT, "
                + "FOREIGN KEY (OwnerID) REFERENCES Owners(OwnerID))";
        
        String createRepairs = "IF NOT EXISTS (SELECT * FROM sys.tables WHERE name='Repairs') "
                + "CREATE TABLE Repairs ("
                + "RepairID INT PRIMARY KEY IDENTITY(1,1), "
                + "CarID INT NOT NULL, "
                + "Description NVARCHAR(MAX), "
                + "Date DATE DEFAULT GETDATE(), "
                + "Cost DECIMAL(10,2), "
                + "FOREIGN KEY (CarID) REFERENCES Cars(CarID))";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createOwners);
            stmt.execute(createCars);
            stmt.execute(createRepairs);
        }
    }
    
    public static void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
