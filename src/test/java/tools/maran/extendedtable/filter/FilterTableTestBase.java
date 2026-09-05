package tools.maran.extendedtable.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import org.junit.jupiter.api.AfterEach;
import tools.maran.extendedtable.JavaFxTest;
import tools.maran.extendedtable.filter.data.FilterTableFixture;
import tools.maran.extendedtable.filter.data.Row;
import tools.maran.extendedtable.filter.data.TableType;
import tools.maran.extendedtable.table.common.TableI18N;

/// Base class for tests of the filterable columns, which performs the actions on a shown table like a user would.
///
/// @author Marius Hanl
public abstract class FilterTableTestBase extends JavaFxTest {

    protected static final int COLUMN_1 = 0;
    protected static final int COLUMN_2 = 1;
    protected static final int COLUMN_3 = 2;

    // The buttons inside the footer of the filter popup, in the order they are added.
    static final int APPLY_BUTTON = 0;
    static final int RESET_COLUMN_BUTTON = 1;
    static final int RESET_ALL_BUTTON = 2;

    FilterTableFixture fixture;
    Stage stage;

    protected FilterTableTestBase() {
    }

    protected void addBackingRow(int index, Row row) {
        runOnFxThread(() -> fixture.addBackingRow(index, row));
    }

    /// Filters the column by selecting exactly the entries with the given values.
    protected void applyFilter(int columnIndex, Set<String> values) {
        runOnFxThread(() -> {
            FilterPopupControl<?> filterPopup = showFilterPopup(columnIndex);

            for (Object entry : nodes(filterPopup, ListView.class).getFirst().getItems()) {
                FilterPopupControlSkin<?>.SelectableItem<?> item = (FilterPopupControlSkin<?>.SelectableItem<?>) entry;
                item.setSelected(values.contains(item.getItemText()));
            }

            button(filterPopup, APPLY_BUTTON).fire();

            assertFalse(filterPopup.isShowing(),
                    "The filter popup should be hidden after the filter of the column was applied");
        });
    }

    @AfterEach
    protected void closeStage() {
        if (stage != null) {
            runOnFxThread(stage::close);
        }
    }

    /// Commits the value into the cell like a user editing it. The row index is the visible one.
    protected void commitValue(int rowIndex, int columnIndex, String value) {
        runOnFxThread(() -> fixture.commitValue(rowIndex, columnIndex, value));
    }

    protected List<Row> getBackingRows() {
        return getOnFxThread(() -> fixture.getBackingRows());
    }

    protected List<Row> getVisibleRows() {
        return getOnFxThread(() -> fixture.getVisibleRows());
    }

    protected boolean isFiltered() {
        return getOnFxThread(() -> fixture.isFiltered());
    }

    protected void refreshFilter(int columnIndex) {
        runOnFxThread(() -> fixture.refreshFilter(columnIndex));
    }

    protected void removeBackingRow(Row row) {
        runOnFxThread(() -> fixture.removeBackingRow(row));
    }

    protected void replaceBackingRow(Row oldRow, Row newRow) {
        runOnFxThread(() -> fixture.replaceBackingRow(oldRow, newRow));
    }

    /// Resets the filter of all columns with the filter popup of the given column.
    protected void resetAllFilters(int columnIndex) {
        runOnFxThread(() -> {
            FilterPopupControl<?> filterPopup = showFilterPopup(columnIndex);

            button(filterPopup, RESET_ALL_BUTTON).fire();

            assertFalse(filterPopup.isShowing(),
                    "The filter popup should be hidden after all filters were reset");
        });
    }

    protected void resetColumnFilter(int columnIndex) {
        runOnFxThread(() -> {
            FilterPopupControl<?> filterPopup = showFilterPopup(columnIndex);

            button(filterPopup, RESET_COLUMN_BUTTON).fire();

            assertFalse(filterPopup.isShowing(),
                    "The filter popup should be hidden after the filter of the column was reset");
        });
    }

    protected void setRows(List<Row> tableRows) {
        runOnFxThread(() -> fixture.setRows(tableRows));
    }

    /// Creates the table of the given type with the rows and shows it inside a [Stage].
    protected void showTable(TableType tableType, List<Row> tableRows) {
        runOnFxThread(() -> {
            fixture = tableType.createFixture();
            fixture.setRows(tableRows);

            stage = new Stage();
            stage.setScene(new Scene(fixture.getTable(), 800, 600));
            stage.show();
        });
    }

