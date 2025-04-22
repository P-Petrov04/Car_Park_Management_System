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
        tabbedPane.add("Автомобили", new CarsPanel(conn));
        tabbedPane.add("Собственици", new OwnersPanel(conn));
        //tabbedPane.add("Ремонти", new RepairsPanel(conn));
        //tabbedPane.add("Справки", new ReportsPanel(conn));
        
        add(tabbedPane);
    }
}
