package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.column.CheckBoxTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.StringTreeTableColumn;

/// Tests the [ExtendedTreeTableView].
///
/// @author Marius Hanl
class ExtendedTreeTableViewTest extends JavaFxTest {

    private ExtendedTreeTableView<Row> table;
    private StringTreeTableColumn<Row> column;

    private TreeItem<Row> itemA;
    private TreeItem<Row> itemA1;
    private TreeItem<Row> itemB;

    @BeforeEach
    void setUp() {
        runOnFxThread(() -> {
            itemA = new TreeItem<>(new Row("false", "A", null));
            itemA1 = new TreeItem<>(new Row("false", "A1", null));
            itemB = new TreeItem<>(new Row("false", "B", null));
            itemA.getChildren().add(itemA1);
            itemA.setExpanded(true);

            column = new StringTreeTableColumn<>();
            column.setReadFunction(Row::second);

            table = new ExtendedTreeTableView<>();
            table.getColumns().add(column);
            table.setItems(FXCollections.observableArrayList(List.of(itemA, itemB)));
        });
    }

    @DisplayName("All tree items contain the children of added items and stay complete when filtered")
    @Test
    void testAllTreeItemsContainChildrenAndStayCompleteWhenFiltered() {
        runOnFxThread(() -> {
            assertAllTreeItems(List.of(itemA, itemA1, itemB));

            table.filter(column, List.of(itemA));
            assertAllTreeItems(List.of(itemA, itemA1));

            table.restoreBackingItems();
            assertAllTreeItems(List.of(itemA, itemA1, itemB));
        });
    }

    @DisplayName("Selecting the header checkbox selects all top level items, even when children are expanded")
    @Test
    void testHeaderCheckBoxSelectsAllTopLevelItems() {
        runOnFxThread(() -> {
            CheckBoxTreeTableColumn<Row> checkBoxColumn = new CheckBoxTreeTableColumn<>();
            checkBoxColumn.setReadFunction(row -> Boolean.valueOf(row.first()));
            checkBoxColumn.setWriteFunction((row, value) -> row.setFirst(value.toString()));
            table.getColumns().add(checkBoxColumn);

            CheckBox checkBox = (CheckBox) checkBoxColumn.getGraphic();
            itemB.getValue().setFirst("true");
            checkBoxColumn.refreshFilter();
            assertTrue(checkBox.isIndeterminate());

            checkBox.setIndeterminate(false);
            checkBox.setSelected(true);

            assertEquals("true", itemA.getValue().first());
            assertEquals("false", itemA1.getValue().first());
            assertEquals("true", itemB.getValue().first());
        });
    }

    @DisplayName("Scrolling to an item scrolls to its row")
    @Test
    void testScrollToItem() {
        runOnFxThread(() -> {
            // The scroll is only performed once the table has a skin.
            var _ = new Scene(table);
            table.applyCss();

            AtomicInteger scrolledRow = new AtomicInteger(-1);
            table.setOnScrollTo(event -> scrolledRow.set(event.getScrollTarget()));

            table.scrollTo(itemB.getValue());

            assertEquals(2, scrolledRow.get());
        });
    }

    private void assertAllTreeItems(List<TreeItem<Row>> treeItems) {
        assertEquals(treeItems.size(), table.getAllTreeItems().size());
        assertTrue(table.getAllTreeItems().containsAll(treeItems));
    }
}
