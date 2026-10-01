package tools.maran.extendedtable.manual;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import tools.maran.extendedtable.manual.Person.Department;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.cell.ComboBoxTableCell;
import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;
import tools.maran.extendedtable.tableview.cell.GenericTextAreaTableCell;
import tools.maran.extendedtable.tableview.column.CheckBoxTableColumn;
import tools.maran.extendedtable.tableview.column.DoubleTableColumn;
import tools.maran.extendedtable.tableview.column.GenericTableColumn;
import tools.maran.extendedtable.tableview.column.IntegerTableColumn;
import tools.maran.extendedtable.tableview.column.LongTableColumn;
import tools.maran.extendedtable.tableview.column.StringTableColumn;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.cell.ComboBoxTreeTableCell;
import tools.maran.extendedtable.treetableview.cell.ExtendedTreeTableCell;
import tools.maran.extendedtable.treetableview.cell.GenericTextAreaTreeTableCell;
import tools.maran.extendedtable.treetableview.column.CheckBoxTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.DoubleTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.GenericTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.IntegerTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.LongTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.StringTreeTableColumn;

/// Manual test which shows the [ExtendedTableView] and the [ExtendedTreeTableView] side by side, each one inside its
/// own tab, so both tables can be tried out by hand with a realistic amount of data.
///
/// @author Marius Hanl
public final class TableSampler {

    private static final int ITEM_COUNT = 25_000;
    private static final double FIXED_CELL_SIZE = 24;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final PersonGenerator GENERATOR = new PersonGenerator();

    private TableSampler() {
        // noop.
    }

    /// Starts the JavaFX toolkit and shows the sampler.
    static void main() {
        Platform.startup(TableSampler::showStage);
    }

    private static <T> void add(ExtendedTable<T> table, int index, T item) {
        List<T> items = table.getItems();
        List<T> backingItems = table.getBackingItems();
        int backingIndex = index < items.size() ? backingItems.indexOf(items.get(index))
                : backingItems.indexOf(items.getLast()) + 1;
        backingItems.add(backingIndex, item);
    }

    private static <T> MenuButton createActionsButton(ExtendedTable<T> table, IntFunction<List<T>> generator) {
        Supplier<T> newItem = () -> generator.apply(1).getFirst();
        return new MenuButton("Actions", null, createMenuItem("Regenerate",
                () -> table.setItems(FXCollections.observableArrayList(generator.apply(ITEM_COUNT)))),
                new SeparatorMenuItem(), createMenuItem("Add first", () -> add(table, 0, newItem.get())),
                createMenuItem("Add middle", () -> add(table, table.getItems().size() / 2, newItem.get())),
                createMenuItem("Add last", () -> add(table, table.getItems().size(), newItem.get())),
                new SeparatorMenuItem(), createMenuItem("Remove first", () -> remove(table, 0)),
                createMenuItem("Remove middle", () -> remove(table, table.getItems().size() / 2)),
                createMenuItem("Remove last", () -> remove(table, table.getItems().size() - 1)));
    }

    private static CheckBoxTableColumn<Person> createActiveColumn() {
        CheckBoxTableColumn<Person> column = new CheckBoxTableColumn<>("Active");
        column.setReadFunction(Person::active);
        column.setWriteFunction(Person::setActive);
        return column;
    }

    private static IntegerTableColumn<Person> createAgeColumn() {
        IntegerTableColumn<Person> column = new IntegerTableColumn<>("Age");
        column.setReadFunction(Person::age);
        column.setWriteFunction(Person::setAge);
        column.setValidator(TableSampler::isValidAge);
        return column;
    }

