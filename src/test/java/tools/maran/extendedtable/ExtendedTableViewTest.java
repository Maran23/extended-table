package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.column.CheckBoxTableColumn;

/// Tests the [ExtendedTableView].
///
/// @author Marius Hanl
class ExtendedTableViewTest extends JavaFxTest {

    private ExtendedTableView<Row> table;
    private CheckBoxTableColumn<Row> checkBoxColumn;

    private Row rowA;
    private Row rowB;
    private Row rowC;

    @BeforeEach
    void setUp() {
        runOnFxThread(() -> {
            rowA = new Row("false", "A", null);
            rowB = new Row("false", "B", null);
            rowC = new Row("false", "C", null);

            checkBoxColumn = new CheckBoxTableColumn<>();
            checkBoxColumn.setReadFunction(row -> Boolean.valueOf(row.first()));
            checkBoxColumn.setWriteFunction((row, value) -> row.setFirst(value.toString()));

            table = new ExtendedTableView<>();
            table.getColumns().add(checkBoxColumn);
            table.setItems(FXCollections.observableArrayList(List.of(rowA, rowB, rowC)));
        });
    }

    @DisplayName("Selecting the header checkbox selects all items")
    @Test
    void testHeaderCheckBoxSelectsAllItems() {
        runOnFxThread(() -> {
            table.setEditable(true);

            CheckBox checkBox = (CheckBox) checkBoxColumn.getGraphic();
            rowA.setFirst("true");
            checkBoxColumn.refreshFilter();
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
    @Test
    void testNullItemsDeselectTheHeaderCheckBox() {
        runOnFxThread(() -> {
            rowA.setFirst("true");
            checkBoxColumn.refreshFilter();

            table.setItems(null);

            CheckBox checkBox = (CheckBox) checkBoxColumn.getGraphic();
            assertFalse(checkBox.isSelected());
            assertFalse(checkBox.isIndeterminate());
        });
    }
}
