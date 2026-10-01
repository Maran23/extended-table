package tools.maran.extendedtable.manual;

import java.time.LocalDate;
import java.util.Random;

import tools.maran.extendedtable.manual.Person.Department;

/// Provides the random values the [PersonGenerator] fills its [Person]s with.
///
/// @author Marius Hanl
public final class PersonData {

    private static final String[] FIRST_NAMES = { "Anna", "Ben", "Clara", "David", "Emma", "Felix", "Greta", "Hannes",
            "Ida", "Jonas", "Katharina", "Lukas", "Marie", "Noah", "Olivia", "Paul", "Quinn", "Rosa", "Simon", "Theo" };

    private static final String[] LAST_NAMES = { "Bauer", "Becker", "Fischer", "Hoffmann", "Kaiser", "Klein", "Koch",
            "König", "Krause", "Lehmann", "Meyer", "Müller", "Neumann", "Richter", "Schmidt", "Schneider", "Schulz",
            "Wagner", "Weber", "Zimmermann" };

    private static final String[] NOTES = { "", "Works from the office.", "Prefers remote work.",
            "On parental leave until the end of the year.", "Mentors two of the new colleagues.",
            "Team lead.\nResponsible for the release planning.",
            "Joined from the support team.\nStill helps out on busy days.\nSpeaks four languages.",
            "No notes available." };

    private static final LocalDate FIRST_HIRE_DATE = LocalDate.of(2000, 1, 1);
    private static final int HIRE_DATE_RANGE_DAYS = 9_400;

    private static final int LOWEST_SALARY = 30_000;
    private static final int SALARY_RANGE = 90_000;

    private final Random random;

    /// Creates a new [PersonData] instance.
    ///
    /// @param seed
    ///         the seed of the [Random] used to pick the values
    public PersonData(long seed) {
        random = new Random(seed);
    }

    /// Returns whether the person with the given id is an active employee.
    ///
    /// @param id
    ///         the unique id of the person
    /// @return the active flag, null when the person is not an employee at all
    public Boolean active(long id) {
        // Every 250th person is not an employee (null), and every fifth one left the company (false).
        return id % 250 == 0 ? null : random.nextInt(5) != 0;
    }

    /// Returns a random number within the given bounds.
    ///
    /// @param minimum
    ///         the smallest possible number, inclusive
    /// @param maximum
    ///         the largest possible number, inclusive
    /// @return the random number
    public int between(int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    /// Returns whether a one out of the given number chance came true.
    ///
    /// @param oneOutOf
    ///         the number of cases, e.g. 4 for a chance of 25 percent
    /// @return true when the chance came true
    public boolean chance(int oneOutOf) {
        return random.nextInt(oneOutOf) == 0;
    }

    /// Returns the notes of a child of the given person.
    ///
    /// @param parent
    ///         the parent of the child
    /// @return the notes
    public String childNotes(Person parent) {
        return "Child of " + parent.firstName() + " " + parent.lastName() + ".";
    }

    /// Returns a random department.
    ///
    /// @return the department
    public Department department() {
        Department[] departments = Department.values();
        return departments[random.nextInt(departments.length)];
    }

    /// Returns the email address of the person with the given name and id.
    ///
    /// @param firstName
    ///         the first name
    /// @param lastName
    ///         the last name
    /// @param id
    ///         the unique id of the person
    /// @return the email address
    public String email(String firstName, String lastName, long id) {
        String email = (firstName + "." + lastName + id + "@example.com").toLowerCase();

        // Every 100th email is invalid, so the column validation has something to complain about.
        return id % 100 == 0 ? email.replace("@", "") : email;
    }

    /// Returns a random first name.
    ///
    /// @return the first name
    public String firstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }

    /// Returns a random hire date.
    ///
    /// @return the hire date
    public LocalDate hireDate() {
        return FIRST_HIRE_DATE.plusDays(random.nextInt(HIRE_DATE_RANGE_DAYS));
    }

    /// Returns a random last name.
    ///
    /// @return the last name
    public String lastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }

    /// Returns random notes, which may be empty and may span multiple lines.
    ///
    /// @return the notes
    public String notes() {
        return NOTES[random.nextInt(NOTES.length)];
    }

    /// Returns a random yearly salary.
    ///
    /// @return the salary
    public double salary() {
        return LOWEST_SALARY + random.nextInt(SALARY_RANGE) + random.nextInt(100) / 100.0;
    }

}
