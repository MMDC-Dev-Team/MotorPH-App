package gui;

import attendance.Attendance;
import employee.Employee;
import utilities.Parser;

import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class MainFrame {
    private JFrame mainFrame;

    private final String role;
    private final String csvPath;

    public MainFrame(String role, String csvPath) {
        this.role = role;
        this.csvPath = csvPath;
        buildUI();
    }

    private void buildUI() {
        // ── Parse data ────────────────────────────────────────────────────────
        // Derive attendance CSV path from the employee CSV path
        String attendanceCsvPath = csvPath.replace("employee_database.csv", "employee_attendance.csv");

        List<Employee> employeeList = Parser.employeeParser(csvPath);
        Map<String, List<Attendance>> attendances = Parser.attendanceParser(attendanceCsvPath);
        Map<Integer, Employee> employeeMap = Employee.buildLookupMap(employeeList);

        mainFrame = new JFrame("MotorPH Employee App — " + roleName());
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1200, 680);
        mainFrame.setLocationRelativeTo(null);

        // Keeps the UI stable and prevents the layout from breaking in fullscreen.
        mainFrame.setResizable(false);

        // ── Root layout ───────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));
        mainFrame.setContentPane(root);

        // ── Top banner ────────────────────────────────────────────────
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(new Color(30, 58, 138));
        banner.setBorder(new EmptyBorder(10, 18, 10, 18));

        JLabel appTitle = new JLabel("MotorPH Employee App");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        appTitle.setForeground(Color.WHITE);

        JPanel rightBanner = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBanner.setOpaque(false);

        JLabel roleLabel = new JLabel("Logged in as: " + roleName());
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        roleLabel.setForeground(new Color(180, 200, 240));

        Color red = new Color(220, 38, 38);

        JButton logoutButton = createButton("Log Out", red);
        logoutButton.addActionListener(_ -> {
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Log out and return to the login screen?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                mainFrame.dispose();
                SwingUtilities.invokeLater(() -> {
                    new LoginFrame(csvPath);
                });
            }
        });

        rightBanner.add(roleLabel);
        rightBanner.add(logoutButton);

        banner.add(appTitle, BorderLayout.WEST);
        banner.add(rightBanner, BorderLayout.EAST);
        root.add(banner, BorderLayout.NORTH);

        // ── Tabbed pane ───────────────────────────────────────────────
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("SansSerif", Font.BOLD, 12));
        tabs.setBackground(new Color(245, 247, 250));

        JPanel feature1Panel = Feature1Panel.createPanel(mainFrame, employeeMap, attendances);

        tabs.addTab("Payroll Search", feature1Panel); // Feature1Panel

        root.add(tabs, BorderLayout.CENTER);
        mainFrame.setVisible(true);
    }

    private JButton createButton(String label, Color buttonColor) {
        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setBackground(buttonColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(4, 12, 4, 12));

        return button;
    }

    private String roleName() {
        return "payroll_staff".equals(role) ? "Payroll Staff" : "Employee";
    }
}