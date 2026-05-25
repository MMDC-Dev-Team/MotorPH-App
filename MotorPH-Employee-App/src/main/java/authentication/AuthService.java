package authentication;

public class AuthService {
    private static final String[] validUsernames = {"employee_user", "payroll_staff"};
    private static final String password = "12345";

    public boolean validate(String user, String pass) {
        boolean validUsername = false;

        for (String username : validUsernames) {
            if (username.equals(user)) {
                validUsername = true;
                break;
            }
        }

        boolean validPassword = password.equals(pass);

        return validUsername && validPassword;
    }

    public String getRole(String username) {
        if ("employee_user".equals(username)) {
            return "employee_user";
        } else if ("payroll_staff".equals(username)) {
            return "payroll_staff";
        } else {
            return "unknown";
        }
    }
}