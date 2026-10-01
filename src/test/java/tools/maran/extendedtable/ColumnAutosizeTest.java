package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;

/// Tests the column autosizing of the [ExtendedTableView] and [ExtendedTreeTableView].
///
/// @author Marius Hanl
class ColumnAutosizeTest extends JavaFxTest {

    private static final String LONG_VALUE = "A value that is much wider than the column";
    private static final double DEFAULT_COLUMN_WIDTH = 80;

    private Stage stage;

    @AfterEach
    void closeStage() {
        runOnFxThread(stage::close);
    }

    @DisplayName("Autosizing a nested column lays out its leaf headers without pixel gaps")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAutosizeLaysOutLeafHeadersWithoutGaps(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> firstColumn = tableType.createColumn("F", "V");
            TableColumnBase<?, ?> secondColumn = tableType.createColumn("S", "V");
            TableColumnBase<?, ?> nestedColumn = tableType.createNestedColumn(LONG_VALUE, firstColumn, secondColumn);

            Control table = tableType.createTable(List.of(nestedColumn));
            show(table);

            tableType.autosizeColumns(table);
            table.layout();

            TableColumnHeader firstHeader = getHeader(table, firstColumn);
            TableColumnHeader secondHeader = getHeader(table, secondColumn);
            assertEquals(firstHeader.getLayoutX() + firstHeader.getWidth(), secondHeader.getLayoutX());
        });
    }

    @DisplayName("Autosizing resizes a column to fit its content")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAutosizeResizesColumn(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> column = tableType.createColumn("C", LONG_VALUE);
            column.setPrefWidth(30);

            Control table = tableType.createTable(List.of(column));
            show(table);
            assertEquals(30, column.getWidth());

            tableType.autosizeColumns(table);

            assertWiderThan(DEFAULT_COLUMN_WIDTH, column);
        });
    }

    @DisplayName("Autosizing resizes the leaf columns of a nested column inside a nested column")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAutosizeResizesLeafColumnsOfDoublyNestedColumn(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> leafColumn = tableType.createColumn("Leaf", LONG_VALUE);
            leafColumn.setPrefWidth(30);
            TableColumnBase<?, ?> innerColumn = tableType.createNestedColumn("I", leafColumn);
            TableColumnBase<?, ?> outerColumn = tableType.createNestedColumn("O", innerColumn);

            Control table = tableType.createTable(List.of(outerColumn));
            show(table);
            assertEquals(30, leafColumn.getWidth());

            tableType.autosizeColumns(table);

            assertWiderThan(DEFAULT_COLUMN_WIDTH, leafColumn);
        });
    }

    @DisplayName("Autosizing takes the cell graphic into account")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAutosizeTakesCellGraphicIntoAccount(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> plainColumn = tableType.createColumn("C", "V");
            TableColumnBase<?, ?> graphicColumn = tableType.createColumn("C", "V", () -> new Label(LONG_VALUE));
            plainColumn.setPrefWidth(30);
            graphicColumn.setPrefWidth(30);

            Control table = tableType.createTable(List.of(plainColumn, graphicColumn));
            show(table);

            tableType.autosizeColumns(table);

            assertWiderThan(plainColumn.getWidth() + DEFAULT_COLUMN_WIDTH, graphicColumn);
        });
    }

    @DisplayName("Autosizing takes the column graphic into account")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAutosizeTakesGraphicIntoAccount(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> plainColumn = tableType.createColumn("C", "V");
            TableColumnBase<?, ?> graphicColumn = tableType.createColumn("C", "V");
            Label prefixLabel = new Label("Prefix");
            graphicColumn.setGraphic(prefixLabel);
            plainColumn.setPrefWidth(30);
            graphicColumn.setPrefWidth(30);

            Control table = tableType.createTable(List.of(plainColumn, graphicColumn));
            show(table);

            tableType.autosizeColumns(table);

            assertWiderThan(plainColumn.getWidth() + prefixLabel.getWidth(), graphicColumn);
        });
    }

    @DisplayName("A column is autosized when the table is shown")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testColumnIsAutosizedOnShow(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> column = tableType.createColumn("C", LONG_VALUE);

            show(tableType.createTable(List.of(column)));

            assertWiderThan(DEFAULT_COLUMN_WIDTH, column);
        });
    }

    @DisplayName("A nested column is autosized to its title when the table is shown")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testNestedColumnIsAutosizedOnShow(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> firstColumn = tableType.createColumn("F", "V");
            TableColumnBase<?, ?> secondColumn = tableType.createColumn("S", "V");
            TableColumnBase<?, ?> nestedColumn = tableType.createNestedColumn(LONG_VALUE, firstColumn, secondColumn);

            Control table = tableType.createTable(List.of(nestedColumn));
            show(table);

            // The nested column width is only synced on the next layout pass, so we check the leaf headers.
            TableColumnHeader firstHeader = getHeader(table, firstColumn);
            TableColumnHeader secondHeader = getHeader(table, secondColumn);
            double leafHeadersWidth = firstHeader.getWidth() + secondHeader.getWidth();
            assertTrue(leafHeadersWidth > 2 * DEFAULT_COLUMN_WIDTH, "Leaf headers width was " + leafHeadersWidth);
            assertEquals(firstHeader.getLayoutX() + firstHeader.getWidth(), secondHeader.getLayoutX());
        });
    }

    private static void assertWiderThan(double expectedMinWidth, TableColumnBase<?, ?> column) {
        assertTrue(column.getWidth() > expectedMinWidth,
                "Column width " + column.getWidth() + " is not wider than " + expectedMinWidth);
    }

    private TableColumnHeader getHeader(Control table, TableColumnBase<?, ?> column) {
        return table.lookupAll(".column-header").stream().filter(TableColumnHeader.class::isInstance)
                .map(TableColumnHeader.class::cast).filter(header -> header.getTableColumn() == column).findFirst()
                .orElseThrow();
    }

    private void show(Control table) {
        stage = new Stage();
        stage.setScene(new Scene(table, 800, 300));
        stage.show();
        table.layout();
    }
}
