package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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

    /// JavaFX keeps the old row item of an empty cell and therefore skips the update when the same row item
    /// is added again at the same index and the cell value did not change (`null`).
    @DisplayName("A cell with a null value is not empty when its row item is added again")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCellWithNullValueIsNotEmptyWhenItsRowItemIsAddedAgain(TableType tableType) {
        runOnFxThread(() -> {
            CellTable cellTable = tableType.create();
            Control table = cellTable.table();

            stage = new Stage();
            stage.setScene(new Scene(table, 400, 300));
            stage.show();
            table.layout();

            IndexedCell<?> cell = getLastRowCell(cellTable);
            assertFalse(cell.isEmpty());

            cellTable.removeLastRow().run();
            table.layout();
            cell = getLastRowCell(cellTable);
            assertTrue(cell.isEmpty());

            cellTable.addLastRow().run();
            table.layout();
            cell = getLastRowCell(cellTable);
            assertFalse(cell.isEmpty());
        });
    }

    private IndexedCell<?> getLastRowCell(CellTable cellTable) {
        return cellTable.table().lookupAll(cellTable.cellSelector()).stream()
                .map(node -> (IndexedCell<?>) node)
                .filter(cell -> cell.getIndex() == 1)
                .findFirst()
                .orElseThrow();
    }

    private record CellTable(Control table, String cellSelector, Runnable removeLastRow, Runnable addLastRow) { }

    private enum TableType {
        TABLE_VIEW {
            @Override
            CellTable create() {
                TableColumn<String, String> column = new TableColumn<>("Column");
                column.setCellValueFactory(_ -> new SimpleObjectProperty<>(null));
                column.setCellFactory(_ -> new ExtendedTableCell<>());

                String lastRow = "Last";
                TableView<String> table = new TableView<>();
                table.getColumns().add(column);
                table.getItems().addAll("First", lastRow);

                return new CellTable(table, ".table-cell", () -> table.getItems().remove(lastRow),
                        () -> table.getItems().add(lastRow));
            }
        }, TREE_TABLE_VIEW {
            @Override
            CellTable create() {
                TreeTableColumn<String, String> column = new TreeTableColumn<>("Column");
                column.setCellValueFactory(_ -> new SimpleObjectProperty<>(null));
                column.setCellFactory(_ -> new ExtendedTreeTableCell<>());

                TreeItem<String> lastRow = new TreeItem<>("Last");
                TreeItem<String> root = new TreeItem<>();
                root.getChildren().addAll(new TreeItem<>("First"), lastRow);

                TreeTableView<String> table = new TreeTableView<>(root);
                table.setShowRoot(false);
                table.getColumns().add(column);

                return new CellTable(table, ".tree-table-cell", () -> root.getChildren().remove(lastRow),
                        () -> root.getChildren().add(lastRow));
            }
        };

        abstract CellTable create();
    }
}
