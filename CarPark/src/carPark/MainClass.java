package carPark;

import javax.swing.SwingUtilities;

public class MainClass {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
            FleetManagementApp app = new FleetManagementApp();
            app.setVisible(true);
        });
	}

}
