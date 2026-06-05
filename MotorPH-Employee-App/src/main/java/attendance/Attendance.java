package attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Immutable value object representing a single attendance entry for one employee.
 * The compact constructor validates all fields at construction time so no
 * invalid Attendance object can exist in the system.
 */
public record Attendance(
        int employeeId,
        String firstName,
        String lastName,
        LocalDate date,
        LocalTime logIn,
        LocalTime logOut
) {
    public Attendance {
        Objects.requireNonNull(firstName, "firstName must not be null");
        Objects.requireNonNull(lastName,  "lastName must not be null");
        Objects.requireNonNull(date,      "date must not be null");
        Objects.requireNonNull(logIn,     "logIn must not be null");
        Objects.requireNonNull(logOut,    "logOut must not be null");
    }
}
