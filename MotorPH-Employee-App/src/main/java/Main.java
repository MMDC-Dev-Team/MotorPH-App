import gui.LoginFrame;

import javax.swing.*;

public class Main {
    private static final String DEFAULT_CSV =
            "src/main/resources/employee_database.csv";

    public static void main(String[] args) {
        // Use system Look and Feel for a native appearance on each OS
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Falls back to Metal L&F if system L&F is unavailable
        }

        String csvPath = (args.length > 0) ? args[0] : DEFAULT_CSV;


        // All Swing component creation must happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new LoginFrame(csvPath);
        });
    }
}
