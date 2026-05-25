package employee;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Immutable value object representing a MotorPH employee.
 * All validation happens in the compact constructor so an Employee instance
 * is always in a valid state after construction.
 */
public record Employee(int employeeID,
                       String firstName,
                       String lastName,
                       LocalDate birthDate,
                       String address,
                       String contactNo,
                       String sss,
                       String philHealth,
                       String tin,
                       String pagIBIG,
                       String status,
                       String position,
                       String supervisor,
                       double basicSalary,
                       double riceAllowance,
                       double phoneAllowance,
                       double clothingAllowance,
                       double grossSemiMonthlySalary,
                       double hourlyRate) {

    public Employee {
        Objects.requireNonNull(firstName,  "firstName must not be null");
        Objects.requireNonNull(lastName,   "lastName must not be null");
        Objects.requireNonNull(birthDate,  "birthDate must not be null");
        Objects.requireNonNull(address,    "address must not be null");
        Objects.requireNonNull(contactNo,  "contactNo must not be null");
        Objects.requireNonNull(sss,        "sss must not be null");
        Objects.requireNonNull(philHealth, "philHealth must not be null");
        Objects.requireNonNull(tin,        "tin must not be null");
        Objects.requireNonNull(pagIBIG,    "pagIBIG must not be null");
        Objects.requireNonNull(status,     "status must not be null");
        Objects.requireNonNull(position,   "position must not be null");
        Objects.requireNonNull(supervisor, "supervisor must not be null");

        if (basicSalary < 0)            throw new IllegalArgumentException("basicSalary must be non-negative");
        if (riceAllowance < 0)          throw new IllegalArgumentException("riceAllowance must be non-negative");
        if (phoneAllowance < 0)         throw new IllegalArgumentException("phoneAllowance must be non-negative");
        if (clothingAllowance < 0)      throw new IllegalArgumentException("clothingAllowance must be non-negative");
        if (grossSemiMonthlySalary < 0) throw new IllegalArgumentException("grossSemiMonthlySalary must be non-negative");
        if (hourlyRate < 0)             throw new IllegalArgumentException("hourlyRate must be non-negative");
    }

    // Add alongside the existing findById — replace the old one entirely
    /**
     * Builds an O(1) lookup map from a list of employees, keyed by employee ID.
     * Call this once at startup and reuse the map for all subsequent lookups.
     */
    public static Map<Integer, Employee> buildLookupMap(List<Employee> employees) {
        return employees.stream().collect(Collectors.toMap(Employee::employeeID, e -> e));
    }


    /**
     * Looks up an employee by ID in O(1) time using a pre-built map.
     *
     * @param employeeMap the map produced by {@link #buildLookupMap}
     * @param employeeID  the ID to find
     * @return the matching Employee
     * @throws IllegalArgumentException if the ID is not in the map
     */
    public static Employee findById(Map<Integer,Employee> employeeMap, int employeeID) {
        Employee employee = employeeMap.get(employeeID);

        if (employee == null) {
            throw new IllegalArgumentException("Employee with id " + employeeID + " not found");
        }

        return employee;
    }
}