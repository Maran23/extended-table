package tools.maran.extendedtable.table.common;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableSelectionModel;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import tools.maran.extendedtable.filter.FilterableColumn;

/// Handles the keys of a table to traverse and edit its cells:
///
/// - TAB while editing moves the edit to the next editable cell, SHIFT + TAB to the previous one, like spreadsheets do.
///   Without editing, TAB is left to the focus traversal, so the focus leaves the table like for any other control
///   (see the WAI-ARIA grid pattern). The arrow keys move between the cells and ESCAPE ends an edit.
/// - SHORTCUT + C copies the text of the selected cells, with columns separated by a tab and rows by a line break,
///   like spreadsheets do. Unselected cells between selected ones are copied empty. A text with a tab, line break or
///   quote is quoted, unless it is the only copied cell.
/// - SHORTCUT + V edits the focused cell and pastes into its text input, without a commit.
/// - Typing a character edits the focused cell and replaces the text of its text input with the character.
/// - ENTER or SPACE toggles and commits the [CheckBox] of the focused cell.
///
/// Except for TAB while editing, only the keys of the focused table itself are handled, so e.g. the header buttons
/// keep their keys. Cells without a matching editor are not edited and the event is left to the table. An extended cell
/// with another or unknown editor does not even start to edit, see [#isEditorAccepted(Node, Class)].
///
/// @param <T>
///         the item type
/// @author Marius Hanl
public final class CellKeyboard<T> {

