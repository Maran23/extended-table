package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Labeled;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

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

    @DisplayName("A cell with a null value is not empty when its row item is added again")
    @ParameterizedTest
    @EnumSource(TableType.class)
    @SuppressWarnings("unchecked")
    void testCellWithNullValueIsNotEmptyWhenItsRowItemIsAddedAgain(TableType tableType) {
        runOnFxThread(() -> {
            Control table = tableType.createTable(List.of(tableType.createColumn("Column", null)));
            ObservableList<Object> items = (ObservableList<Object>) ((ExtendedTable<?>) table).getBackingItems();

            showInStage(table, 400, 300);

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

    @DisplayName("A row is snapped to whole pixels when its cells are not")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRowIsSnappedToWholePixelsWhenItsCellsAreNot(TableType tableType) {
        runOnFxThread(() -> {
            Control table = tableType.createTable(List.of(tableType.createColumn("Column", "Value", () -> {
                Region graphic = new Region();
                graphic.setPrefHeight(30.5);
                return graphic;
            })));

            showInStage(table, 400, 300);

            Region row = (Region) getLastRowCell(tableType, table).getParent();
            assertEquals(Math.ceil(row.getHeight()), row.getHeight());
        });
    }

    @DisplayName("A cell stays aligned center left in a row higher than 24 pixels")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCellStaysAlignedCenterLeftInAHighRow(TableType tableType) {
        runOnFxThread(() -> {
            Control table = tableType.createTable(List.of(tableType.createColumn("Column", "Value", () -> {
                Region graphic = new Region();
                graphic.setPrefHeight(40);
                return graphic;
            })));

            showInStage(table, 400, 300);

            assertEquals(Pos.CENTER_LEFT, ((Labeled) getLastRowCell(tableType, table)).getAlignment());
        });
    }

    @DisplayName("The text of an edited cell stays in place with the cell padding of a theme")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testEditedTextStaysInPlaceWithTheCellPaddingOfATheme(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> column = tableType.createStringColumn();
            Control table = tableType.createTable(List.of(column));
            tableType.setEditable(table, true);
            Stage stage = showInStage(table, 400, 300);
            stage.getScene().getStylesheets().add("data:text/css,.table-cell, .tree-table-cell { -fx-padding: 0 1em; }");
            table.applyCss();
            table.layout();

            edit(table, column);

            Node editedText = table.lookup(".cell-text-input .text");
            Node text = getLastRowCell(tableType, table).lookup(".text");
            assertEquals(text.localToScene(text.getLayoutBounds()).getMinX(),
                    editedText.localToScene(editedText.getLayoutBounds()).getMinX(), 1);
        });
    }

    @DisplayName("The editor fills the edited cell with its ring, over a selected cell styled by a theme, in the same height")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testEditorFillsTheEditedCellOverASelectedCellStyledByATheme(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> column = tableType.createStringColumn();
            Control table = tableType.createTable(List.of(column));
            tableType.setEditable(table, true);
            Stage stage = showInStage(table, 400, 300);
            stage.getScene().getStylesheets()
                    .add("data:text/css,.table-cell:selected, .tree-table-cell:selected { -fx-background-color: red; }");
            double height = getLastRowCell(tableType, table).getHeight();

            edit(table, column);

            Region editor = (Region) table.lookup(".cell-text-input");
            Region cell = (Region) editor.getParent();
            assertEquals(cell.getLayoutBounds(), editor.getBoundsInParent());
            assertEquals(2, editor.getBackground().getFills().size());
            assertEquals(height, cell.getHeight());
        });
    }

    /// Edits the first row with the cell selection, so the edited cell is selected.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void edit(Control table, TableColumnBase<?, ?> column) {
        switch (table) {
            case TableView tableView -> {
                tableView.getSelectionModel().setCellSelectionEnabled(true);
                tableView.getSelectionModel().select(0, (TableColumn) column);
                tableView.edit(0, (TableColumn) column);
            }
            case TreeTableView treeTableView -> {
                treeTableView.getSelectionModel().setCellSelectionEnabled(true);
                treeTableView.getSelectionModel().select(0, (TreeTableColumn) column);
                treeTableView.edit(0, (TreeTableColumn) column);
            }
            default -> throw new IllegalArgumentException();
        }
        table.applyCss();
        table.layout();
    }

    private IndexedCell<?> getLastRowCell(TableType tableType, Control table) {
        return table.lookupAll(tableType.getCellSelector()).stream().map(node -> (IndexedCell<?>) node)
                .filter(cell -> cell.getIndex() == 1).findFirst().orElseThrow();
    }
}
