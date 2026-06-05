package gui;

import attendance.Attendance;
import employee.Employee;
import payroll.PayrollService;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.format.DateTimeFormatter;


public class Feature1Panel {

    // ── Color palette ─────────────────────────────────────────────────────────
    private static final Color DARK_BLUE  = new Color(30, 58, 138);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color WHITE      = Color.WHITE;
    private static final Color BORDER_GRAY = new Color(150, 150, 150);
    private static final Color HINT_GRAY  = new Color(160, 160, 160);
    private static final Color LABEL_DARK = new Color(40, 40, 40);
    private static final Color SECTION_BG = new Color(240, 244, 255);

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private static final int LEFT_COLUMN_WIDTH = 500;

    // ── Entry point ───────────────────────────────────────────────────────────

    /**
     * Builds the Feature 1 panel.
     *
     * @param parentFrame  owner frame for dialogs
     * @param employeeMap  O(1) lookup map built once by MainFrame
     * @param attendances  attendance records keyed by employee ID string
     */
    public static JPanel createPanel(JFrame parentFrame,
                                     Map<Integer, Employee> employeeMap,
                                     Map<String, List<Attendance>> attendances) {

        PayrollService payrollService = new PayrollService(attendances);

        // ── Root: left column | right column ──────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(LIGHT_GRAY);
        root.setBorder(new EmptyBorder(14, 16, 14, 16));

        // ── Left column ───────────────────────────────────────────────────────
        JPanel leftColumn = new JPanel(new GridBagLayout());
        leftColumn.setBackground(LIGHT_GRAY);
        leftColumn.setMinimumSize(new Dimension(LEFT_COLUMN_WIDTH, 0));   // ADD THIS
        leftColumn.setPreferredSize(new Dimension(LEFT_COLUMN_WIDTH, 0));

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.fill    = GridBagConstraints.BOTH;
        leftGbc.gridx   = 0;
        leftGbc.weightx = 1;

        // ── Right column – Payroll Results ────────────────────────────────────
        JPanel resultsCard = buildCard("Payroll Results");
        resultsCard.setLayout(new BorderLayout());

        JPanel placeholder = new JPanel(new GridBagLayout());
        placeholder.setBackground(WHITE);
        JLabel placeholderLabel = new JLabel("Results will appear here after clicking PROCESS.");
        placeholderLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        placeholderLabel.setForeground(HINT_GRAY);
        placeholder.add(placeholderLabel);

        JPanel resultsContent = new JPanel();
        resultsContent.setLayout(new BoxLayout(resultsContent, BoxLayout.Y_AXIS));
        resultsContent.setBackground(WHITE);
        resultsContent.add(placeholder);

        JScrollPane scrollPane = new JScrollPane(resultsContent);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        resultsCard.add(scrollPane, BorderLayout.CENTER);

        // ── SearchForm — instantiated with injected callbacks ─────────────────
        SearchForm searchForm = new SearchForm(
                parentFrame,
                employeeMap,
                payload -> {
                    Employee employee;
                    try {
                        employee = Employee.findById(employeeMap,
                                Integer.parseInt(payload.employeeNumber()));
                    } catch (IllegalArgumentException error) {
                        JOptionPane.showMessageDialog(parentFrame,
                                error.getMessage() + "\nPlease check the Employee Number and try again.",
                                "Employee Not Found", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String employeeIdString = String.valueOf(employee.employeeID());
                    List<Attendance> empAttendance = attendances.getOrDefault(employeeIdString, List.of());
                    YearMonth yearMonth = payload.yearMonth();
                    String    cutoff    = payload.selectedCutoff();

                    boolean show1st = cutoff.startsWith("1st") || cutoff.startsWith("Both");
                    boolean show2nd = cutoff.startsWith("2nd") || cutoff.startsWith("Both");

                    resultsContent.removeAll();

                    if (show1st) {
                        PayrollService.CutoffResult cutoff1 = payrollService.computeCutoff(
                                employee, empAttendance, yearMonth, 1, 15, 1);
                        resultsContent.add(buildFirstCutoffBlock(employee, yearMonth, cutoff1));
                        resultsContent.add(Box.createVerticalStrut(10));
                    }

                    if (show2nd) {
                        PayrollService.CutoffResult cutoff2 = payrollService.computeCutoff(
                                employee, empAttendance, yearMonth, 16,
                                yearMonth.lengthOfMonth(), 2);
                        resultsContent.add(buildSecondCutoffBlock(employee, yearMonth, cutoff2));
                        resultsContent.add(Box.createVerticalStrut(10));
                    }

                    resultsContent.revalidate();
                    resultsContent.repaint();
                    SwingUtilities.invokeLater(() ->
                            scrollPane.getVerticalScrollBar().setValue(0));
                },
                () -> {
                    resultsContent.removeAll();
                    resultsContent.add(placeholder);
                    resultsContent.revalidate();
                    resultsContent.repaint();
                }
        );

        JPanel searchPanel = searchForm.createPanel();

        // ── Assemble left column ──────────────────────────────────────────────
        leftGbc.gridy   = 0;
        leftGbc.weighty = 1;
        leftGbc.insets  = new Insets(0, 0, 0, 0);
        leftColumn.add(searchPanel, leftGbc);

        leftGbc.gridy   = 1;
        leftGbc.weighty = 0;
        leftGbc.insets  = new Insets(10, 0, 0, 0);
        leftColumn.add(buildInstructionsPanel(), leftGbc);

        // ── Assemble root ─────────────────────────────────────────────────────
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                leftColumn, resultsCard);
        splitPane.setDividerLocation(LEFT_COLUMN_WIDTH);

        // Right side absorbs all resize; left column stays fixed.
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setBackground(LIGHT_GRAY);

        // Prevent the user from collapsing either panel accidentally.
        splitPane.setOneTouchExpandable(false);

        root.add(splitPane, BorderLayout.CENTER);

        return root;
    }

    // ── Instructions panel ────────────────────────────────────────────────────

    /**
     * Extracted into its own method to reduce the length of createPanel()
     * and make the instruction content easy to update independently.
     */
    private static JPanel buildInstructionsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LIGHT_GRAY);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createDashedBorder(BORDER_GRAY),
                new EmptyBorder(15, 20, 20, 20)
        ));

        JLabel title = new JLabel("Instructions");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(130, 130, 150));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));

        String[] steps = {
                "1. Enter the Employee Number to look up an employee.",
                "2. Select the Month and Cutoff for the pay coverage.",
                "3. Click PROCESS to continue and prepare the payroll result.",
                "4. Click CLEAR to reset the form."
        };

        for (String step : steps) {
            JLabel stepLabel = new JLabel(step);
            stepLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            stepLabel.setForeground(new Color(150, 150, 170));
            stepLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(stepLabel);
            panel.add(Box.createVerticalStrut(6));
        }

        return panel;
    }

    // ── Cutoff result cards ───────────────────────────────────────────────────

    /**
     * Renders the 1st cutoff card: earnings, allowances, and net salary.
     * Deductions are not applied on the first cutoff.
     */
    private static JPanel buildFirstCutoffBlock(Employee employee,
                                                YearMonth yearMonth,
                                                PayrollService.CutoffResult result) {
        JPanel card = createBaseCard(employee, yearMonth, 1);

        card.add(sectionDivider(
                "Cutoff 1: " + yearMonth.format(MONTH_FMT) + " (1-15)"));
        card.add(Box.createVerticalStrut(8));
        card.add(doubleRowLabel(
                "Total Hours Worked", result.hoursWorked() + " hrs",
                "Hourly Rate",        "₱ " + fmt(result.grossSalary())));

        card.add(Box.createVerticalStrut(4));
        card.add(rowLabel("Rice Subsidy",       "₱ " + fmt(employee.riceAllowance()),     false));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Phone Allowance",    "₱ " + fmt(employee.phoneAllowance()),    false));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Clothing Allowance", "₱ " + fmt(employee.clothingAllowance()), false));

        appendNetSalaryRow(card, result.netSalary());
        return card;
    }

    /**
     * Renders the 2nd cutoff card: earnings, government deductions, and net salary.
     * Allowances are not shown on the second cutoff.
     */
    private static JPanel buildSecondCutoffBlock(Employee employee,
                                                 YearMonth yearMonth,
                                                 PayrollService.CutoffResult result) {
        JPanel card = createBaseCard(employee, yearMonth, 2);
        int lastDay = yearMonth.lengthOfMonth();

        card.add(sectionDivider(
                "Cutoff 2: " + yearMonth.format(MONTH_FMT) + " (16-" + lastDay + ")"));
        card.add(Box.createVerticalStrut(8));
        card.add(doubleRowLabel(
                "Total Hours Worked", result.hoursWorked() + " hrs",
                "Hourly Rate",        "₱ " + fmt(result.grossSalary())));

        card.add(Box.createVerticalStrut(8));
        card.add(sectionDivider("Deductions"));
        card.add(Box.createVerticalStrut(4));
        card.add(rowLabel("SSS",             "₱ " + fmt(result.sss()),            false));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("PhilHealth",      "₱ " + fmt(result.philHealth()),     false));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Pag-IBIG",        "₱ " + fmt(result.pagIbig()),        false));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Withholding Tax", "₱ " + fmt(result.withholdingTax()), false));

        appendNetSalaryRow(card, result.netSalary());
        return card;
    }

    /**
     * Builds the shared employee info header that both cutoff cards start with.
     * Each cutoff method adds its own section content after calling this.
     */
    private static JPanel createBaseCard(Employee employee,
                                         YearMonth yearMonth,
                                         int cutoffNum) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(12, 16, 14, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(rowLabel("Employee Number",
                String.valueOf(employee.employeeID()), true));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Employee Name",
                employee.firstName() + " " + employee.lastName(), true));
        card.add(Box.createVerticalStrut(2));
        card.add(rowLabel("Pay Coverage",
                yearMonth.format(MONTH_FMT) + " - Cutoff " + cutoffNum, true));
        card.add(Box.createVerticalStrut(8));

        return card;
    }

    /**
     * Appends the horizontal separator and net salary row that every cutoff card ends with.
     * Extracted to avoid duplicating these four lines in both cutoff builders.
     */
    private static void appendNetSalaryRow(JPanel card, double netSalary) {
        card.add(Box.createVerticalStrut(8));

        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_GRAY);
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(separator);

        card.add(Box.createVerticalStrut(6));
        card.add(rowLabel("Net Salary", "₱ " + fmt(netSalary), false));
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private static JPanel rowLabel(String label, String value, boolean highlight) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", highlight ? Font.BOLD : Font.PLAIN, 12));
        labelComponent.setForeground(highlight ? LABEL_DARK : HINT_GRAY);

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Segoe UI", highlight ? Font.BOLD : Font.PLAIN, 12));
        valueComponent.setForeground(LABEL_DARK);
        valueComponent.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(labelComponent, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.EAST);

        return row;
    }

    private static JPanel doubleRowLabel(String label1, String value1,
                                         String label2, String value2) {
        JPanel row = new JPanel(new GridLayout(1, 4, 4, 0));
        row.setBackground(WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl1 = styledLabel(label1, false, SwingConstants.LEFT);
        JLabel val1 = styledLabel(value1, false, SwingConstants.LEFT);
        JLabel lbl2 = styledLabel(label2, false, SwingConstants.RIGHT);
        JLabel val2 = styledLabel(value2, false, SwingConstants.RIGHT);

        lbl1.setForeground(HINT_GRAY);
        lbl2.setForeground(HINT_GRAY);

        row.add(lbl1); row.add(val1);
        row.add(lbl2); row.add(val2);

        return row;
    }

    private static JLabel styledLabel(String text, boolean bold, int align) {
        JLabel label = new JLabel(text, align);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 12));
        label.setForeground(LABEL_DARK);
        return label;
    }

    private static JPanel sectionDivider(String title) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SECTION_BG);
        bar.setBorder(new EmptyBorder(4, 8, 4, 8));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel label = new JLabel(title, SwingConstants.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setForeground(DARK_BLUE);
        bar.add(label, BorderLayout.CENTER);

        return bar;
    }

    private static JPanel buildCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(10, 14, 14, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(DARK_BLUE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));

        return card;
    }

    private static String fmt(double value) {
        return String.format("%,.2f", value);
    }
}
