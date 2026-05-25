package gui;

import employee.Employee;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SearchForm {

    private static final Logger LOGGER = Logger.getLogger(SearchForm.class.getName());

    // Color palette.
    private static final Color DARK_BLUE = new Color(30, 58, 138);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color WHITE = Color.WHITE;
    private static final Color BORDER_GRAY = new Color(200, 200, 200);
    private static final Color HINT_GRAY = new Color(160, 160, 160);
    private static final Color LABEL_DARK = new Color(40, 40, 40);
    private static final Color READ_ONLY_GRAY = new Color(235, 235, 235);

    // CSV file path used by Parser to load employee records.
    private static final String EMPLOYEE_DATABASE_PATH = "src/main/resources/employee_database.csv";

    private final JFrame parentFrame;
    private final Map<Integer, Employee> employeeMap;

    // Instance state
    /**
     * Feature1Panel calls this once, before the tab is shown, to register the
     * action that fires when a valid PROCESS click occurs.
     */
    private static Consumer<FormResult> onValidSearch;

    // Callback used when CLEAR is clicked, so Feature1Panel can also clear Payroll Results.
    private static Runnable onClear;

    // ── FormResult record ─────────────────────────────────────────────────────
    /**
     * Carries validated form values to Feature1Panel once the user clicks PROCESS.
     * Using YearMonth (instead of a plain month string) lets PayrollService filter
     * attendance records by exact calendar month without any string parsing.
     */
    public record FormResult(String employeeNumber, String employeeName, YearMonth yearMonth, String selectedCutoff) {}

    /**
     * @param parentFrame   owner frame for JOptionPane dialogs
     * @param employeeMap   pre-built lookup map from {@link Employee#buildLookupMap}
     * @param onValidSearch called with validated form data when PROCESS succeeds
     * @param onClear       called when CLEAR is clicked, so the results panel can reset
     */
    public SearchForm(JFrame parentFrame, Map<Integer, Employee> employeeMap, Consumer<FormResult> onValidSearch, Runnable onClear) {
        this.parentFrame   = parentFrame;
        this.employeeMap   = employeeMap;
        this.onValidSearch = onValidSearch;
        this.onClear       = onClear;
    }

    public JPanel createPanel() {
        return buildSearchFormSection();
    }

    private JPanel buildSearchFormSection() {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(LIGHT_GRAY);

        JPanel infoSection = new JPanel(new GridBagLayout());
        infoSection.setBackground(WHITE);
        infoSection.setPreferredSize(new Dimension(400, 290));
        infoSection.setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
        infoSection.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)
        ));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.fill   = GridBagConstraints.HORIZONTAL;

        JLabel infoTitle = new JLabel("Employee Information");
        infoTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        infoTitle.setForeground(DARK_BLUE);

        constraints.gridx = 0; constraints.gridy = 0;
        constraints.gridwidth = 2; constraints.weightx = 1;
        infoSection.add(infoTitle, constraints);
        constraints.gridwidth = 1;

        JTextField employeeNumberField = createHintField("Positive whole number only");
        JTextField employeeNameField   = createReadOnlyHintField(
                "Auto-filled after Employee Number is entered");

        JComboBox<YearMonth> monthDropdown  = createMonthDropdown();
        JComboBox<String>    cutoffDropdown = createDropdown(new String[]{ "Select Cutoff" });
        cutoffDropdown.setEnabled(false);

        addFormRow(infoSection, constraints, 1, "Employee Number:", employeeNumberField);
        addFormRow(infoSection, constraints, 2, "Employee Name:",   employeeNameField);

        JPanel payCoveragePanel = new JPanel(new BorderLayout(8, 0));
        payCoveragePanel.setBackground(WHITE);
        payCoveragePanel.add(monthDropdown,  BorderLayout.WEST);
        payCoveragePanel.add(cutoffDropdown, BorderLayout.CENTER);
        addFormRow(infoSection, constraints, 3, "Pay Coverage:", payCoveragePanel);

        JButton processButton = createButton("PROCESS", DARK_BLUE, WHITE);
        JButton clearButton   = createButton("CLEAR", BORDER_GRAY, LABEL_DARK);
        processButton.setEnabled(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonPanel.setBackground(WHITE);
        buttonPanel.add(processButton);
        buttonPanel.add(clearButton);

        constraints.gridx = 0; constraints.gridy = 4;
        constraints.gridwidth = 2; constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets(14, 6, 6, 6);
        infoSection.add(buttonPanel, constraints);

        JLabel noteLabel = new JLabel(
                "PROCESS button is disabled until Employee Number and Pay Coverage are completed.");
        noteLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        noteLabel.setForeground(HINT_GRAY);
        noteLabel.setHorizontalAlignment(SwingConstants.CENTER);

        constraints.gridx = 0; constraints.gridy = 5;
        constraints.gridwidth = 2; constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(10, 6, 2, 6);
        infoSection.add(noteLabel, constraints);

        searchPanel.add(infoSection, BorderLayout.CENTER);

        wireEventHandlers(employeeNumberField, employeeNameField,
                monthDropdown, cutoffDropdown,
                processButton, clearButton);

        return searchPanel;
    }

    // Event wiring
    private void wireEventHandlers(JTextField employeeNumberField,
                                   JTextField employeeNameField,
                                   JComboBox<YearMonth> monthDropdown,
                                   JComboBox<String> cutoffDropdown,
                                   JButton processButton,
                                   JButton clearButton) {

        // When Employee Number is typed, the program tries to autofill Employee Name.
        employeeNumberField.getDocument().addDocumentListener(new DocumentListener() {
            private void handleChange() {
                autoFillEmployeeName(employeeNumberField, employeeNameField);
                updateProcessButtonState(employeeNumberField, monthDropdown,
                        cutoffDropdown, processButton);
            }
            @Override public void insertUpdate(DocumentEvent e)  { handleChange(); }
            @Override public void removeUpdate(DocumentEvent e)  { handleChange(); }
            @Override public void changedUpdate(DocumentEvent e) { handleChange(); }
        });

        // When Month is selected, the Cutoff dropdown updates based on the selected month.
        monthDropdown.addActionListener(e -> {
            YearMonth selectedMonth = (YearMonth) monthDropdown.getSelectedItem();
            updateCutoffDropdown(selectedMonth, cutoffDropdown);
            updateProcessButtonState(employeeNumberField, monthDropdown,
                    cutoffDropdown, processButton);
        });

        // When Cutoff is selected, update the Process button state.
        cutoffDropdown.addActionListener(e ->
                updateProcessButtonState(employeeNumberField, monthDropdown,
                        cutoffDropdown, processButton));

        // Clear button resets all fields and also tells Feature1Panel to clear results.
        clearButton.addActionListener(e -> {
            clearForm(employeeNumberField, employeeNameField,
                    monthDropdown, cutoffDropdown, processButton);
            if (onClear != null) onClear.run();
        });

        // Process button validates input and fires the onValidSearch callback.
        processButton.addActionListener(e ->
                handleProcessClick(employeeNumberField, employeeNameField,
                        monthDropdown, cutoffDropdown));
    }

    // Process click
    private void handleProcessClick(JTextField employeeNumberField,
                                    JTextField employeeNameField,
                                    JComboBox<YearMonth> monthDropdown,
                                    JComboBox<String> cutoffDropdown) {
        try {
            String employeeNumberText = employeeNumberField.getText().trim();
            String employeeName       = employeeNameField.getText().trim();

            validateEmployeeNumber(employeeNumberText);

            if (employeeName.isEmpty()) {
                throw new IllegalArgumentException(
                        "Employee record not found. Please enter a valid Employee Number.");
            }
            if (monthDropdown.getSelectedIndex() == 0) {
                throw new IllegalArgumentException("Please select a month.");
            }
            if (cutoffDropdown.getSelectedIndex() == 0) {
                throw new IllegalArgumentException("Please select a cutoff.");
            }

            YearMonth selectedMonth  = (YearMonth) monthDropdown.getSelectedItem();
            String    selectedCutoff = (String) cutoffDropdown.getSelectedItem();

            LOGGER.info("Valid search: emp=" + employeeNumberText
                    + " month=" + selectedMonth + " cutoff=" + selectedCutoff);

            if (onValidSearch != null) {
                onValidSearch.accept(new FormResult(
                        employeeNumberText, employeeName, selectedMonth, selectedCutoff));
            }

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(parentFrame, ex.getMessage(),
                    "Validation Warning", JOptionPane.WARNING_MESSAGE);
            LOGGER.log(Level.WARNING, "Validation warning: {0}", ex.getMessage());

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Unexpected error occurred:\n" + ex.getMessage(),
                    "System Error", JOptionPane.ERROR_MESSAGE);
            LOGGER.log(Level.SEVERE, "Unexpected system error", ex);
        }
    }

    // Instance helpers
    private void autoFillEmployeeName(JTextField employeeNumberField,
                                      JTextField employeeNameField) {
        String text = employeeNumberField.getText().trim();

        if (text.isEmpty() || !text.matches("\\d+")) {
            employeeNameField.setText("");
            return;
        }

        int id = Integer.parseInt(text);
        if (id <= 0) {
            employeeNameField.setText("");
            return;
        }

        try {
            Employee employee = Employee.findById(employeeMap, id);
            employeeNameField.setText(employee.firstName() + " " + employee.lastName());
        } catch (IllegalArgumentException ex) {
            employeeNameField.setText("");
        }
    }

    // Static helpers
    private static void validateEmployeeNumber(String employeeNumberText) {
        if (employeeNumberText.isEmpty()) {
            throw new IllegalArgumentException("Please enter an Employee Number.");
        }
        if (!employeeNumberText.matches("\\d+")) {
            throw new IllegalArgumentException(
                    "Invalid input. Employee Number must contain whole numbers only.");
        }
        if (Integer.parseInt(employeeNumberText) <= 0) {
            throw new IllegalArgumentException("Employee Number must be a positive number.");
        }
    }

    private static void updateProcessButtonState(JTextField employeeNumberField,
                                                 JComboBox<YearMonth> monthDropdown,
                                                 JComboBox<String> cutoffDropdown,
                                                 JButton processButton) {
        boolean hasEmployeeNumber = !employeeNumberField.getText().trim().isEmpty();
        boolean hasMonth          = monthDropdown.getSelectedIndex() > 0;
        boolean hasCutoff         = cutoffDropdown.getSelectedIndex() > 0;
        processButton.setEnabled(hasEmployeeNumber && hasMonth && hasCutoff);
    }

    private static void updateCutoffDropdown(YearMonth selectedMonth,
                                             JComboBox<String> cutoffDropdown) {
        cutoffDropdown.removeAllItems();
        cutoffDropdown.addItem("Select Cutoff");

        if (selectedMonth == null) {
            cutoffDropdown.setEnabled(false);
            return;
        }

        String monthLabel = selectedMonth.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedMonth.getYear();
        int lastDay = selectedMonth.lengthOfMonth();

        cutoffDropdown.addItem("1st Cutoff ("   + monthLabel + " 1 - 15)");
        cutoffDropdown.addItem("2nd Cutoff ("   + monthLabel + " 16 - " + lastDay + ")");
        cutoffDropdown.addItem("Both Cutoffs (" + monthLabel + " 1 - " + lastDay + ")");

        cutoffDropdown.setEnabled(true);
        cutoffDropdown.setSelectedIndex(0);
    }

    private static void clearForm(JTextField employeeNumberField,
                                  JTextField employeeNameField,
                                  JComboBox<YearMonth> monthDropdown,
                                  JComboBox<String> cutoffDropdown,
                                  JButton processButton) {
        employeeNumberField.setText("");
        employeeNameField.setText("");
        monthDropdown.setSelectedIndex(0);
        cutoffDropdown.removeAllItems();
        cutoffDropdown.addItem("Select Cutoff");
        cutoffDropdown.setSelectedIndex(0);
        cutoffDropdown.setEnabled(false);
        processButton.setEnabled(false);
        employeeNumberField.requestFocus();
    }

    // UI Helpers
    private static void addFormRow(JPanel panel, GridBagConstraints constraints,
                                   int row, String labelText, Component inputComponent) {
        constraints.gridx = 0; constraints.gridy = row;
        constraints.weightx = 0; constraints.anchor = GridBagConstraints.LINE_END;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(LABEL_DARK);
        panel.add(label, constraints);

        constraints.gridx = 1; constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.LINE_START;
        panel.add(inputComponent, constraints);
    }

    private static JTextField createHintField(String hint) {
        JTextField field = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    g.setColor(HINT_GRAY);
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    int pad = getInsets().left;
                    int y   = (getHeight() - g.getFontMetrics().getHeight()) / 2
                            + g.getFontMetrics().getAscent();
                    g.drawString(hint, pad + 2, y);
                }
            }
        };
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        return field;
    }

    private static JTextField createReadOnlyHintField(String hint) {
        JTextField field = createHintField(hint);
        field.setEditable(false);
        field.setBackground(READ_ONLY_GRAY);
        return field;
    }

    private static JComboBox<YearMonth> createMonthDropdown() {
        YearMonth[] months = {
                null,
                YearMonth.of(2024, Month.JUNE),    YearMonth.of(2024, Month.JULY),
                YearMonth.of(2024, Month.AUGUST),  YearMonth.of(2024, Month.SEPTEMBER),
                YearMonth.of(2024, Month.OCTOBER), YearMonth.of(2024, Month.NOVEMBER),
                YearMonth.of(2024, Month.DECEMBER)
        };

        JComboBox<YearMonth> dropdown = new JComboBox<>(months) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(160, super.getPreferredSize().height);
            }
        };

        dropdown.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dropdown.setBackground(WHITE);
        dropdown.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value == null) {
                    setText("Select Month");
                } else {
                    YearMonth ym = (YearMonth) value;
                    setText(ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                            + " " + ym.getYear());
                }
                return this;
            }
        });
        return dropdown;
    }

    private static JComboBox<String> createDropdown(String[] options) {
        JComboBox<String> box = new JComboBox<>(options) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(160, super.getPreferredSize().height);
            }
        };

        box.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        box.setBackground(WHITE);
        box.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));

        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(value == null ? "" : value.toString());

                // Only clip the selected-item display (index == -1), not the open dropdown list
                if (index == -1) {
                    label.setToolTipText(value == null ? null : value.toString());
                }
                return label;
            }
        });

        return box;
    }

    private static JButton createButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return button;
    }
}