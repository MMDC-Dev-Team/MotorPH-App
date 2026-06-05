package payroll;

import attendance.Attendance;
import employee.Employee;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.logging.Logger;

public class PayrollService {
    private static final Logger logger = Logger.getLogger(PayrollService.class.getName());
    private final Map<String, List<Attendance>> attendances;

    public PayrollService(Map<String, List<Attendance>> attendances) {
        this.attendances = attendances;
    }

    // ── CutoffResult record ───────────────────────────────────────────────────

    /**
     * Immutable value object holding all computed figures for a single cutoff period.
     * Feature1Panel reads these fields to render the results card.
     */
    public record CutoffResult(double hoursWorked,
                               double grossSalary,
                               double sss,
                               double philHealth,
                               double pagIbig,
                               double withholdingTax,
                               double netSalary) {

    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Computes all payroll figures for one cutoff window for a given employee.
     *
     * @param employee  the employee whose rates and allowances apply
     * @param attendances   that employee's full attendance list
     * @param yearMonth     the month being processed
     * @param startDay  first day of the cutoff window (inclusive)
     * @param endDay    last day of the cutoff window (inclusive; use 31 to capture all remaining days)
     * @return a {@link CutoffResult} with hours, gross, deductions, and net
     */
    public CutoffResult computeCutoff(Employee employee, List<Attendance> attendances, YearMonth yearMonth, int startDay, int endDay, int cutoffNum) {

        List<Attendance> window = getCutoffRecords(attendances, yearMonth, startDay, endDay);

        double hoursWorked = PayrollCalculator.calculateWorkHours(window);
        double grossSalary = PayrollCalculator.computeGrossSalary(hoursWorked, employee.hourlyRate());

        double sss, philHealth, pagIbig, withholdingTax, netSalary;

        if (cutoffNum == 1) {
            sss = 0;
            philHealth = 0;
            pagIbig = 0;
            withholdingTax = 0;

            double allowances = employee.riceAllowance() + employee.phoneAllowance() + employee.clothingAllowance();

            netSalary = grossSalary + allowances;
        } else {
            sss = PayrollCalculator.computeSSS(grossSalary);
            philHealth = PayrollCalculator.computePhilHealth(grossSalary);
            pagIbig = PayrollCalculator.computePagIBIG(grossSalary);
            withholdingTax = PayrollCalculator.computeWithholdingTax(grossSalary, sss, philHealth, pagIbig);

            netSalary = grossSalary - sss - philHealth - pagIbig - withholdingTax;
        }


        return new CutoffResult(hoursWorked, grossSalary, sss, philHealth, pagIbig, withholdingTax, netSalary);
    }

    /**
     * Filters a list of attendance records to only those within a specific month and day range.
     * Accepts a YearMonth for comparison to avoid string-based month matching.
     * Used to separate records into cutoff 1 (days 1-15) and cutoff 2 (days 16-31).
     * Records with missing or blank date/time fields are skipped with a logged warning.
     */
    private List<Attendance> getCutoffRecords(List<Attendance> attendances, YearMonth monthYear, int startDay, int endDay) {

        List<Attendance> cutoff = new ArrayList<>();

        for (Attendance attendance : attendances) {
            LocalDate date = attendance.date();
            LocalTime logIn = attendance.logIn();
            LocalTime logOut = attendance.logOut();

            // Skip records with missing or blank date, log-in, or log-out fields
            if (date == null || logIn == null || logOut == null) {
                logger.log(java.util.logging.Level.WARNING,
                        "Skipping record with missing date or time fields for employee #"
                                + attendance.employeeId());
                continue;
            }

            // Compare using YearMonth.from() to ensure both the year and month match,
            // then check whether the day falls within the cutoff window.
            boolean sameMonth = YearMonth.from(date).equals(monthYear);
            int day = date.getDayOfMonth();

            if (sameMonth && day >= startDay && day <= endDay) {
                cutoff.add(attendance);
            }
        }

        return cutoff;
    }

    /**
     * Returns a chronologically sorted, deduplicated list of all YearMonth values
     * present across every employee's attendance record.
     * Used to populate the Month-Year dropdown in SearchForm.
     */
    public List<YearMonth> getAvailableMonths() {
        LinkedHashSet<YearMonth> monthSet = new LinkedHashSet<>();

        for (List<Attendance> records : attendances.values()) {
            for (Attendance record : records) {
                if (record.date() == null) continue;

                monthSet.add(YearMonth.from(record.date()));
            }
        }

        List<YearMonth> sortedMonths = new ArrayList<>(monthSet);
        Collections.sort(sortedMonths);

        return sortedMonths;
    }

    /**
     * Same as {@link #getAvailableMonths()} but scoped to a specific employee's
     * attendance list. Used when "All" months is selected for a single employee.
     */
    public List<YearMonth> getAvailableMonthsForEmployee(List<Attendance> records) {
        return getMonths(records);
    }

    /**
     * Extracts a chronologically sorted, deduplicated list of YearMonth values
     * from the attendance records. Using YearMonth (instead of a plain String) ensures
     * months are compared and sorted in true calendar order regardless of the order
     * records appear in the CSV file.
     * This list drives the outer loop in displayPayroll so each month is processed exactly once.
     */
    private List<YearMonth> getMonths(List<Attendance> attendanceRecords) {
        LinkedHashSet<YearMonth> monthSet = new LinkedHashSet<>();

        for (Attendance attendance : attendanceRecords) {
            LocalDate date = attendance.date();

            if (date == null) {
                logger.log(java.util.logging.Level.WARNING, "Skipping record with missing date for employee #" + attendance.employeeId());
                continue;
            }

            monthSet.add(YearMonth.from(date));
        }

        // Sort months chronologically so payroll is always displayed in calendar order,
        // regardless of the order records appear in the CSV file.
        List<YearMonth> sortedMonths = new ArrayList<>(monthSet);
        Collections.sort(sortedMonths);

        return sortedMonths;

    }

}
