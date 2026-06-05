package gui;

import authentication.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * LoginFrame
 * <br/>
 * First window the user sees.  Validates credentials using the existing
 * AuthService then opens MainAppFrame for the correct role.
 * <br/>
 * Event handling: ActionListener on the Login button and on the password
 * field (pressing Enter triggers login).
 * Exception handling: empty-field check with JOptionPane before AuthService call.
 */

public class LoginFrame {
    private final AuthService authService = new AuthService();

    private JFrame frame;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    /** Path to the employee CSV - passed through to SearchForm. */
    private final String csvPath;

    public LoginFrame(String csvPath) {
        this.csvPath = csvPath;
        buildUI();
    }

    private void buildUI() {
        frame = new JFrame();
        frame.setTitle("MotorPH Employee App - Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 400);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // ── Root panel ──────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(245, 247, 250));
        frame.setContentPane(root);

        // ── Header band ─────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setBackground(new Color(30, 58, 138));
        header.setBorder(new EmptyBorder(18, 24, 18, 24));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("MotorPH Employee App");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Please sign in to continue");
        sub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        sub.setForeground(new Color(180, 200, 240));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        // ── Form panel ──────────────────────────────────────────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(245, 247, 250));
        form.setBorder(new EmptyBorder(20, 36, 12, 36));

        GridBagConstraints loginConstraints = new GridBagConstraints();
        loginConstraints.fill = GridBagConstraints.HORIZONTAL;
        loginConstraints.insets = new Insets(5, 0, 5, 0);
        loginConstraints.weightx = 1.0;

        // Username row
        loginConstraints.gridx = 0;
        loginConstraints.gridy = 0;
        loginConstraints.gridwidth = 1;
        form.add(makeLabel("Username"), loginConstraints);

        loginConstraints.gridy = 1;
        userNameField = new JTextField();
        styleTextField(userNameField);
        form.add(userNameField, loginConstraints);

        // Password row
        loginConstraints.gridy = 2;
        form.add(makeLabel("Password"), loginConstraints);

        loginConstraints.gridy = 3;
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        passwordField.addActionListener(this::onLogin);
        form.add(passwordField, loginConstraints);

        // Login button
        loginConstraints.gridy = 4;
        loginConstraints.insets = new Insets(14, 0, 4, 0);
        JButton loginButton = new JButton("Sign in");
        styleButton(loginButton);
        loginButton.addActionListener(this::onLogin);
        form.add(loginButton, loginConstraints);

        // Status / error label
        loginConstraints.gridy = 5;
        loginConstraints.insets = new Insets(4, 0, 0, 0);
        statusLabel = new JLabel("");
        statusLabel.setForeground(new Color(180, 30, 30));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        form.add(statusLabel, loginConstraints);

        root.add(form, BorderLayout.CENTER);

        // ── Hint footer ─────────────────────────────────────────────
        JPanel footer = new JPanel();
        footer.setBackground(new Color(235, 238, 244));
        footer.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel hint = new JLabel("employee_user / payroll_staff | password: 12345");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(new Color(120, 130, 150));
        footer.add(hint);
        root.add(footer, BorderLayout.SOUTH);

        frame.setVisible(true);
        userNameField.requestFocusInWindow();
    }

    // ── Event handler ────────────────────────────────────────────────────
    private void onLogin(ActionEvent event) {
        String username = userNameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        // Input validation - Feature 1 requirement
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Username and password are required.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (authService.validate(username, password)) {
                String role = authService.getRole(username);
                frame.dispose();

                SwingUtilities.invokeLater(() -> {
                    new MainFrame(role, csvPath);
                });
            } else {
                statusLabel.setText("Invalid username or password.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
            }

        } catch (Exception err) {
            JOptionPane.showMessageDialog(frame,
                    "Login error: " + err.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Style helpers ────────────────────────────────────────────────────
    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(new Color(55, 65, 90));

        return label;
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        textField.setPreferredSize(new Dimension(0, 32));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 210)),
                new EmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(new Color(30, 58, 138));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 36));
        button.setOpaque(true);
    }
}
