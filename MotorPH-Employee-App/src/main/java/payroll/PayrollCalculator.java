package payroll;

import attendance.Attendance;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.logging.Logger;

public final class PayrollCalculator {

    private static final Logger logger = Logger.getLogger(PayrollCalculator.class.getName());

    /** Utility class — do not instantiate. */
    private PayrollCalculator() {
        throw new UnsupportedOperationException("PayrollCalculator is a utility class");
    }

//------Work Hours Calculation----------------------------------------------------------------
    /**
     * Calculates total billable work hours for a set of attendance records.
     * <br/>
     * Rules applied:
     * - A 10-minute grace period is allowed after 8:00 AM. Employees who log in
     *   within this window are treated as having arrived at exactly 8:00 AM.
     *   Employees who log in after the grace period lose pay from 8:00 AM to their actual login.
     * - Log-out times after 5:00 PM are capped at 5:00 PM; overtime is not compensated.
     * - A 60-minute lunch break is deducted from any day where worked minutes exceed 60,
     *   because the lunch period is unpaid. Days with 60 minutes or fewer of work
     *   are credited as-is without a lunch deduction.
     */
    static double calculateWorkHours(List<Attendance> records) {
        double totalHours = 0.0;

        final LocalTime shiftStart = LocalTime.of(8, 0);
        final LocalTime shiftEnd = LocalTime.of(17, 0);
        final  LocalTime gracePeriodEnd = LocalTime.of(8, 10);
        final int lunchBreak = 60;

        for (Attendance record : records) {
        LocalTime logIn = record.logIn();
        LocalTime logOut = record.logOut();

        if (logIn == null || logOut == null) {
            logger.log(java.util.logging.Level.WARNING,
                    "Skipping record with missing time for employee #" + record.employeeId());
            continue;
        }

        LocalTime effectiveLogIn = logIn.isAfter(gracePeriodEnd) ? logIn : shiftStart;
        LocalTime effectiveLogOut = logOut.isBefore(shiftEnd) ? logOut : shiftEnd;

        long workedMinutes = Duration.between(effectiveLogIn, effectiveLogOut).toMinutes();

        // Handle cases where logout is before login
        if (workedMinutes < 0) {
            logger.log(java.util.logging.Level.WARNING,
                    "Skipping record where logout is before login for employee #"
                            + record.employeeId()
                            + " (login: " + logIn + ", logout: " + logOut + ")");
            continue;
        }

        if (workedMinutes == 0) {
            continue;
        }

        if (workedMinutes > lunchBreak) {
            workedMinutes -= lunchBreak;
        }

        totalHours += (double) workedMinutes / 60;
        }

        return totalHours;
    }

//------Gross Salary Calculation----------------------------------------------------------------
    /**
     * Computes the gross salary for a single cutoff period.
     * Gross salary = hours worked x hourly rate.
     */
    static double computeGrossSalary (double hoursWorked, double hourlyRate) {
        return hoursWorked * hourlyRate;
    }

//------SSS Calculation----------------------------------------------------------------
    /**
     * Computes the monthly SSS contribution based on the salary bracket table.
     * Contributions are stepped in PHP 500 salary increments with a PHP 22.50 increase per step.
     * The minimum contribution is PHP 135.00 and the maximum is PHP 1,125.00.
     * <br/>
     * The formula uses a linear step calculation to avoid hard-coding every bracket individually:
     *   step = floor((salary - baseSalary) / salaryIncrements)
     *   contribution = baseContribution + (step * contributionIncrements)
     */
    static double computeSSS (double grossSalary) {
        double baseSalary = 3250.00;
        double maxSalary = 24750.00;
        double minContribution = 135.00;
        double maxContribution = 1125.00;
        double baseContribution = 157.50; // starting point of the bracket formula
        double salaryIncrement = 500.00;
        double contributionIncrement = 22.50;

        if (grossSalary < baseSalary)
            return minContribution;

        if (grossSalary >= maxSalary)
            return maxContribution;

        double step = Math.floor((grossSalary - baseSalary) / salaryIncrement);
        return baseContribution + (step * contributionIncrement);


    }

//------PhilHealth Calculation----------------------------------------------------------------
    /**
     * Computes the employee's 50% share of the monthly PhilHealth premium.
     * - Monthly gross ≤ PHP 10,000: fixed premium of PHP 300 (employee share: PHP 150)
     * - Monthly gross ≥ PHP 60,000: fixed premium of PHP 1,800 (employee share: PHP 900)
     * - Otherwise: 3% of monthly gross, split equally between employee and employer.
     * <br/>
     * The monthly premium is halved because PhilHealth premiums are shared equally
     * between the employee and the employer under Philippine law.
     */
    static double computePhilHealth (double grossSalary) {

        double minSalary = 10000.00;
        double maxSalary = 60000;
        double minContribution = 300.00;
        double maxContribution = 1800.00;
        double premiumRate = 0.03;

        double monthlyPremium;
        if (grossSalary <= minSalary) {
            monthlyPremium = minContribution;
        }

        else if (grossSalary >= maxSalary) {
            monthlyPremium = maxContribution;
        }

        else {
            monthlyPremium = grossSalary * premiumRate;
        }

        return monthlyPremium / 2.0;
    }

//------PagIBIG Calculation----------------------------------------------------------------
    /**
     * Computes the employee's monthly Pag-Ibig (HDMF) contribution.
     * - Salaries between PHP 1,000–1,500: 1% rate
     * - Salaries above PHP 1,500: 2% rate
     * - Maximum employee contribution is capped at PHP 100 per month.
     * - Salaries below PHP 1,000 are not subject to Pag-Ibig contributions.
     */
    static double computePagIBIG (double grossSalary) {
        double rate;

        if (grossSalary >= 1000 && grossSalary <=1500){
            rate = 0.01;
        }

        else if (grossSalary > 1500) {
            rate = 0.02;
        }

        else {
            rate = 0.0;
        }

        return Math.min(grossSalary * rate, 100.00);
    }

//------Withholding Tax Calculation----------------------------------------------------------------
    /**
     * Computes the monthly withholding tax based on taxable income brackets (TRAIN Law).
     * Taxable income is derived by subtracting SSS, PhilHealth, and Pag-Ibig from the gross.
     * These contributions are deducted first because they are pre-tax under Philippine law,
     * which lowers the taxable base and reduces the overall tax burden.
     * <br/>
     * Tax brackets (monthly):
     *   0        – 20,832  : 0%
     *   20,833   – 33,332  : 20% of excess over 20,833
     *   33,333   – 66,666  : 2,500 + 25% of excess over 33,333
     *   66,667   – 166,666 : 10,833 + 30% of excess over 66,667
     *   166,667  – 666,666 : 40,833.33 + 32% of excess over 166,667
     *   666,667  and above : 200,833.33 + 35% of excess over 666,667
     */
    static double computeWithholdingTax (double grossSalary, double sssDeduction, double philHealthDeduction, double pagIbig) {
        // Pre-tax deductions (SSS, PhilHealth, Pag-Ibig) are subtracted before applying
        // the tax table because these contributions are exempt from income tax.
        double taxableIncome = grossSalary - sssDeduction - philHealthDeduction - pagIbig;

        double tax;

        if (taxableIncome <= 20833) {
            tax = 0.0; // Within the tax-exempt bracket
        } else if (taxableIncome <= 33333) {
            tax = (taxableIncome - 20833) * 0.20; // 20% on excess over 20,833
        } else if (taxableIncome <= 66667) {
            tax = 2500 + (taxableIncome - 33333) * 0.25; // Fixed 2,500 + 25% on excess
        } else if (taxableIncome <= 166667) {
            tax = 10833.33 + (taxableIncome - 66667) * 0.30; // Fixed 10,833 + 30% on excess
        }  else if (taxableIncome <= 666667) {
            tax = 40833.33 + (taxableIncome - 166667) * 0.32; // Fixed 40,833.33 + 32% on excess
        }  else {
            tax = 200833.33 + (taxableIncome - 666667) * 0.35; // Fixed 200,833.33 + 35% on excess
        }

        return tax;
    }

}

