package tools.maran.extendedtable.manual;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// Generates the [Person]s shown by the [TableSampler].
///
/// @author Marius Hanl
public final class PersonGenerator {

    private static final long FIRST_CHILD_ID = 1_000_001;

    private final PersonData data;

    private long nextId = 1;
    private long nextChildId = FIRST_CHILD_ID;

    /// Creates a new [PersonGenerator] instance which always generates the same data.
    public PersonGenerator() {
        data = new PersonData(new Random().nextLong());
    }

    /// Generates the given number of persons, some of them with children.
    ///
    /// The returned list only contains the persons of the first level, the children are only reachable via
    /// [Person#children()].
    ///
    /// @param count
    ///         the number of persons to generate
    /// @return the newly generated persons
    public List<Person> generate(int count) {
        List<Person> persons = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            boolean hasChildren = index % 8 == 0;

            Person person = createEmployee(nextId++, hasChildren ? data.between(45, 64) : data.between(20, 44), null);
            if (hasChildren) {
                createChildren(person);
            }

            persons.add(person);
        }

        return persons;
    }

    private void createChildren(Person parent) {
        int childCount = data.between(1, 3);

        for (int index = 0; index < childCount; index++) {
            Person child = createEmployee(nextChildId++, data.between(18, 40), parent.lastName());
            child.setNotes(data.childNotes(parent));
            parent.children().add(child);

            // Sometimes a child has a child on its own, so the tree is one level deeper.
            if (data.chance(4)) {
                child.children().add(createGrandChild(child));
            }
        }
    }

    private Person createEmployee(long id, int age, String lastName) {
        String firstName = data.firstName();
        String name = lastName != null ? lastName : data.lastName();

        return new Person(id, firstName, name, age, data.active(id), data.department(), data.salary(),
                data.email(firstName, name, id), data.hireDate(), data.notes());
    }

    private Person createGrandChild(Person parent) {
        long id = nextChildId++;

        return new Person(id, data.firstName(), parent.lastName(), data.between(1, 17), null, null, null, null, null,
                data.childNotes(parent));
    }

}
