package gui;

import utilities.CSVManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmployeeRecordFrame {

    // =====================================================
    // CONSTANTS
    // =====================================================

    private static final String[] COLUMNS = {
            "Employee #", "Last Name", "First Name", "Birthday", "Address",
            "Phone Number", "SSS #", "PhilHealth #", "TIN #", "Pag-IBIG #",
            "Status", "Position", "Immediate Supervisor", "Basic Salary",
            "Rice Subsidy", "Phone Allowance", "Clothing Allowance",
            "Gross Semi-monthly Rate", "Hourly Rate"
    };

    private static final Color DARK_BLUE = new Color(30, 58, 138);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color HINT_GRAY = new Color(160, 160, 160);

    private static final DateTimeFormatter CLOCK_FORMAT =
            DateTimeFormatter.ofPattern("MMMM dd, yyyy | hh:mm:ss a");

    private static final DateTimeFormatter ADDED_FORMAT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm:ss a");

    // =====================================================
    // FIELDS
    // =====================================================

    private final String csvPath;

    private JTable employeeTable;
    private DefaultTableModel tableModel;

    // Employee Info
    private JTextField empNumField;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField birthdayField;

    // Contact Info
    private JTextField addressField;
    private JTextField phoneField;

    // Government IDs
    private JTextField sssField;
    private JTextField philhealthField;
    private JTextField tinField;
    private JTextField pagibigField;

    // Employment Info
    private JTextField statusField;
    private JTextField positionField;
    private JComboBox<String> supervisorBox;

    // Compensation
    private JTextField basicSalaryField;
    private JTextField riceField;
    private JTextField phoneAllowanceField;
    private JTextField clothingField;
    private JTextField grossSemiMonthlyField;
    private JTextField hourlyRateField;

    // Search
    private JTextField searchField;
    private JComboBox<String> searchOptionBox;

    // Labels
    private JLabel dateTimeLabel;
    private JLabel lastAddedLabel;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeRecordFrame(String csvPath) {
        this.csvPath = csvPath;
    }

    // =====================================================
    // MAIN PANEL
    // =====================================================

    public JPanel createPanel() {
        JPanel root = new JPanel(new BorderLayout(5, 5));
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(createHeaderPanel(), BorderLayout.NORTH);
        topPanel.add(createSearchPanel(), BorderLayout.SOUTH);

        JPanel tablePanel = createTablePanel();
        tablePanel.add(createDeletePanel(), BorderLayout.SOUTH);

        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setPreferredSize(new Dimension(350, 0));

        JScrollPane formScrollPane = new JScrollPane(createFormPanel());
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        formContainer.add(formScrollPane, BorderLayout.CENTER);
        formContainer.add(createFormButtonPanel(), BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                tablePanel,
                formContainer
        );

        splitPane.setResizeWeight(1.0);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);

        root.add(topPanel, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);

        return root;
    }

    // =====================================================
    // UI BUILDERS
    // =====================================================

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(10, 20, 5, 20));

        JLabel titleLabel = new JLabel("MotorPH Employee Records");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(DARK_BLUE);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        dateTimeLabel = new JLabel();
        dateTimeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dateTimeLabel.setForeground(DARK_BLUE);
        dateTimeLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        lastAddedLabel = new JLabel("Last employee added: Not available");
        lastAddedLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lastAddedLabel.setForeground(HINT_GRAY);
        lastAddedLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPanel.add(dateTimeLabel);
        rightPanel.add(lastAddedLabel);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        Timer timer = new Timer(1000, e -> updateDateTime());
        timer.start();
        updateDateTime();

        return headerPanel;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(new EmptyBorder(0, 20, 2, 20));

        searchOptionBox = new JComboBox<>(new String[]{
                "Employee #", "Last Name", "First Name"
        });

        searchField = new JTextField(25);

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> searchEmployee());

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearSearch());

        panel.add(new JLabel("Search by:"));
        panel.add(searchOptionBox);
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(clearButton);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        tableModel = new DefaultTableModel(COLUMNS, 0);
        employeeTable = new JTable(tableModel);

        employeeTable.setDefaultEditor(Object.class, null);
        employeeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        employeeTable.setRowHeight(24);
        employeeTable.setGridColor(Color.WHITE);

        employeeTable.getTableHeader().setBackground(DARK_BLUE);
        // employeeTable.getTableHeader().setForeground(DARK_BLUE);

        employeeTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                loadSelectedEmployeeToForm();
            }
        });

        configureColumnWidths();
        loadEmployeeData();

        panel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(320, 1230));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(LIGHT_GRAY);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        JLabel title = new JLabel("Employee Details");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(DARK_BLUE);

        gbc.gridy = 0;
        panel.add(title, gbc);

        initializeFormFields();

        addFormRow(panel, gbc, 1, "Employee #", empNumField);
        addFormRow(panel, gbc, 2, "Last Name", lastNameField);
        addFormRow(panel, gbc, 3, "First Name", firstNameField);
        addFormRow(panel, gbc, 4, "Birthday", birthdayField);
        addFormRow(panel, gbc, 5, "Address", addressField);
        addFormRow(panel, gbc, 6, "Phone Number", phoneField);
        addFormRow(panel, gbc, 7, "SSS #", sssField);
        addFormRow(panel, gbc, 8, "PhilHealth #", philhealthField);
        addFormRow(panel, gbc, 9, "TIN #", tinField);
        addFormRow(panel, gbc, 10, "Pag-IBIG #", pagibigField);
        addFormRow(panel, gbc, 11, "Status", statusField);
        addFormRow(panel, gbc, 12, "Position", positionField);
        addFormRow(panel, gbc, 13, "Immediate Supervisor", supervisorBox);
        addFormRow(panel, gbc, 14, "Basic Salary", basicSalaryField);
        addFormRow(panel, gbc, 15, "Rice Subsidy", riceField);
        addFormRow(panel, gbc, 16, "Phone Allowance", phoneAllowanceField);
        addFormRow(panel, gbc, 17, "Clothing Allowance", clothingField);
        addFormRow(panel, gbc, 18, "Gross Semi-monthly Rate", grossSemiMonthlyField);
        addFormRow(panel, gbc, 19, "Hourly Rate", hourlyRateField);

        return panel;
    }

    private JPanel createFormButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setPreferredSize(new Dimension(0, 45));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadEmployeeData());

        JButton addButton = new JButton("Add Employee");
        addButton.addActionListener(e -> addEmployee());

        JButton updateButton = new JButton("Update Employee");
        updateButton.addActionListener(e -> updateEmployee());

        panel.add(refreshButton);
        panel.add(addButton);
        panel.add(updateButton);

        return panel;
    }

    private JPanel createDeletePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setPreferredSize(new Dimension(0, 45));
        panel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JButton deleteButton = new JButton("Delete Employee");
        deleteButton.addActionListener(e -> deleteEmployee());

        panel.add(deleteButton);

        return panel;
    }

    // =====================================================
    // FORM HELPERS
    // =====================================================

    private void initializeFormFields() {
        empNumField = createTextField();
        lastNameField = createTextField();
        firstNameField = createTextField();
        birthdayField = createTextField();
        addressField = createTextField();
        phoneField = createTextField();

        sssField = createTextField();
        philhealthField = createTextField();
        tinField = createTextField();
        pagibigField = createTextField();

        statusField = createTextField();
        positionField = createTextField();

        supervisorBox = new JComboBox<>(new String[]{
                "N/A",
                "Benedict Sio",
                "Nikki A. Go",
                "Rosie B. Garcia",
                "Darren M. Valdez"
        });

        basicSalaryField = createTextField();
        riceField = createTextField();
        phoneAllowanceField = createTextField();
        clothingField = createTextField();
        grossSemiMonthlyField = createTextField();
        hourlyRateField = createTextField();
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(220, 30));
        return field;
    }

    private void addFormRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            JComponent component
    ) {
        gbc.gridy = row * 2;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridy = row * 2 + 1;
        component.setPreferredSize(new Dimension(220, 30));
        panel.add(component, gbc);
    }

    private void clearForm() {
        empNumField.setText("");
        lastNameField.setText("");
        firstNameField.setText("");
        birthdayField.setText("");
        addressField.setText("");
        phoneField.setText("");

        sssField.setText("");
        philhealthField.setText("");
        tinField.setText("");
        pagibigField.setText("");

        statusField.setText("");
        positionField.setText("");
        supervisorBox.setSelectedIndex(0);

        basicSalaryField.setText("");
        riceField.setText("");
        phoneAllowanceField.setText("");
        clothingField.setText("");
        grossSemiMonthlyField.setText("");
        hourlyRateField.setText("");

        employeeTable.clearSelection();
    }

    private String[] getFormData() {
        return new String[]{
                empNumField.getText().trim(),
                lastNameField.getText().trim(),
                firstNameField.getText().trim(),
                birthdayField.getText().trim(),
                addressField.getText().trim(),
                phoneField.getText().trim(),
                sssField.getText().trim(),
                philhealthField.getText().trim(),
                tinField.getText().trim(),
                pagibigField.getText().trim(),
                statusField.getText().trim(),
                positionField.getText().trim(),
                (String) supervisorBox.getSelectedItem(),
                basicSalaryField.getText().trim(),
                riceField.getText().trim(),
                phoneAllowanceField.getText().trim(),
                clothingField.getText().trim(),
                grossSemiMonthlyField.getText().trim(),
                hourlyRateField.getText().trim()
        };
    }

    private boolean validateRequiredFields() {
        if (empNumField.getText().trim().isEmpty()
                || firstNameField.getText().trim().isEmpty()
                || lastNameField.getText().trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    null,
                    "Employee #, First Name, and Last Name are required.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;
        }

        return true;
    }

    // =====================================================
    // TABLE HELPERS
    // =====================================================

    private void configureColumnWidths() {
        int[] widths = {
                90, 120, 120, 100, 280,
                130, 120, 130, 120, 120,
                100, 180, 180, 120, 120,
                130, 150, 180, 110
        };

        for (int i = 0; i < widths.length; i++) {
            employeeTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void loadEmployeeData() {
        tableModel.setRowCount(0);

        List<String[]> rows = CSVManager.readAll(csvPath, COLUMNS.length);

        for (String[] row : rows) {
            tableModel.addRow(row);
        }
    }

    private void loadSelectedEmployeeToForm() {
        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);

        empNumField.setText(getTableValue(modelRow, 0));
        lastNameField.setText(getTableValue(modelRow, 1));
        firstNameField.setText(getTableValue(modelRow, 2));
        birthdayField.setText(getTableValue(modelRow, 3));
        addressField.setText(getTableValue(modelRow, 4));
        phoneField.setText(getTableValue(modelRow, 5));

        sssField.setText(getTableValue(modelRow, 6));
        philhealthField.setText(getTableValue(modelRow, 7));
        tinField.setText(getTableValue(modelRow, 8));
        pagibigField.setText(getTableValue(modelRow, 9));

        statusField.setText(getTableValue(modelRow, 10));
        positionField.setText(getTableValue(modelRow, 11));
        supervisorBox.setSelectedItem(getTableValue(modelRow, 12));

        basicSalaryField.setText(getTableValue(modelRow, 13));
        riceField.setText(getTableValue(modelRow, 14));
        phoneAllowanceField.setText(getTableValue(modelRow, 15));
        clothingField.setText(getTableValue(modelRow, 16));
        grossSemiMonthlyField.setText(getTableValue(modelRow, 17));
        hourlyRateField.setText(getTableValue(modelRow, 18));
    }

    private String getTableValue(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    // =====================================================
    // BUTTON ACTIONS
    // =====================================================

    private void addEmployee() {
        if (!validateRequiredFields()) {
            return;
        }

        String employeeId = empNumField.getText().trim();

        if (CSVManager.rowExists(csvPath, employeeId, 0, COLUMNS.length)) {
            JOptionPane.showMessageDialog(
                    null,
                    "Employee ID already exists.",
                    "Duplicate Employee",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        CSVManager.appendRow(csvPath, getFormData());

        loadEmployeeData();
        updateLastAddedLabel();
        clearForm();

        JOptionPane.showMessageDialog(
                null,
                "Employee added successfully."
        );
    }

    private void updateEmployee() {
        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please select an employee first.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (!validateRequiredFields()) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                null,
                "Update employee information?",
                "Confirm Update",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        boolean updated = CSVManager.updateRow(
                csvPath,
                getFormData(),
                0,
                COLUMNS.length
        );

        if (updated) {
            loadEmployeeData();
            clearForm();

            JOptionPane.showMessageDialog(
                    null,
                    "Employee updated successfully."
            );
        } else {
            JOptionPane.showMessageDialog(
                    null,
                    "Employee not found.",
                    "Update Failed",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteEmployee() {
        int selectedRow = employeeTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please select an employee first.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = employeeTable.convertRowIndexToModel(selectedRow);
        String employeeId = getTableValue(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                null,
                "Delete employee " + employeeId + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        CSVManager.deleteRow(csvPath, employeeId, 0, COLUMNS.length);

        loadEmployeeData();
        clearForm();

        JOptionPane.showMessageDialog(
                null,
                "Employee deleted successfully."
        );
    }

    private void searchEmployee() {
        String searchText = searchField.getText().trim().toLowerCase();

        if (searchText.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Please enter a search value."
            );
            return;
        }

        int columnIndex = getSearchColumnIndex();

        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String value = getTableValue(row, columnIndex).toLowerCase();

            if (value.contains(searchText)) {
                employeeTable.setRowSelectionInterval(row, row);
                employeeTable.scrollRectToVisible(
                        employeeTable.getCellRect(row, 0, true)
                );
                loadSelectedEmployeeToForm();
                return;
            }
        }

        JOptionPane.showMessageDialog(
                null,
                "No matching employee found."
        );
    }

    private void clearSearch() {
        searchField.setText("");
        employeeTable.clearSelection();
        clearForm();
        loadEmployeeData();
    }

    // =====================================================
    // SMALL HELPERS
    // =====================================================

    private int getSearchColumnIndex() {
        String selectedOption = (String) searchOptionBox.getSelectedItem();

        switch (selectedOption) {
            case "Last Name":
                return 1;
            case "First Name":
                return 2;
            case "Employee #":
            default:
                return 0;
        }
    }

    private void updateDateTime() {
        dateTimeLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT));
    }

    private void updateLastAddedLabel() {
        String employeeName =
                firstNameField.getText().trim() + " " + lastNameField.getText().trim();

        String timestamp = LocalDateTime.now().format(ADDED_FORMAT);

        lastAddedLabel.setText(
                "<html>Last employee added: " + employeeName
                        + "<br>" + timestamp + "</html>"
        );
    }
}