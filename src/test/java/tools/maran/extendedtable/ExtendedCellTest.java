package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;
import tools.maran.extendedtable.treetableview.cell.ExtendedTreeTableCell;

/// Tests the [ExtendedTableCell] and [ExtendedTreeTableCell].
///
/// @author Marius Hanl
class ExtendedCellTest extends JavaFxTest {

    private Stage stage;

    @AfterEach
    void closeStage() {
        runOnFxThread(stage::close);
    }

    @DisplayName("A cell with a null value is not empty when its row item is added again")
    @ParameterizedTest
    @EnumSource(TableType.class)
    @SuppressWarnings("unchecked")
    void testCellWithNullValueIsNotEmptyWhenItsRowItemIsAddedAgain(TableType tableType) {
        runOnFxThread(() -> {
            Control table = tableType.createTable(List.of(tableType.createColumn("Column", null)));
            ObservableList<Object> items = (ObservableList<Object>) ((ExtendedTable<?>) table).getBackingItems();

            stage = new Stage();
            stage.setScene(new Scene(table, 400, 300));
            stage.show();
            table.layout();

            IndexedCell<?> cell = getLastRowCell(tableType, table);
            assertFalse(cell.isEmpty());

            Object lastRow = items.removeLast();
            table.layout();
            cell = getLastRowCell(tableType, table);
            assertTrue(cell.isEmpty());

            items.add(lastRow);
            table.layout();
            cell = getLastRowCell(tableType, table);
            assertFalse(cell.isEmpty());
        });
    }

    private IndexedCell<?> getLastRowCell(TableType tableType, Control table) {
        return table.lookupAll(tableType.getCellSelector()).stream().map(node -> (IndexedCell<?>) node)
                .filter(cell -> cell.getIndex() == 1).findFirst().orElseThrow();
    }
}