    private static final KeyCombination TAB = new KeyCodeCombination(KeyCode.TAB, KeyCombination.SHIFT_ANY);
    private static final KeyCombination COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination PASTE = new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN);
    private static final KeyCombination TOGGLE_ENTER = new KeyCodeCombination(KeyCode.ENTER);
    private static final KeyCombination TOGGLE_SPACE = new KeyCodeCombination(KeyCode.SPACE);

    private static final String EDITOR_TYPE = "cellKeyboardEditorType";

    private final ExtendedTable<T> table;
    private final IntSupplier rowCount;

    /// Creates a new [CellKeyboard] instance.
    ///
    /// @param table
    ///         the table to handle the keys for
    /// @param rowCount
    ///         returns the number of rows
    public CellKeyboard(ExtendedTable<T> table, IntSupplier rowCount) {
        this.table = table;
        this.rowCount = rowCount;
    }

    /// Handles the given key pressed or typed event and consumes it, if it was handled.
    ///
    /// @param event
    ///         the key event
    public void handle(KeyEvent event) {
        if (TAB.match(event)) {
            if (table.isEditing()) {
                editNextCell(event);
            }
            return;
        }

        if (!table.isFocused() || table.isEditing()) {
            return;
        }

        if (event.getEventType() == KeyEvent.KEY_TYPED) {
            if (isTypedText(event)) {
                editFocusedCell(event, TextInputControl.class, textInput -> {
                    textInput.setText(event.getCharacter());
                    textInput.end();
                });
            }
        } else if (COPY.match(event)) {
            copy();
            event.consume();
        } else if (PASTE.match(event)) {
            editFocusedCell(event, TextInputControl.class, TextInputControl::paste);
        } else if (TOGGLE_ENTER.match(event) || TOGGLE_SPACE.match(event)) {
            editFocusedCell(event, CheckBox.class, CheckBox::fire);
        }
    }

    /// Returns true, when a cell with an editor of the given type may start the current edit of its table. An edit
    /// started by a key requires an editor of the matching type, so a cell with another or unknown editor refuses to
    /// edit and is therefore never committed or cancelled.
    ///
    /// @param table
    ///         the table of the cell
    /// @param editorType
    ///         the type of the editor of the cell, or null when unknown
    /// @return true, when the cell may start the edit, false otherwise
    public static boolean isEditorAccepted(Node table, Class<? extends Node> editorType) {
        return !(table.getProperties().get(EDITOR_TYPE) instanceof Class<?> requiredType) || editorType != null
                && requiredType.isAssignableFrom(editorType);
    }

    private void editNextCell(KeyEvent event) {
        List<? extends TableColumnBase<T, ?>> columns = table.getVisibleLeafColumns().stream()
                .filter(TableColumnBase::isEditable).toList();
        // The editing column may have become hidden or not editable while editing.
        if (columns.isEmpty()) {
            return;
        }

        int rows = rowCount.getAsInt();
        int step = event.isShiftDown() ? -1 : 1;
        int row = Math.max(0, table.getFocusModel().getFocusedIndex());
        int column = columns.indexOf(table.getFocusedColumn());
        // A focused row or non-editable column is treated as being right before the first editable column.
        if (column == -1 && step == -1) {
            column = 0;
        }

        // Cells which refuse to edit are skipped, but at most one row.
        for (int i = 0; i < columns.size(); i++) {
            column += step;
            if (column == -1 || column == columns.size()) {
                column = Math.floorMod(column, columns.size());
                row = Math.floorMod(row + step, rows);
            }

            table.getSelectionModel().clearAndSelect(row, columns.get(column));
            table.scrollIntoView(row);
            table.edit(row, columns.get(column));
            if (table.isEditing()) {
                break;
            }
        }

        table.scrollToColumn(columns.get(column));
        event.consume();
    }

    private void copy() {
        TableSelectionModel<T> selectionModel = table.getSelectionModel();
        List<Integer> rows = selectionModel.getSelectedIndices().stream().sorted().distinct().toList();
        // Every column with a selected cell, so the rows have the same columns and unselected cells stay empty.
        List<? extends TableColumnBase<T, ?>> columns = table.getVisibleLeafColumns().stream()
                .filter(column -> rows.stream().anyMatch(row -> isSelected(selectionModel, row, column))).toList();
        if (columns.isEmpty()) {
            return;
        }

        // A single cell is copied as it is, so it can be pasted into a text input again.
        boolean quote = rows.size() > 1 || columns.size() > 1;
        String text = rows.stream()
                .map(row -> columns.stream()
                        .map(column -> isSelected(selectionModel, row, column) ? getCellText(column, row) : "")
                        .map(cellText -> quote ? quote(cellText) : cellText)
                        .collect(Collectors.joining("\t")))
                .collect(Collectors.joining("\n"));

        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private <C> void editFocusedCell(KeyEvent event, Class<C> editorType, Consumer<C> action) {
        int row = table.getFocusModel().getFocusedIndex();
        TableColumnBase<T, ?> column = table.getFocusedColumn();
        if (row == -1 || column == null) {
            return;
        }

        table.getProperties().put(EDITOR_TYPE, editorType);
        table.edit(row, column);
        table.getProperties().remove(EDITOR_TYPE);

        Node focusOwner = table.getScene().getFocusOwner();
        if (table.isEditing() && editorType.isInstance(focusOwner)) {
            action.accept(editorType.cast(focusOwner));
            event.consume();
        } else {
            // Clears the editing cell of a refused edit, or ends the edit of a cell with an unknown editor.
            table.edit(-1, null);
        }
    }

    private static String getCellText(TableColumnBase<?, ?> column, int row) {
        String text = column instanceof FilterableColumn<?> filterableColumn ? filterableColumn.getCellText(row)
                : null;
        return text != null ? text : Objects.toString(column.getCellData(row), "");
    }

    /// Quotes the text when it contains a tab, line break or quote, with its quotes doubled, like spreadsheets do.
    private static String quote(String text) {
        if (text.chars().noneMatch(c -> c == '\t' || c == '\n' || c == '\r' || c == '"')) {
            return text;
        }
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static <T> boolean isSelected(TableSelectionModel<T> selectionModel, int row,
            TableColumnBase<T, ?> column) {
        return !selectionModel.isCellSelectionEnabled() || selectionModel.isSelected(row, column);
    }

    private static boolean isTypedText(KeyEvent event) {
        // AltGr is reported as CTRL + ALT on Windows, but types text (e.g. @ on a German layout).
        if ((event.isControlDown() || event.isMetaDown()) && !event.isAltDown()) {
            return false;
        }

        String character = event.getCharacter();
        return !character.isBlank() && !Character.isISOControl(character.charAt(0));
    }
}
