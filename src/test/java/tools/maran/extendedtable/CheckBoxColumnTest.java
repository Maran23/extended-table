package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumnBase;

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
}
