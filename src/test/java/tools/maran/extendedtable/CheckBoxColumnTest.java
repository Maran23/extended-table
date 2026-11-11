package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Labeled;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.tableview.column.CheckBoxTableColumn;
import tools.maran.extendedtable.treetableview.column.CheckBoxTreeTableColumn;

/// Tests the [CheckBoxTableColumn] and [CheckBoxTreeTableColumn].
///
/// @author Marius Hanl
class CheckBoxColumnTest extends JavaFxTest {

    @DisplayName("Selecting the header checkbox selects all items")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testHeaderCheckBoxSelectsAllItems(TableType tableType) {
        runOnFxThread(() -> {
            Row rowA = new Row("false", "A", null);
            Row rowB = new Row("false", "B", null);
            Row rowC = new Row("false", "C", null);
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            Control table = tableType.createTable(List.of(checkBoxColumn), List.of(rowA, rowB, rowC));
            tableType.setEditable(table, true);

            CheckBox checkBox = (CheckBox) checkBoxColumn.getGraphic();
            rowA.setFirst("true");
            ((ExtendedTable<?>) table).refreshColumnFilters();
            assertTrue(checkBox.isIndeterminate());

            checkBox.setSelected(false);
            checkBox.fire();

            assertEquals("true", rowA.first());
            assertEquals("true", rowB.first());
            assertEquals("true", rowC.first());
            assertTrue(checkBox.isSelected());
        });
    }

    @DisplayName("Null items deselect the header checkbox")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testNullItemsDeselectTheHeaderCheckBox(TableType tableType) {
        runOnFxThread(() -> {
            Row rowA = new Row("true", "A", null);
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            ExtendedTable<?> table = (ExtendedTable<?>) tableType.createTable(List.of(checkBoxColumn),
                    List.of(rowA, new Row("false", "B", null)));
            table.refreshColumnFilters();

            table.setItems(null);

            CheckBox checkBox = (CheckBox) checkBoxColumn.getGraphic();
            assertFalse(checkBox.isSelected());
            assertFalse(checkBox.isIndeterminate());
        });
    }

    @DisplayName("A checkbox cell is styled like the one of JavaFX, centered without padding")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCheckBoxCellIsStyledLikeTheOneOfJavaFx(TableType tableType) {
        runOnFxThread(() -> {
            Control table = tableType.createTable(List.of(tableType.createCheckBoxColumn()));

            showInStage(table, 400, 300);

            Labeled checkBoxCell = (Labeled) table.lookup(tableType.getCellSelector());
            assertEquals(Pos.CENTER, checkBoxCell.getAlignment());
            assertEquals(Insets.EMPTY, checkBoxCell.getPadding());
        });
    }

    @DisplayName("Clicking the checkbox of a cell selects only its row, like a click on the cell")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testClickingTheCheckBoxSelectsOnlyItsRow(TableType tableType) {
        runOnFxThread(() -> {
            Row rowC = new Row("false", "C", null);
            Control table = tableType.createTable(List.of(tableType.createCheckBoxColumn()),
                    List.of(new Row("false", "A", null), new Row("false", "B", null), rowC));
            tableType.setEditable(table, true);
            showInStage(table, 400, 300);
            TableSelectionModel<?> selectionModel = ((ExtendedTable<?>) table).getSelectionModel();
            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
            selectionModel.select(0);

            CheckBox checkBox = (CheckBox) table.lookupAll(".check-box").stream()
                    .filter(node -> node.getParent() instanceof IndexedCell<?> cell && cell.getIndex() == 2)
                    .findFirst().orElseThrow();
            click(checkBox);

            assertEquals(List.of(2), selectionModel.getSelectedIndices());
            assertEquals("true", rowC.first());
        });
    }

    private static void click(CheckBox checkBox) {
        Point2D center = checkBox.localToScene(checkBox.getWidth() / 2, checkBox.getHeight() / 2);
        for (EventType<MouseEvent> type : List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_RELEASED)) {
            Event.fireEvent(checkBox, new MouseEvent(type, center.getX(), center.getY(), center.getX(), center.getY(),
                    MouseButton.PRIMARY, 1, false, false, false, false, type == MouseEvent.MOUSE_PRESSED, false, false,
                    true, false, true, null));
        }
    }
}
