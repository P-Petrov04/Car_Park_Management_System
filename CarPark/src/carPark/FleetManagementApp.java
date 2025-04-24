package carPark;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.sql.Connection;

public class FleetManagementApp extends JFrame {
    private final Connection conn;
    
    public FleetManagementApp() {
        setTitle("Система за управление на автопарк");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        conn = DBConnection.getConnection();
        
        JTabbedPane tabbedPane = new JTabbedPane();
        RepairsPanel repairsPanel = new RepairsPanel(conn);
        ReportsPanel reportsPanel = new ReportsPanel(conn);
        CarsPanel carsPanel = new CarsPanel (conn, repairsPanel, reportsPanel);
        OwnersPanel ownersPanel = new OwnersPanel(conn, reportsPanel, carsPanel);
        tabbedPane.add("Ремонти", repairsPanel);
        tabbedPane.add("Автомобили", carsPanel);
        tabbedPane.add("Собственици", ownersPanel);
        tabbedPane.add("Справки", reportsPanel);
        
        add(tabbedPane);
    }
}