    private static Button createButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setOnAction(_ -> action.run());
        return button;
    }

    private static GenericTableColumn<Person, Department> createDepartmentColumn() {
        GenericTableColumn<Person, Department> column = new GenericTableColumn<>("Department") {

            @Override
            protected ExtendedTableCell<Person, Department> createTableCell() {
                ComboBoxTableCell<Person, Department> cell = new ComboBoxTableCell<>(departments());
                cell.setReadFunction(TableSampler::departmentLabel);
                return cell;
            }
        };
        column.setReadFunction(Person::department);
        column.setWriteFunction(Person::setDepartment);
        column.setToStringConverter(TableSampler::departmentLabel);
        column.setFromStringConverter(Department::fromLabel);
        return column;
    }

    private static StringTableColumn<Person> createEmailColumn() {
        StringTableColumn<Person> column = new StringTableColumn<>("Email");
        column.setReadFunction(Person::email);
        column.setWriteFunction(Person::setEmail);
        column.setValidator(TableSampler::isValidEmail);
        return column;
    }

    private static StringTableColumn<Person> createFirstNameColumn() {
        StringTableColumn<Person> column = new StringTableColumn<>("First Name");
        column.setReadFunction(Person::firstName);
        column.setWriteFunction(Person::setFirstName);
        column.setValidator(TableSampler::isNotBlank);
        return column;
    }

    private static CheckMenuItem createFixedCellSizeToggle(DoubleProperty fixedCellSize) {
        CheckMenuItem menuItem = createToggle("Fixed cell size",
                selected -> fixedCellSize.set(selected ? FIXED_CELL_SIZE : 0));
        menuItem.setSelected(true);
        return menuItem;
    }

    private static GenericTableColumn<Person, LocalDate> createHireDateColumn() {
        GenericTableColumn<Person, LocalDate> column = new GenericTableColumn<>("Hire Date");
        column.setReadFunction(Person::hireDate);
        column.setWriteFunction(Person::setHireDate);
        column.setToStringConverter(TableSampler::formatDate);
        column.setFromStringConverter(TableSampler::parseDate);
        return column;
    }

    private static LongTableColumn<Person> createIdColumn() {
        LongTableColumn<Person> column = new LongTableColumn<>("ID");
        column.setReadFunction(Person::id);
        column.setEditable(false);
        return column;
    }

    private static StringTableColumn<Person> createLastNameColumn() {
        StringTableColumn<Person> column = new StringTableColumn<>("Last Name");
        column.setReadFunction(Person::lastName);
        column.setWriteFunction(Person::setLastName);
        column.setValidator(TableSampler::isNotBlank);
        return column;
    }

    private static MenuItem createMenuItem(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setOnAction(_ -> action.run());
        return menuItem;
    }

    private static TableColumn<Person, String> createNameColumn() {
        TableColumn<Person, String> column = new TableColumn<>("Name");
        column.getColumns().addAll(createFirstNameColumn(), createLastNameColumn());
        return column;
    }

    private static StringTableColumn<Person> createNotesColumn() {
        StringTableColumn<Person> column = new StringTableColumn<>("Notes") {

            @Override
            protected ExtendedTableCell<Person, String> createTableCell() {
                GenericTextAreaTableCell<Person, String> cell = new GenericTextAreaTableCell<>();
                cell.setReadFunction(this::convertToString);
                cell.setWriteFunction(this::convertFromString);
                return cell;
            }
        };
        column.setReadFunction(Person::notes);
        column.setWriteFunction(Person::setNotes);
        return column;
    }

    private static DoubleTableColumn<Person> createSalaryColumn() {
        DoubleTableColumn<Person> column = new DoubleTableColumn<>("Salary");
        column.setReadFunction(Person::salary);
        column.setWriteFunction(Person::setSalary);
        column.setValidator(TableSampler::isValidSalary);
        return column;
    }

    private static Node createSpinner(String text, IntegerProperty property, int max) {
        Spinner<Integer> spinner = new Spinner<>(0, max, property.get());
        spinner.setPrefWidth(70);
        spinner.valueProperty().addListener((_, _, value) -> property.set(value));

        Label label = new Label(text);
        label.setLabelFor(spinner);

        HBox spinnerBox = new HBox(4, label, spinner);
        spinnerBox.setAlignment(Pos.CENTER);

        return spinnerBox;
    }

    private static Region createTableView() {
        ExtendedTableView<Person> tableView = new ExtendedTableView<>();
        tableView.setEditable(true);
        tableView.setHeaderText("Persons");
        tableView.setFixedColumnCount(2);

        tableView.getColumns().addAll(createIdColumn(), createNameColumn(), createAgeColumn(), createActiveColumn(),
                createDepartmentColumn(), createSalaryColumn(), createEmailColumn(), createHireDateColumn(),
                createNotesColumn());

        tableView.getColumns().forEach(column -> column.setSortable(true));

        tableView.setItems(FXCollections.observableArrayList(GENERATOR.generate(ITEM_COUNT)));
        tableView.setValidationEnabled(true);

        MenuButton options = new MenuButton("Options", null, createToggle("Editable", tableView.editableProperty()),
                createToggle("Validation", tableView.validationEnabledProperty()),
                createFixedCellSizeToggle(tableView.fixedCellSizeProperty()));
        tableView.getHeaderButtons().addAll(options,
                createSpinner("Fixed columns", tableView.fixedColumnCountProperty(), tableView.getColumns().size()),
                createButton("Autosize columns", tableView::autosizeColumns),
                createActionsButton(tableView, GENERATOR::generate));

        return tableView;
    }

    private static CheckMenuItem createToggle(String text, BooleanProperty property) {
        CheckMenuItem menuItem = new CheckMenuItem(text);
        menuItem.setSelected(property.get());

        property.bindBidirectional(menuItem.selectedProperty());

        return menuItem;
    }

    private static CheckMenuItem createToggle(String text, Consumer<Boolean> onSelectionChanged) {
        CheckMenuItem menuItem = new CheckMenuItem(text);
        menuItem.selectedProperty().addListener((_, _, selected) -> onSelectionChanged.accept(selected));
        return menuItem;
    }

    private static CheckBoxTreeTableColumn<Person> createTreeActiveColumn() {
        CheckBoxTreeTableColumn<Person> column = new CheckBoxTreeTableColumn<>("Active");
        column.setReadFunction(Person::active);
        column.setWriteFunction(Person::setActive);
        return column;
    }

    private static IntegerTreeTableColumn<Person> createTreeAgeColumn() {
        IntegerTreeTableColumn<Person> column = new IntegerTreeTableColumn<>("Age");
        column.setReadFunction(Person::age);
        column.setWriteFunction(Person::setAge);
        column.setValidator(TableSampler::isValidAge);
        return column;
    }

    private static GenericTreeTableColumn<Person, Department> createTreeDepartmentColumn() {
        GenericTreeTableColumn<Person, Department> column = new GenericTreeTableColumn<>("Department") {

            @Override
            protected ExtendedTreeTableCell<Person, Department> createTreeTableCell() {
                ComboBoxTreeTableCell<Person, Department> cell = new ComboBoxTreeTableCell<>(departments());
                cell.setReadFunction(TableSampler::departmentLabel);
                return cell;
            }
        };
        column.setReadFunction(Person::department);
        column.setWriteFunction(Person::setDepartment);
        column.setToStringConverter(TableSampler::departmentLabel);
        column.setFromStringConverter(Department::fromLabel);
        return column;
    }

    private static StringTreeTableColumn<Person> createTreeEmailColumn() {
        StringTreeTableColumn<Person> column = new StringTreeTableColumn<>("Email");
        column.setReadFunction(Person::email);
        column.setWriteFunction(Person::setEmail);
        column.setValidator(TableSampler::isValidEmail);
        return column;
    }

    private static StringTreeTableColumn<Person> createTreeFirstNameColumn() {
        StringTreeTableColumn<Person> column = new StringTreeTableColumn<>("First Name");
        column.setReadFunction(Person::firstName);
        column.setWriteFunction(Person::setFirstName);
        column.setValidator(TableSampler::isNotBlank);
        return column;
    }

    private static GenericTreeTableColumn<Person, LocalDate> createTreeHireDateColumn() {
        GenericTreeTableColumn<Person, LocalDate> column = new GenericTreeTableColumn<>("Hire Date");
        column.setReadFunction(Person::hireDate);
        column.setWriteFunction(Person::setHireDate);
        column.setToStringConverter(TableSampler::formatDate);
        column.setFromStringConverter(TableSampler::parseDate);
        return column;
    }

    private static LongTreeTableColumn<Person> createTreeIdColumn() {
        LongTreeTableColumn<Person> column = new LongTreeTableColumn<>("ID");
        column.setReadFunction(Person::id);
        column.setEditable(false);
        return column;
    }

    private static TreeItem<Person> createTreeItem(Person person) {
        TreeItem<Person> treeItem = new TreeItem<>(person);

        for (Person child : person.children()) {
            treeItem.getChildren().add(createTreeItem(child));
        }
        treeItem.setExpanded(!treeItem.getChildren().isEmpty());

        return treeItem;
    }

    private static StringTreeTableColumn<Person> createTreeLastNameColumn() {
        StringTreeTableColumn<Person> column = new StringTreeTableColumn<>("Last Name");
        column.setReadFunction(Person::lastName);
        column.setWriteFunction(Person::setLastName);
        column.setValidator(TableSampler::isNotBlank);
        return column;
    }

    private static TreeTableColumn<Person, String> createTreeNameColumn() {
        TreeTableColumn<Person, String> column = new TreeTableColumn<>("Name");
        column.getColumns().addAll(createTreeFirstNameColumn(), createTreeLastNameColumn());
        return column;
    }

    private static StringTreeTableColumn<Person> createTreeNotesColumn() {
        StringTreeTableColumn<Person> column = new StringTreeTableColumn<>("Notes") {

            @Override
            protected ExtendedTreeTableCell<Person, String> createTreeTableCell() {
                GenericTextAreaTreeTableCell<Person, String> cell = new GenericTextAreaTreeTableCell<>();
                cell.setReadFunction(this::convertToString);
                cell.setWriteFunction(this::convertFromString);
                return cell;
            }
        };
        column.setReadFunction(Person::notes);
        column.setWriteFunction(Person::setNotes);
        return column;
    }

    private static DoubleTreeTableColumn<Person> createTreeSalaryColumn() {
        DoubleTreeTableColumn<Person> column = new DoubleTreeTableColumn<>("Salary");
        column.setReadFunction(Person::salary);
        column.setWriteFunction(Person::setSalary);
        column.setValidator(TableSampler::isValidSalary);
        return column;
    }

    private static Region createTreeTableView() {
        ExtendedTreeTableView<Person> treeTableView = new ExtendedTreeTableView<>();
        treeTableView.setEditable(true);
        treeTableView.setHeaderText("Persons");
        treeTableView.setFixedColumnCount(2);
        treeTableView.setShowTreeLines(true);

        treeTableView.getColumns()
                .addAll(createTreeIdColumn(), createTreeNameColumn(), createTreeAgeColumn(), createTreeActiveColumn(),
                        createTreeDepartmentColumn(), createTreeSalaryColumn(), createTreeEmailColumn(),
                        createTreeHireDateColumn(), createTreeNotesColumn());

        treeTableView.getColumns().forEach(column -> column.setSortable(true));

        IntFunction<List<TreeItem<Person>>> treeItemGenerator = count -> GENERATOR.generate(count).stream()
                .map(TableSampler::createTreeItem).toList();
        treeTableView.setItems(FXCollections.observableArrayList(treeItemGenerator.apply(ITEM_COUNT)));
        treeTableView.setValidationEnabled(true);

        MenuButton options = new MenuButton("Options", null, createToggle("Editable", treeTableView.editableProperty()),
                createToggle("Validation", treeTableView.validationEnabledProperty()),
                createToggle("Tree lines", treeTableView.showTreeLinesProperty()),
                createFixedCellSizeToggle(treeTableView.fixedCellSizeProperty()));
        treeTableView.getHeaderButtons().addAll(options,
                createSpinner("Fixed columns", treeTableView.fixedColumnCountProperty(),
                        treeTableView.getColumns().size()),
                createButton("Expand all", () -> setExpanded(treeTableView, true)),
                createButton("Collapse all", () -> setExpanded(treeTableView, false)),
                createButton("Autosize columns", treeTableView::autosizeColumns),
                createActionsButton(treeTableView, treeItemGenerator));

        return treeTableView;
    }

    private static String departmentLabel(Department department) {
        return department == null ? null : department.label();
    }

    private static ObservableList<Department> departments() {
        return FXCollections.observableArrayList(Department.values());
    }

    private static String formatDate(LocalDate date) {
        return date == null ? null : DATE_FORMATTER.format(date);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isValidAge(Integer age) {
        return age != null && age >= 0 && age <= 120;
    }

    private static boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    private static boolean isValidSalary(Double salary) {
        return salary == null || salary >= 0;
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(text, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /// Removes the visible item at the given index from the backing items.
    private static <T> void remove(ExtendedTable<T> table, int index) {
        table.getBackingItems().remove(table.getItems().get(index));
    }

    private static void setExpanded(ExtendedTreeTableView<Person> treeTableView, boolean expanded) {
        for (TreeItem<Person> treeItem : treeTableView.getAllTreeItems()) {
            if (!treeItem.getChildren().isEmpty()) {
                treeItem.setExpanded(expanded);
            }
        }

        // The root is never shown, so it always needs to stay expanded.
        treeTableView.getRoot().setExpanded(true);
    }

    private static void showStage() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(new Tab("ExtendedTableView", createTableView()),
                new Tab("ExtendedTreeTableView", createTreeTableView()));

        Stage stage = new Stage();
        stage.setTitle("Extended-Table Sampler");
        stage.setScene(new Scene(tabPane, 800, 600));
        stage.show();
    }

}
