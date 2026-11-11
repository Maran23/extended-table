package tools.maran.extendedtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.shadfx.Palette;
import tools.maran.shadfx.Theme;

/// Tests the TAB navigation, the keyboard editing of the cells and the copy and paste of cell text.
///
/// @author Marius Hanl
class CellNavigationTest extends JavaFxTest {

    /// `-fx-focus-color` of Modena.
    private static final Color MODENA_FOCUS_COLOR = Color.web("#039ed3");

    @DisplayName("TAB without editing is not handled, so the focus traversal can leave the table")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTabWithoutEditingIsNotHandled(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, stringColumn, comboBoxColumn);
            setCellSelectionEnabled(table);
            select(table, 0, stringColumn);

            pressKey(table, KeyCode.TAB);

            assertEquals(0, getFocusedRow(table));
            assertSame(stringColumn, getFocusedColumn(table));
        });
    }

    @DisplayName("TAB while editing commits the ComboBox value and edits the next cell")
    @ParameterizedTest
    @EnumSource(TableType.class)
    @SuppressWarnings("unchecked")
    void testTabWhileEditingCommitsTheComboBoxValue(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("1", "A", null);
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, List.of(row, new Row("2", "A", null)), stringColumn,
                    comboBoxColumn);

            edit(table, 0, stringColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB);

            ComboBox<String> comboBox = (ComboBox<String>) table.getScene().getFocusOwner();
            comboBox.getSelectionModel().select("B");
            pressKey(comboBox, KeyCode.TAB);

            assertEquals("B", row.second());
            assertSame(stringColumn, getFocusedColumn(table));
            assertTrue(isEditing(table));
        });
    }

    @DisplayName("TAB while editing the last cell edits the first cell, which is scrolled into view")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTabWhileEditingWrapsToTheFirstRow(TableType tableType) {
        runOnFxThread(() -> {
            List<Row> rows = IntStream.range(0, 100).mapToObj(i -> new Row(String.valueOf(i), "A", null)).toList();
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, rows, stringColumn, comboBoxColumn);

            select(table, 99, comboBoxColumn);
            ((ExtendedTable<?>) table).scrollIntoView(99);
            edit(table, 99, comboBoxColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB);

            assertEquals(0, getFocusedRow(table));
            assertSame(stringColumn, getFocusedColumn(table));
            assertTrue(((ExtendedTable<?>) table).isEditing());
            assertTrue(table.getScene().getFocusOwner() instanceof TextField);
        });
    }

    @DisplayName("SHIFT + TAB while editing the first cell edits the last cell, which is scrolled into view")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testShiftTabWhileEditingWrapsToTheLastRow(TableType tableType) {
        runOnFxThread(() -> {
            List<Row> rows = IntStream.range(0, 100).mapToObj(i -> new Row(String.valueOf(i), "A", null)).toList();
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, rows, stringColumn, comboBoxColumn);

            edit(table, 0, stringColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB, true);

            assertEquals(99, getFocusedRow(table));
            assertSame(comboBoxColumn, getFocusedColumn(table));
            assertTrue(((ExtendedTable<?>) table).isEditing());
            assertTrue(table.getScene().getFocusOwner() instanceof ComboBox<?>);
        });
    }

    @DisplayName("ESCAPE on the ComboBox cancels the edit and focuses the table")
    @ParameterizedTest
    @EnumSource(TableType.class)
    @SuppressWarnings("unchecked")
    void testEscapeOnTheComboBoxCancelsTheEdit(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("1", "A", null);
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, List.of(row), comboBoxColumn);

            edit(table, 0, comboBoxColumn);
            ComboBox<String> comboBox = (ComboBox<String>) table.getScene().getFocusOwner();
            comboBox.getSelectionModel().select("B");
            pressKey(comboBox, KeyCode.ESCAPE);

            assertEquals("A", row.second());
            assertFalse(isEditing(table));
            assertEquals("A", comboBox.getValue());
            assertSame(table, table.getScene().getFocusOwner());
        });
    }

    @DisplayName("Editing the ComboBox opens its popup, which TAB hides when moving the edit to the next cell")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testEditingTheComboBoxOpensThePopup(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            Control table = createShownTable(tableType, comboBoxColumn, stringColumn);

            edit(table, 0, comboBoxColumn);
            ComboBox<?> comboBox = (ComboBox<?>) table.getScene().getFocusOwner();
            assertTrue(comboBox.isShowing());

            // The popup gets the key events while showing.
            pressKey(((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent(), KeyCode.TAB);

            assertFalse(comboBox.isShowing());
            assertSame(stringColumn, getFocusedColumn(table));
            assertTrue(isEditing(table));
        });
    }

    @DisplayName("A reuse of the editing ComboBox cell commits the picked value into the edited row")
    @ParameterizedTest
    @EnumSource(TableType.class)
    @SuppressWarnings("unchecked")
    void testReusedComboBoxCellCommitsThePickedValue(TableType tableType) {
        runOnFxThread(() -> {
            List<Row> rows = IntStream.range(0, 100).mapToObj(i -> new Row(String.valueOf(i), "A", null)).toList();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, rows, comboBoxColumn);

            edit(table, 0, comboBoxColumn);
            ((ComboBox<String>) table.getScene().getFocusOwner()).getSelectionModel().select("B");
            scrollTo(table, rows.size() - 1);
            table.layout();

            assertEquals("B", rows.getFirst().second());
            assertTrue(rows.stream().skip(1).allMatch(row -> "A".equals(row.second())));
        });
    }

    @DisplayName("TAB while editing only focuses the CheckBox, which ENTER then toggles")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTabWhileEditingFocusesTheCheckBox(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("false", "A", null);
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            Control table = createShownTable(tableType, List.of(row), comboBoxColumn, checkBoxColumn);

            edit(table, 0, comboBoxColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB);

            assertEquals("false", row.first());
            assertTrue(table.getScene().getFocusOwner() instanceof CheckBox);

            pressKey(table.getScene().getFocusOwner(), KeyCode.ENTER);

            assertEquals("true", row.first());
            assertFalse(isEditing(table));
        });
    }

    /// The ring is the one of the theme, Modena's `.check-box-table-cell`, which the tree table cell has as well.
    @DisplayName("The CheckBox focused by TAB shows the focus ring of Modena on its box")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFocusedCheckBoxShowsTheFocusRing(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            Control table = createShownTable(tableType, List.of(new Row("false", "A", null)), comboBoxColumn,
                    checkBoxColumn);
            CheckBox checkBox = (CheckBox) table.lookupAll(".check-box").stream()
                    .filter(node -> node.getParent() instanceof IndexedCell<?> cell && cell.getIndex() == 0)
                    .findFirst().orElseThrow();
            Region box = (Region) checkBox.lookup(".box");
            assertNotEquals(MODENA_FOCUS_COLOR, box.getBackground().getFills().getFirst().getFill());

            edit(table, 0, comboBoxColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB);
            table.applyCss();
            table.layout();

            assertSame(checkBox, table.getScene().getFocusOwner());
            assertEquals(MODENA_FOCUS_COLOR, box.getBackground().getFills().getFirst().getFill());
        });
    }

    @DisplayName("The ComboBox shows no box and the text color of its selected cell, even with a theme")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testComboBoxShowsNoBoxWithATheme(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, stringColumn, comboBoxColumn);
            table.getScene().getStylesheets().addAll(Theme.of(Palette.of()).stylesheets());
            edit(table, 0, stringColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.ESCAPE);
            table.applyCss();
            table.layout();

            List<Labeled> firstRowCells = table.lookupAll(tableType.getCellSelector()).stream()
                    .filter(node -> ((IndexedCell<?>) node).getIndex() == 0).map(Labeled.class::cast).toList();
            Labeled stringCell = firstRowCells.stream().filter(cell -> cell.getGraphic() == null).findFirst()
                    .orElseThrow();
            ComboBox<?> comboBox = (ComboBox<?>) firstRowCells.stream().filter(cell -> cell.getGraphic() != null)
                    .findFirst().orElseThrow().getGraphic();
            Labeled comboBoxText = comboBox.getChildrenUnmodifiable().stream().filter(ListCell.class::isInstance)
                    .map(Labeled.class::cast).findFirst().orElseThrow();

            assertNull(comboBox.getBackground());
            assertNull(comboBox.getBorder());
            assertEquals("A", comboBoxText.getText());
            assertEquals(stringCell.getTextFill(), comboBoxText.getTextFill());
        });
    }

    @DisplayName("CTRL + TAB while editing is not handled, so the focus traversal can leave the table")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTabWithModifierWhileEditingIsNotHandled(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, stringColumn, comboBoxColumn);

            edit(table, 0, stringColumn);
            Event.fireEvent(table.getScene().getFocusOwner(),
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, true, false, false));

            assertSame(stringColumn, getFocusedColumn(table));
        });
    }

    @DisplayName("TAB while editing scrolls as little as possible, so the next cell is fully visible")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTabWhileEditingScrollsTheNextCellFullyIntoView(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            stringColumn.setPrefWidth(300);
            comboBoxColumn.setPrefWidth(300);
            Control table = createShownTable(tableType, stringColumn, comboBoxColumn);

            edit(table, 0, stringColumn);
            pressKey(table.getScene().getFocusOwner(), KeyCode.TAB);
            table.layout();

            Node comboBox = table.getScene().getFocusOwner();
            Bounds comboBoxBounds = comboBox.localToScene(comboBox.getLayoutBounds());
            Bounds tableBounds = table.localToScene(table.getLayoutBounds());
            assertTrue(comboBox instanceof ComboBox<?>);
            assertTrue(comboBoxBounds.getMaxX() <= tableBounds.getMaxX(), "The ComboBox should be fully visible");
            assertTrue(comboBoxBounds.getMinX() > tableBounds.getMinX(),
                    "The table should only scroll as much as needed");
        });
    }

    @DisplayName("Keys on a focused header button are not handled by the table")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testKeysOnAHeaderButtonAreNotHandled(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, stringColumn, comboBoxColumn);
            setCellSelectionEnabled(table);
            select(table, 0, stringColumn);
            Button headerButton = new Button();
            ExtendedTable<?> extendedTable = (ExtendedTable<?>) table;
            extendedTable.setShowHeader(true);
            extendedTable.getHeaderButtons().add(headerButton);
            table.layout();
            headerButton.requestFocus();

            pressKey(headerButton, KeyCode.TAB);
            Event.fireEvent(headerButton,
                    new KeyEvent(KeyEvent.KEY_TYPED, "x", "", KeyCode.UNDEFINED, false, false, false, false));

            assertSame(stringColumn, getFocusedColumn(table));
            assertFalse(isEditing(table));
        });
    }


    @DisplayName("SHORTCUT + C copies the selected rows with tab separated columns")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCopySelectedRows(TableType tableType) {
        runOnFxThread(() -> {
            Control table = createShownTable(tableType, List.of(new Row("1", "A", null), new Row("2", "B", null)),
                    tableType.createStringColumn(), tableType.createComboBoxColumn(List.of("A", "B")));
            selectRows(table, 0, 1);

            pressShortcut(table, KeyCode.C);

            assertEquals("1\tA\n2\tB", Clipboard.getSystemClipboard().getString());
        });
    }

    @DisplayName("SHORTCUT + C quotes the text of multiple cells which contains a tab, line break or quote")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCopyQuotesTheTextOfMultipleCells(TableType tableType) {
        runOnFxThread(() -> {
            Control table = createShownTable(tableType,
                    List.of(new Row("a\tb", "A", null), new Row("c\nd", "B", null), new Row("say \"e\"", "A", null)),
                    tableType.createStringColumn());
            selectRows(table, 0, 1, 2);

            pressShortcut(table, KeyCode.C);

            assertEquals("\"a\tb\"\n\"c\nd\"\n\"say \"\"e\"\"\"", Clipboard.getSystemClipboard().getString());
        });
    }

    @DisplayName("SHORTCUT + C copies a single cell as it is, even when its text is empty")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCopySingleCellAsItIs(TableType tableType) {
        runOnFxThread(() -> {
            Control table = createShownTable(tableType, List.of(new Row("c\nd", "A", null), new Row("", "B", null)),
                    tableType.createStringColumn());

            selectRows(table, 0);
            pressShortcut(table, KeyCode.C);
            assertEquals("c\nd", Clipboard.getSystemClipboard().getString());

            selectRows(table, 1);
            pressShortcut(table, KeyCode.C);
            assertEquals("", Clipboard.getSystemClipboard().getString());
        });
    }

    @DisplayName("SHORTCUT + C copies unselected cells between the selected cells empty")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testCopySelectedCellsKeepsTheColumns(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, List.of(new Row("1", "A", null), new Row("2", "B", null)),
                    stringColumn, comboBoxColumn);
            setCellSelectionEnabled(table);
            ((ExtendedTable<?>) table).getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            select(table, 0, stringColumn);
            select(table, 1, comboBoxColumn);

            pressShortcut(table, KeyCode.C);

            assertEquals("1\t\n\tB", Clipboard.getSystemClipboard().getString());
        });
    }

    @DisplayName("SHORTCUT + V edits the focused cell and pastes into its text input, without a commit")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testPasteEditsTheFocusedCell(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("1", "A", null);
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            Control table = createShownTable(tableType, List.of(row), stringColumn);
            select(table, 0, stringColumn);
            ClipboardContent content = new ClipboardContent();
            content.putString("x");
            Clipboard.getSystemClipboard().setContent(content);

            pressShortcut(table, KeyCode.V);

            TextField textField = (TextField) table.getScene().getFocusOwner();
            assertEquals("x", textField.getText());
            assertEquals("1", row.first());
        });
    }

    @DisplayName("SHORTCUT + V does not edit a cell without a text input")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testPasteDoesNotEditACellWithoutTextInput(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, comboBoxColumn);
            select(table, 0, comboBoxColumn);

            pressShortcut(table, KeyCode.V);

            assertFalse(((ExtendedTable<?>) table).isEditing());
        });
    }

    @DisplayName("Typing a character edits the focused cell and replaces its text, without a commit")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTypingEditsTheFocusedCell(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("1", "A", null);
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            Control table = createShownTable(tableType, List.of(row), stringColumn);
            select(table, 0, stringColumn);

            typeKey(table, "x");

            TextField textField = (TextField) table.getScene().getFocusOwner();
            assertEquals("x", textField.getText());
            assertEquals(1, textField.getCaretPosition());
            assertEquals("1", row.first());
        });
    }

    @DisplayName("Typing does not edit a cell without a text input")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTypingDoesNotEditACellWithoutTextInput(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            Control table = createShownTable(tableType, comboBoxColumn);
            select(table, 0, comboBoxColumn);

            typeKey(table, "x");

            assertFalse(((ExtendedTable<?>) table).isEditing());
        });
    }

    @DisplayName("Keys for another editor neither edit nor commit the focused cell")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testKeysForAnotherEditorDoNotCommit(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("false", "A", null);
            TableColumnBase<?, ?> stringColumn = tableType.createStringColumn();
            TableColumnBase<?, ?> comboBoxColumn = tableType.createComboBoxColumn(List.of("A", "B"));
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            Control table = createShownTable(tableType, List.of(row), stringColumn, comboBoxColumn, checkBoxColumn);
            AtomicInteger commits = new AtomicInteger();
            table.addEventHandler(ExtendedTable.commitEvent(), _ -> commits.incrementAndGet());

            select(table, 0, comboBoxColumn);
            typeKey(table, "x");
            assertFalse(((ExtendedTable<?>) table).isEditing());
            assertFalse(isEditing(table));

            select(table, 0, checkBoxColumn);
            typeKey(table, "x");
            assertFalse(((ExtendedTable<?>) table).isEditing());

            // SPACE is then handled by the table, which edits the cell.
            select(table, 0, stringColumn);
            pressKey(table, KeyCode.SPACE);
            assertTrue(table.getScene().getFocusOwner() instanceof TextField);

            assertEquals(0, commits.get());
            assertEquals("false", row.first());
        });
    }

    @DisplayName("Keys do not edit a cell with an unknown editor")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testKeysDoNotEditACellWithAnUnknownEditor(TableType tableType) {
        runOnFxThread(() -> {
            TableColumnBase<?, ?> column = tableType.createColumn("Column", "value", () -> null);
            Control table = createShownTable(tableType, column);
            AtomicInteger editStarts = new AtomicInteger();
            onEditStart(column, editStarts::incrementAndGet);
            select(table, 0, column);

            typeKey(table, "x");
            pressShortcut(table, KeyCode.V);

            assertEquals(0, editStarts.get());
        });
    }

    @DisplayName("ENTER and SPACE toggle and commit the CheckBox of the focused cell")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testEnterAndSpaceToggleTheCheckBox(TableType tableType) {
        runOnFxThread(() -> {
            Row row = new Row("false", "A", null);
            TableColumnBase<?, ?> checkBoxColumn = tableType.createCheckBoxColumn();
            Control table = createShownTable(tableType, List.of(row), checkBoxColumn);
            select(table, 0, checkBoxColumn);

            pressKey(table, KeyCode.ENTER);
            assertEquals("true", row.first());

            pressKey(table, KeyCode.SPACE);
            assertEquals("false", row.first());
            assertFalse(((ExtendedTable<?>) table).isEditing());
            assertSame(table, table.getScene().getFocusOwner());
        });
    }

    private static Control createShownTable(TableType tableType, TableColumnBase<?, ?>... columns) {
        return createShownTable(tableType, List.of(new Row("1", "A", null), new Row("2", "A", null)), columns);
    }

    private static Control createShownTable(TableType tableType, List<Row> rows, TableColumnBase<?, ?>... columns) {
        Control table = tableType.createTable(List.of(columns), rows);
        tableType.setEditable(table, true);
        showInStage(table, 400, 300);
        table.requestFocus();
        return table;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void select(Control table, int row, TableColumnBase<?, ?> column) {
        switch (table) {
            case TableView tableView -> tableView.getSelectionModel().select(row, (TableColumn) column);
            case TreeTableView treeTableView -> treeTableView.getSelectionModel().select(row, (TreeTableColumn) column);
            default -> throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void edit(Control table, int row, TableColumnBase<?, ?> column) {
        select(table, row, column);
        switch (table) {
            case TableView tableView -> tableView.edit(row, (TableColumn) column);
            case TreeTableView treeTableView -> treeTableView.edit(row, (TreeTableColumn) column);
            default -> throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void onEditStart(TableColumnBase<?, ?> column, Runnable handler) {
        switch (column) {
            case TableColumn tableColumn ->
                    tableColumn.addEventHandler(TableColumn.editStartEvent(), _ -> handler.run());
            case TreeTableColumn treeTableColumn ->
                    treeTableColumn.addEventHandler(TreeTableColumn.editStartEvent(), _ -> handler.run());
            default -> throw new IllegalArgumentException();
        }
    }

    private static void scrollTo(Control table, int row) {
        switch (table) {
            case TableView<?> tableView -> tableView.scrollTo(row);
            case TreeTableView<?> treeTableView -> treeTableView.scrollTo(row);
            default -> throw new IllegalArgumentException();
        }
    }

    private static void setCellSelectionEnabled(Control table) {
        switch (table) {
            case TableView<?> tableView -> tableView.getSelectionModel().setCellSelectionEnabled(true);
            case TreeTableView<?> treeTableView -> treeTableView.getSelectionModel().setCellSelectionEnabled(true);
            default -> throw new IllegalArgumentException();
        }
    }

    private static int getFocusedRow(Control table) {
        return switch (table) {
            case TableView<?> tableView -> tableView.getFocusModel().getFocusedIndex();
            case TreeTableView<?> treeTableView -> treeTableView.getFocusModel().getFocusedIndex();
            default -> throw new IllegalArgumentException();
        };
    }

    private static TableColumnBase<?, ?> getFocusedColumn(Control table) {
        return switch (table) {
            case TableView<?> tableView -> tableView.getFocusModel().getFocusedCell().getTableColumn();
            case TreeTableView<?> treeTableView -> treeTableView.getFocusModel().getFocusedCell().getTableColumn();
            default -> throw new IllegalArgumentException();
        };
    }

    private static boolean isEditing(Control table) {
        return switch (table) {
            case TableView<?> tableView -> tableView.getEditingCell() != null;
            case TreeTableView<?> treeTableView -> treeTableView.getEditingCell() != null;
            default -> throw new IllegalArgumentException();
        };
    }

    private static void pressKey(Node target, KeyCode code) {
        pressKey(target, code, false);
    }

    private static void pressKey(Node target, KeyCode code, boolean shift) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, shift, false, false, false));
    }

    private static void selectRows(Control table, int... rows) {
        switch (table) {
            case TableView<?> tableView -> {
                tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                tableView.getSelectionModel().clearSelection();
                tableView.getSelectionModel().selectIndices(-1, rows);
            }
            case TreeTableView<?> treeTableView -> {
                treeTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                treeTableView.getSelectionModel().clearSelection();
                treeTableView.getSelectionModel().selectIndices(-1, rows);
            }
            default -> throw new IllegalArgumentException();
        }
    }

    private static void typeKey(Control table, String character) {
        Event.fireEvent(table,
                new KeyEvent(KeyEvent.KEY_TYPED, character, "", KeyCode.UNDEFINED, false, false, false, false));
    }

    private static void pressShortcut(Control table, KeyCode code) {
        Event.fireEvent(table, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, true, false, false));
    }
}
