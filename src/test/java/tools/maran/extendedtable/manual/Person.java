package tools.maran.extendedtable.manual;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// The model used in the table sampler.
///
/// @author Marius Hanl
public final class Person {

    private final long id;
    private String firstName;
    private String lastName;
    private Integer age;
    private Boolean active;
    private Department department;
    private Double salary;
    private String email;
    private LocalDate hireDate;
    private String notes;

    private List<Person> children;

    public Person(long id, String firstName, String lastName, Integer age, Boolean active, Department department,
            Double salary, String email, LocalDate hireDate, String notes) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.active = active;
        this.department = department;
        this.salary = salary;
        this.email = email;
        this.hireDate = hireDate;
        this.notes = notes;
    }

    public Boolean active() {
        return active;
    }

    public Integer age() {
        return age;
    }

    public List<Person> children() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public Department department() {
        return department;
    }

    public String email() {
        return email;
    }

    public String firstName() {
        return firstName;
    }

    public LocalDate hireDate() {
        return hireDate;
    }

    /// Returns the unique id. The id is the only value which can not be edited.
    ///
    /// @return the id
    public long id() {
        return id;
    }

    public String lastName() {
        return lastName;
    }

    public String notes() {
        return notes;
    }

    public Double salary() {
        return salary;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    @Override
    public String toString() {
        return "Person[" + id + ", " + firstName + " " + lastName + "]";
    }

    public enum Department {

        DEVELOPMENT("Development"),
        SALES("Sales"),
        MARKETING("Marketing"),
        SUPPORT("Support"),
        FINANCE("Finance"),
        HUMAN_RESOURCES("Human Resources");

        private static final Map<String, Department> BY_LABEL = Arrays.stream(values())
                .collect(Collectors.toMap(Department::label, Function.identity()));

        private final String label;

        Department(String label) {
            this.label = label;
        }

        public static Department fromLabel(String label) {
            return BY_LABEL.get(label);
        }

        public String label() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

}
