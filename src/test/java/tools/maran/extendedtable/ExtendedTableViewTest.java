package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableCell;
import javafx.scene.shape.Rectangle;

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

    @DisplayName("A cell scrolled behind the fixed cells is clipped there until scrolled back, while the fixed cell is not")
    @Test
    void testACellScrolledBehindTheFixedCellsIsClippedUntilScrolledBack() {
        runOnFxThread(() -> {
            StringTableColumn<Row> fixedColumn = createColumn(Row::first);
            StringTableColumn<Row> scrolledColumn = createColumn(Row::second);
            StringTableColumn<Row> lastColumn = createColumn(Row::third);
            lastColumn.setPrefWidth(100);
            ExtendedTableView<Row> table = new ExtendedTableView<>(FXCollections.observableArrayList(rowA));
            table.getColumns().addAll(List.of(fixedColumn, scrolledColumn, lastColumn));
            table.setFixedColumnCount(1);
            showInStage(table, 300, 200);

            table.scrollToColumn(lastColumn);
            table.layout();

            Rectangle fixedClip = (Rectangle) getCell(table, fixedColumn).getClip();
            TableCell<?, ?> scrolledCell = getCell(table, scrolledColumn);
            Rectangle scrolledClip = (Rectangle) scrolledCell.getClip();
            assertEquals(0, fixedClip.getX());
            assertTrue(scrolledClip.getX() > 0 && scrolledClip.getX() < scrolledCell.getWidth());
            assertEquals(scrolledCell.getWidth() - scrolledClip.getX(), scrolledClip.getWidth());

            table.scrollToColumn(scrolledColumn);
            table.layout();

            assertSame(scrolledClip, scrolledCell.getClip());
            assertEquals(0, scrolledClip.getX());
            assertTrue(scrolledClip.widthProperty().isBound());
        });
    }

    private static StringTableColumn<Row> createColumn(Function<Row, String> readFunction) {
        StringTableColumn<Row> column = new StringTableColumn<>();
        column.setReadFunction(readFunction::apply);
        column.setPrefWidth(150);
        return column;
    }

    private static TableCell<?, ?> getCell(ExtendedTableView<Row> table, StringTableColumn<Row> column) {
        return table.lookupAll(".table-cell").stream().map(TableCell.class::cast)
                .filter(cell -> cell.getIndex() == 0 && cell.getTableColumn() == column).findFirst().orElseThrow();
    }
}
