package utilities;

import employee.Employee;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import attendance.Attendance;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(Parser.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    /**
     * Parses the employee database CSV and maps each record to a HashMap.
     * Returns a list of all employees with their ID, name, birthdate, and hourly rate.
     */
    public static List<Employee> employeeParser(String employeeDatabase) {
        List<Employee> employees = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(Paths.get(employeeDatabase));
             var csvRecords = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : csvRecords) {
                int empId = Integer.parseInt(record.get("Employee #"));
                String firstName = record.get("First Name");
                String lastName = record.get("Last Name");
                LocalDate birthDate = LocalDate.parse(record.get("Birthday"), DATE_FORMATTER);
                String address = record.get("Address");
                String phoneNumber = record.get("Phone Number");
                String sss = record.get("SSS #");
                String philHealth = record.get("Philhealth #");
                String tin = record.get("TIN #");
                String pagIBIG = record.get("Pag-ibig #");
                String status = record.get("Status");
                String position = record.get("Position");
                String immediateSup = record.get("Immediate Supervisor");
                double basicSalary = parseDoubleSafe(record.get("Basic Salary"));
                double riceAllowance = parseDoubleSafe(record.get("Rice Subsidy"));
                double phoneAllowance = parseDoubleSafe(record.get("Phone Allowance"));
                double clothingAllowance = parseDoubleSafe(record.get("Clothing Allowance"));
                double grossSemiMonthlyRate = parseDoubleSafe(record.get("Gross Semi-monthly Rate"));
                double hourlyRate = parseDoubleSafe(record.get("Hourly Rate"));

                Employee employee = new Employee(empId, firstName, lastName, birthDate, address, phoneNumber, sss, philHealth, tin, pagIBIG, status, position, immediateSup, basicSalary, riceAllowance, phoneAllowance, clothingAllowance, grossSemiMonthlyRate, hourlyRate);
                employees.add(employee);
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to read employee records from file: " + employeeDatabase, e);
        }

        return employees;
    }

    /**
     * Parses the employee attendance CSV and groups records by employee ID.
     * Using a map allows O(1) lookup of all attendance records for a given employee
     * rather than scanning the entire list each time.
     */
    public static Map<String, List<Attendance>> attendanceParser(String attendanceDatabase) {
        Map<String, List<Attendance>> attendanceRecord = new HashMap<>();

        try (Reader reader = Files.newBufferedReader(Paths.get(attendanceDatabase));
             var csvRecords = CSVFormat.DEFAULT.builder().setHeader("Employee #", "Last Name", "First Name", "Date", "Log In", "Log Out").setSkipHeaderRecord(true).build().parse(reader)) {

            for (CSVRecord record : csvRecords) {
                String empId = record.get("Employee #");
                String firstName = record.get("First Name");
                String lastName = record.get("Last Name");
                LocalDate date = LocalDate.parse(record.get("Date"), DATE_FORMATTER);
                LocalTime logIn = LocalTime.parse(record.get("Log In"), TIME_FORMATTER);
                LocalTime logOut = LocalTime.parse(record.get("Log Out"), TIME_FORMATTER);

                Attendance empAttendance = new Attendance(Integer.parseInt(empId), firstName, lastName, date, logIn, logOut);

                attendanceRecord.computeIfAbsent(empId, _ -> new ArrayList<>()).add(empAttendance);
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to read attendance records from file: " + attendanceDatabase, e);
        }

        return attendanceRecord;
    }

    private static double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(value.replace(",", "").trim());
    }
}
