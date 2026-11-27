package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumnBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;

/// Tests scrolling horizontally to a column of the [ExtendedTableView] and [ExtendedTreeTableView].
///
/// @author Marius Hanl
class HorizontalScrollTest extends JavaFxTest {

    @DisplayName("Scrolling to a column shows it fully, between the fixed columns and the vertical scroll bar")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testScrollingToAColumnShowsItFully(TableType tableType) {
        runOnFxThread(() -> {
            List<TableColumnBase<?, ?>> columns = IntStream.range(0, 5)
                    .<TableColumnBase<?, ?>>mapToObj(i -> tableType.createColumn("Column " + i, "Value"))
                    .peek(column -> column.setPrefWidth(100)).toList();
            List<Row> rows = IntStream.range(0, 50).mapToObj(i -> new Row("" + i, "" + i, "" + i)).toList();
            Control table = tableType.createTable(columns, rows);
            tableType.setFixedColumnCount(table, 1);
            showInStage(table, 300, 200);

            tableType.scrollToColumn(table, columns.get(2));
            table.layout();
            assertFullyVisible(tableType, table, columns.getFirst(), columns.get(2));

            tableType.scrollToColumn(table, columns.get(4));
            table.layout();
            assertFullyVisible(tableType, table, columns.getFirst(), columns.get(4));

            tableType.scrollToColumn(table, columns.get(1));
            table.layout();
            assertFullyVisible(tableType, table, columns.getFirst(), columns.get(1));
        });
    }

    private static void assertFullyVisible(TableType tableType, Control table, TableColumnBase<?, ?> fixedColumn,
            TableColumnBase<?, ?> column) {
        Bounds fixedCell = cellBounds(tableType.getCell(table, 0, fixedColumn));
        ScrollBar vbar = table.lookupAll(".scroll-bar").stream().map(ScrollBar.class::cast)
                .filter(bar -> bar.getOrientation() == Orientation.VERTICAL).findFirst().orElseThrow();
        assertTrue(vbar.isVisible());

        Bounds cell = cellBounds(tableType.getCell(table, 0, column));
        assertTrue(cell.getMinX() >= fixedCell.getMaxX() - 0.5, () -> cell + " behind " + fixedCell);
        assertTrue(cell.getMaxX() <= vbar.localToScene(vbar.getLayoutBounds()).getMinX() + 0.5,
                () -> cell + " behind the vertical scroll bar");
    }

    private static Bounds cellBounds(Node cell) {
        return cell.localToScene(cell.getLayoutBounds());
    }
}