    /// Shows the filter popup of the column, runs the assertions against it and hides it without filtering.
    protected void withFilterPopup(int columnIndex, Consumer<FilterPopupControl<?>> assertions) {
        runOnFxThread(() -> {
            FilterPopupControl<?> filterPopup = showFilterPopup(columnIndex);
            try {
                assertions.accept(filterPopup);
            } finally {
                filterPopup.hide();
            }
        });
    }

    /// Filters the column by all entries matching the given texts.
    void applyFilter(int columnIndex, String... filterTexts) {
        runOnFxThread(() -> {
            FilterPopupControl<?> filterPopup = showFilterPopup(columnIndex);

            // Typing a text only shows the matching entries, which are then all selected at once.
            for (String filterText : filterTexts) {
                selectEntries(filterPopup, filterText);
            }

            button(filterPopup, APPLY_BUTTON).fire();

            assertFalse(filterPopup.isShowing(),
                    "The filter popup should be hidden after the filter of the column was applied");
        });
    }

    /// Asserts the entries of the filter popup of the column, without filtering it.
    void assertFilterEntries(int columnIndex, List<String> expectedEntries) {
        withFilterPopup(columnIndex, filterPopup -> assertEquals(expectedEntries, entryTexts(filterPopup)));
    }

    static Button button(FilterPopupControl<?> filterPopup, int index) {
        List<Button> buttons = nodes(filterPopup, Button.class);

        assertEquals(3, buttons.size(),
                "Expected the apply, the reset column and the reset all button inside the filter popup");
        return buttons.get(index);
    }

    static <T extends Node> void collectNodes(Node node, Class<T> type, List<T> nodes) {
        if (type.isInstance(node)) {
            nodes.add(type.cast(node));
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectNodes(child, type, nodes);
            }
        }
    }

    /// Returns the texts of the shown entries of the filter popup.
    static List<String> entryTexts(FilterPopupControl<?> filterPopup) {
        ListView<?> entries = nodes(filterPopup, ListView.class).getFirst();

        return entries.getItems().stream()
                .map(entry -> ((FilterPopupControlSkin<?>.SelectableItem<?>) entry).getItemText())
                .toList();
    }

    void filter(int columnIndex, List<Row> filteredRows) {
        runOnFxThread(() -> fixture.filter(columnIndex, filteredRows));
    }

    static TextField filterTextField(FilterPopupControl<?> filterPopup) {
        return nodes(filterPopup, TextField.class).getFirst();
    }

    boolean isColumnFiltered(int columnIndex) {
        return getOnFxThread(() -> fixture.isColumnFiltered(columnIndex));
    }

    static <T extends Node> List<T> nodes(FilterPopupControl<?> filterPopup, Class<T> type) {
        List<T> nodes = new ArrayList<>();
        collectNodes(filterPopup.getSkin().getNode(), type, nodes);
        return nodes;
    }

    /// Returns the checkbox which selects all shown entries. It is the first one, the others belong to the entries.
    static CheckBox selectAllCheckBox(FilterPopupControl<?> filterPopup) {
        return nodes(filterPopup, CheckBox.class).getFirst();
    }

    /// Selects all entries matching the text by typing it in and firing the select all check box.
    static void selectEntries(FilterPopupControl<?> filterPopup, String text) {
        filterTextField(filterPopup).setText(text);

        CheckBox selectAllCheckBox = selectAllCheckBox(filterPopup);
        // Firing the check box toggles it, so it needs to be unselected to actually select the shown entries.
        // This matters when entries were selected before, which leaves the check box selected (and indeterminate).
        selectAllCheckBox.setSelected(false);
        selectAllCheckBox.fire();
    }

    static String selectedMessage(int selected, int all) {
        return TableI18N.message("count.selected", selected, all);
    }

    /// Returns the text of the selection count label. It is the only label directly inside the filter popup.
    static String selectionText(FilterPopupControl<?> filterPopup) {
        Parent filterBox = (Parent) filterPopup.getSkin().getNode();

        return filterBox.getChildrenUnmodifiable().stream()
                .filter(Label.class::isInstance)
                .map(node -> ((Label) node).getText())
                .findFirst()
                .orElseThrow();
    }

    void setBackingRows(List<Row> backingRows) {
        runOnFxThread(() -> fixture.setBackingRows(backingRows));
    }

    FilterPopupControl<?> showFilterPopup(int columnIndex) {
        FilterPopupControl<?> filterPopup = fixture.getFilterPopup(columnIndex);
        filterPopup.show(stage);

        assertTrue(filterPopup.isShowing(), "The filter popup of the column should be showing");

        // The text typed in the last time the popup was shown is kept, which would hide entries.
        filterTextField(filterPopup).clear();
        return filterPopup;
    }
}
