package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.column.StringTableColumn;

/// Tests the [ExtendedTableView].
///
/// @author Marius Hanl
class ExtendedTableViewTest extends JavaFxTest {

    private final Row rowA = new Row("A", "1", "T8");
    private final Row rowB = new Row("B", "2", "T9");

    /// Rethrows an exception inside a listener, which JavaFX passes on to the uncaught exception handler instead.
    @BeforeEach
    void setUp() {
        runOnFxThread(() -> Thread.currentThread().setUncaughtExceptionHandler((_, throwable) -> {
            if (throwable instanceof RuntimeException exception) {
                throw exception;
            }
            throw new RuntimeException(throwable);
        }));
    }

    @AfterEach
    void tearDown() {
        runOnFxThread(() -> Thread.currentThread().setUncaughtExceptionHandler(null));
    }

    @DisplayName("Binding the items shows them as they are, without an error")
    @Test
    void testBindingTheItemsShowsThemAsTheyAre() {
        runOnFxThread(() -> {
            ExtendedTableView<Row> table = new ExtendedTableView<>();
            ObservableList<Row> rows = FXCollections.observableArrayList(rowA);

            table.itemsProperty().bind(new SimpleObjectProperty<>(rows));

            assertSame(rows, table.getItems());
        });
    }

    @DisplayName("Filtering never modifies or replaces the set items, which may be unmodifiable")
    @Test
    void testFilteringNeverModifiesOrReplacesTheSetItems() {
        runOnFxThread(() -> {
            StringTableColumn<Row> column = new StringTableColumn<>();
            column.setReadFunction(Row::first);
            ExtendedTableView<Row> table = new ExtendedTableView<>();
            table.getColumns().add(column);

            ObservableList<Row> rows = FXCollections.unmodifiableObservableList(
                    FXCollections.observableArrayList(rowA, rowB));
            table.setItems(rows);
            ObservableList<Row> items = table.getItems();

            table.filter(column, List.of(rowA));
            assertEquals(List.of(rowA), table.getItems());

            table.restoreBackingItems();
            assertEquals(List.of(rowA, rowB), table.getItems());

            assertSame(items, table.getItems());
            assertEquals(List.of(rowA, rowB), rows);
        });
    }

    @DisplayName("Setting items copies them without notifying a change listener, as the own items stay the value")
    @Test
    void testSettingItemsCopiesThemWithoutNotifyingAChangeListener() {
        runOnFxThread(() -> {
            ExtendedTableView<Row> table = new ExtendedTableView<>();
            ObservableList<Row> items = table.getItems();
            List<ObservableList<Row>> newValues = new ArrayList<>();
            table.itemsProperty().addListener((_, _, newValue) -> newValues.add(newValue));

            table.setItems(FXCollections.observableArrayList(rowA, rowB));

            assertEquals(List.of(), newValues);
            assertSame(items, table.getItems());
            assertEquals(List.of(rowA, rowB), items);
        });
    }
}
