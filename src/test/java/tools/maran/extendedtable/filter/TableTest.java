package tools.maran.extendedtable.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.data.Row;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.data.ValueRow;

/// Tests the filter of the filterable columns, for every table which supports filtering.
///
/// The filters are applied the same way a user would do it: The filter popup of a column is shown, a text is typed in
/// to only show the matching entries, those entries are selected and the filter is applied (or reset) by pressing the
/// corresponding button.
///
/// The same is true for the values of the rows: They are changed by committing a new value into a cell, exactly like
/// the table does it when a user finishes editing a cell.
///
/// @author Marius Hanl
class TableTest extends FilterTableTestBase {

    // The default mutable rows of the table, with two values inside every column.
    private final Row rowA1T8 = new Row("A", "1", "T8");
    private final Row rowA2T8 = new Row("A", "2", "T8");
    private final Row rowA2T9 = new Row("A", "2", "T9");
    private final Row rowB2T8 = new Row("B", "2", "T8");
    private final Row rowA1T9 = new Row("A", "1", "T9");
    private final Row rowB1T9 = new Row("B", "1", "T9");

    private final List<Row> rows = List.of(rowA1T8, rowA2T8, rowA2T9, rowB2T8, rowA1T9, rowB1T9);

    // Rows with three different values inside every column, so a filter can be narrowed down to fewer values instead
    // of ending up with a single one right away. Several rows share their first and second value, so even the third
    // column still has three values left when the other two columns are filtered.
    private final Row rowA1T1 = new Row("A", "1", "T1");
    private final Row rowA1T2 = new Row("A", "1", "T2");
    private final Row rowA1T3 = new Row("A", "1", "T3");
    private final Row rowA2T1 = new Row("A", "2", "T1");
    private final Row rowA3T2 = new Row("A", "3", "T2");
    private final Row rowB1T1 = new Row("B", "1", "T1");

    private final List<Row> narrowableRows = List.of(rowA1T1, rowA1T2, rowA1T3, rowA2T1, rowA3T2, rowB1T1);

    // A row which is not part of any rows above, e.g. to be added later on.
    private final Row rowNewB3T7 = new Row("B", "3", "T7");

    @DisplayName("A refreshed filter shows externally changed values")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testARefreshedFilterShowsTheValuesWhichWereChangedOutsideOfTheTable(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_3, List.of("T8", "T9"));

        runOnFxThread(() -> rowB2T8.setThird("T7"));
        refreshFilter(COLUMN_3);

        withFilterPopup(COLUMN_3, filterPopup -> {
            assertEquals(List.of("T8", "T9", "T7"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "T7");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_3, "T7");

        assertEquals(List.of(rowB2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("A row added between the backing rows of a filtered table is shown at the same position")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testARowAddedBetweenTheBackingRowsOfAFilteredTableIsShownAtTheSamePosition(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        addBackingRow(3, rowNewB3T7);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB3T7, rowA1T9), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB3T7, rowB2T8, rowA1T9, rowB1T9), getBackingRows());
    }

    @DisplayName("A row added to the end of the backing rows of a filtered table is shown last")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testARowAddedToTheEndOfTheBackingRowsOfAFilteredTableIsShownLast(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());

        // The new row does not match the filter, but an added row is always shown.
        addBackingRow(6, rowNewB3T7);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9, rowNewB3T7), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowB2T8, rowA1T9, rowB1T9, rowNewB3T7), getBackingRows());
    }

    @DisplayName("A row added to the top of the backing rows of a filtered table is shown first")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testARowAddedToTheTopOfTheBackingRowsOfAFilteredTableIsShownFirst(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        addBackingRow(0, rowNewB3T7);

        assertTrue(isFiltered());
        assertEquals(List.of(rowNewB3T7, rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
        assertEquals(List.of(rowNewB3T7, rowA1T8, rowA2T8, rowA2T9, rowB2T8, rowA1T9, rowB1T9), getBackingRows());
    }

    @DisplayName("A value edited inside a filtered column moves the row to the entry of the new value")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAValueEditedInsideAFilteredColumnMovesTheRowToTheEntryOfTheNewValue(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_2, "2");
        assertEquals(List.of(rowA2T8, rowA2T9, rowB2T8), getVisibleRows());

        // The row is still shown, although it does not match the filter anymore.
        commitValue(0, COLUMN_2, "1");
        assertEquals(List.of(rowA2T8, rowA2T9, rowB2T8), getVisibleRows());

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "2"), entryTexts(filterPopup));

            selectEntries(filterPopup, "1");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        // Every row with a '1' is let through, including the ones hidden by the previous filter of this column.
        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T8, rowA2T8, rowA1T9, rowB1T9), getVisibleRows());

        resetColumnFilter(COLUMN_2);

        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("A value edited to a new one and back again loses the new filter entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAValueEditedToANewOneAndBackAgainLosesTheNewEntryInsideTheFilter(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_2, List.of("1", "2"));

        commitValue(1, COLUMN_2, "3");
        assertFilterEntries(COLUMN_2, List.of("1", "3", "2"));

        commitValue(1, COLUMN_2, "2");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "2"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "2");
            assertEquals(selectedMessage(3, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "2");

        assertEquals(List.of(rowA2T8, rowA2T9, rowB2T8), getVisibleRows());
    }

    @DisplayName("A new value gets its own filter entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAValueEditedToANewOneGetsItsOwnEntryInsideTheFilter(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_2, List.of("1", "2"));

        // The second row is the first one with a '2', which is now the only row with a '3'.
        commitValue(1, COLUMN_2, "3");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "3", "2"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "3");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "2");

        assertEquals(List.of(rowA2T9, rowB2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("An existing value reuses its filter entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAValueEditedToAnExistingOneIsAddedToTheExistingEntryInsideTheFilter(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_1, List.of("A", "B"));

        // The fourth row is the first one with a 'B', so only the last row is left with a 'B'.
        commitValue(3, COLUMN_1, "A");

        withFilterPopup(COLUMN_1, filterPopup -> {
            assertEquals(List.of("A", "B"), entryTexts(filterPopup));

            selectEntries(filterPopup, "A");
            assertEquals(selectedMessage(5, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_1, "B");

        assertEquals(List.of(rowB1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("An edited value is sorted by the item comparator")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAnEditedValueIsSortedByTheComparatorInsideTheFilter(TableType tableType) {
        showTable(tableType);
        runOnFxThread(() -> fixture.setFilterComparator(COLUMN_3, Comparator.comparing(Row::third)));
        assertFilterEntries(COLUMN_3, List.of("T8", "T9"));

        // The last row is sorted last, but its new value needs to be sorted first.
        commitValue(5, COLUMN_3, "T0");

        withFilterPopup(COLUMN_3, filterPopup -> {
            assertEquals(List.of("T0", "T8", "T9"), entryTexts(filterPopup));

            selectEntries(filterPopup, "T0");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_3, "T0");

        assertEquals(List.of(rowB1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("An edited value of a row whose hash code depends on its values is reflected inside the filter")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAnEditedValueOfARowWhoseHashCodeDependsOnItsValuesIsReflectedInsideTheFilter(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T8");
        Row valueRowB2 = new ValueRow("B", "2", "T9");
        Row valueRowA3 = new ValueRow("A", "3", "T9");
        List<Row> valueRows = List.of(valueRowA1, valueRowA2, valueRowB2, valueRowA3);

        showTable(tableType, valueRows);
        assertFilterEntries(COLUMN_2, List.of("1", "2", "3"));

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowA3), getVisibleRows());

        // Changes the hash code of a row which is already known by the filter of both columns.
        commitValue(1, COLUMN_2, "4");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "4", "3"), entryTexts(filterPopup));

            selectEntries(filterPopup, "4");
            assertEquals(selectedMessage(1, 4), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "4");
        assertEquals(List.of(valueRowA2), getVisibleRows());

        resetColumnFilter(COLUMN_2);
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowA3), getVisibleRows());

        resetColumnFilter(COLUMN_1);

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "4", "2", "3"), entryTexts(filterPopup));

            selectEntries(filterPopup, "2");
            assertEquals(selectedMessage(1, 4), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "4");

        assertEquals(List.of(valueRowA2), getVisibleRows());
        assertEquals(valueRows, getBackingRows());
    }

    @DisplayName("An entry hidden by the filter of another column is shown again with the edited rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testAnEntryHiddenByTheFilterOfAnotherColumnIsShownAgainWithTheEditedRowsWhenThatFilterIsReset(
            TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertFilterEntries(COLUMN_2, List.of("1", "2"));

        // Both visible rows with a '2' are changed, so only the hidden row with a 'B' is left with a '2'.
        commitValue(1, COLUMN_2, "1");
        commitValue(2, COLUMN_2, "1");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1"), entryTexts(filterPopup));

            selectEntries(filterPopup, "1");
            assertEquals(selectedMessage(4, 6), selectionText(filterPopup));
        });

        resetColumnFilter(COLUMN_1);

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "2"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "2");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "2");

        assertEquals(List.of(rowB2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Applying a filter also shows the hidden rows with a selected value")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testApplyingAFilterAlsoShowsTheHiddenRowsWithASelectedValue(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        addBackingRow(6, rowNewB3T7);
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9, rowNewB3T7), getVisibleRows());

        // Only the added row shows a 'B', but the filter lets every row with a 'B' through.
        applyFilter(COLUMN_1, "B");

        assertTrue(isFiltered());
        assertEquals(List.of(rowB2T8, rowB1T9, rowNewB3T7), getVisibleRows());
    }

    @DisplayName("Filtering a column shows only the matching rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFilterOfASingleColumnShowsOnlyTheMatchingRows(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Filtering a column in code shows the given rows and can be combined and reset like a user filter")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFilteringAColumnInCodeShowsTheGivenRowsAndCanBeCombinedAndResetLikeAUserFilter(TableType tableType) {
        showTable(tableType);

        // The order of the given rows does not matter, the rows are shown in the order of the backing rows.
        filter(COLUMN_1, List.of(rowA1T9, rowA1T8, rowA2T9, rowA2T8));

        assertTrue(isFiltered());
        assertTrue(isColumnFiltered(COLUMN_1));
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
        assertFilterEntries(COLUMN_1, List.of("A"));

        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());

        resetColumnFilter(COLUMN_1);

        assertFalse(isColumnFiltered(COLUMN_1));
        assertTrue(isColumnFiltered(COLUMN_2));
        assertEquals(List.of(rowA1T8, rowA1T9, rowB1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Filtering a column in code with a row which is not a backing row throws")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFilteringAColumnInCodeWithARowWhichIsNotABackingRowThrows(TableType tableType) {
        showTable(tableType);

        assertThrows(IllegalArgumentException.class, () -> filter(COLUMN_1, List.of(rowA1T8, rowNewB3T7)));

        assertFalse(isFiltered());
        assertFalse(isColumnFiltered(COLUMN_1));
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Filtering a column in code without filtering out a row does not filter the table")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFilteringAColumnInCodeWithoutFilteringOutARowDoesNotFilterTheTable(TableType tableType) {
        showTable(tableType);

        filter(COLUMN_1, rows);

        assertFalse(isFiltered());
        assertFalse(isColumnFiltered(COLUMN_1));
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Filtering a table with the same row multiple times shows every occurrence")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFilteringATableWithTheSameRowMultipleTimesShowsEveryOccurrence(TableType tableType) {
        showTable(tableType, List.of(rowA1T8, rowB2T8, rowA1T8));

        applyFilter(COLUMN_1, "A");

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA1T8), getVisibleRows());
    }

    @DisplayName("Filtering works again after a reset")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFiltersCanBeAppliedAgainAfterAllFiltersWereReset(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "2");
        resetAllFilters(COLUMN_1);

        applyFilter(COLUMN_1, "B");
        assertEquals(List.of(rowB2T8, rowB1T9), getVisibleRows());

        applyFilter(COLUMN_3, "T9");

        assertTrue(isFiltered());
        assertEquals(List.of(rowB1T9), getVisibleRows());
    }

    @DisplayName("Resetting a filter works again after all filters were reset")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFiltersCanBeResetAgainAfterAllFiltersWereReset(TableType tableType) {
        showTable(tableType, narrowableRows);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "1", "2");
        applyFilter(COLUMN_2, "1");
        resetAllFilters(COLUMN_2);
        assertEquals(narrowableRows, getVisibleRows());

        applyFilter(COLUMN_3, "T1");
        assertEquals(List.of(rowA1T1, rowA2T1, rowB1T1), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertFalse(isFiltered());
        assertEquals(narrowableRows, getVisibleRows());
    }

    @DisplayName("Filters of multiple columns are combined")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testFiltersOfMultipleColumnsAreCombined(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());

        applyFilter(COLUMN_2, "2");
        assertEquals(List.of(rowA2T8, rowA2T9), getVisibleRows());

        applyFilter(COLUMN_3, "T8");
        assertEquals(List.of(rowA2T8), getVisibleRows());

        assertTrue(isFiltered());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Hidden rows moved by setting all backing rows stay filtered when another filter is reset")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testHiddenRowsMovedBySettingAllBackingRowsStayFilteredWhenAnotherFilterIsReset(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        setBackingRows(List.of(rowB1T9, rowA1T8, rowA1T9));
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());

        applyFilter(COLUMN_3, "T8");
        assertEquals(List.of(rowA1T8), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());
    }

    @DisplayName("Moving or hiding a filtered column keeps its filter")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testMovingOrHidingAFilteredColumnKeepsItsFilter(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "1");

        // The second column is the first one afterwards.
        runOnFxThread(() -> fixture.moveColumn(COLUMN_2, 0));
        runOnFxThread(() -> fixture.setColumnVisible(0, false));

        assertTrue(isColumnFiltered(0));
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());

        resetColumnFilter(0);

        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
    }

    @DisplayName("Multiple edits before showing the filter are all reflected")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testMultipleValuesEditedBeforeTheFilterIsShownAreAllReflectedInsideTheFilter(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_1, List.of("A", "B"));

        // Both rows with a 'B' are changed, one to an existing and one to a new value, which another row gets as well.
        commitValue(0, COLUMN_1, "C");
        commitValue(3, COLUMN_1, "A");
        commitValue(5, COLUMN_1, "C");

        withFilterPopup(COLUMN_1, filterPopup -> {
            assertEquals(List.of("C", "A"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "C");
            assertEquals(selectedMessage(2, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_1, "C");

        assertEquals(List.of(rowA1T8, rowB1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("A filtered table only shows entries of visible rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testOnlyTheEditedValuesOfTheVisibleRowsHaveAnEntryInsideTheFilterOfAFilteredTable(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertFilterEntries(COLUMN_2, List.of("1", "2"));

        // The second visible row is the first one with a '2', which is now the only row with a '3'.
        commitValue(1, COLUMN_2, "3");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "3", "2"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            // Only the visible rows are selectable, that is the two rows with a '1' which have an 'A' as well.
            selectEntries(filterPopup, "1");
            assertEquals(selectedMessage(2, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "3");

        assertTrue(isFiltered());
        assertEquals(List.of(rowA2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Removing a backing row of a filtered table removes exactly that row, even when another row is equal")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRemovingABackingRowOfAFilteredTableRemovesExactlyThatRowEvenWhenAnotherRowIsEqual(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T9");
        Row valueRowA1Copy = new ValueRow("A", "1", "T8");
        Row valueRowB1 = new ValueRow("B", "1", "T8");

        showTable(tableType, List.of(valueRowA1, valueRowA2, valueRowA1Copy, valueRowB1));

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowA1Copy), getVisibleRows());

        // The equal rows are not next to each other, so removing the wrong one changes the order of the rows.
        removeBackingRow(valueRowA1Copy);

        assertTrue(isFiltered());
        assertEquals(List.of(valueRowA1, valueRowA2), getVisibleRows());
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowB1), getBackingRows());
    }

    @DisplayName("Removing a backing row of an unfiltered table removes exactly that row, even when another row is equal")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRemovingABackingRowOfAnUnfilteredTableRemovesExactlyThatRowEvenWhenAnotherRowIsEqual(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T9");
        Row valueRowA1Copy = new ValueRow("A", "1", "T8");

        showTable(tableType, List.of(valueRowA1, valueRowA2, valueRowA1Copy));

        removeBackingRow(valueRowA1Copy);

        assertFalse(isFiltered());
        assertEquals(List.of(valueRowA1, valueRowA2), getVisibleRows());
        assertEquals(List.of(valueRowA1, valueRowA2), getBackingRows());
    }

    @DisplayName("Removing a filtered column resets its filter")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRemovingAFilteredColumnResetsItsFilter(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());

        runOnFxThread(() -> fixture.removeColumn(COLUMN_2));

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
    }

    @DisplayName("Removing a hidden backing row after a filter popup was shown does not keep it inside that filter")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRemovingAHiddenBackingRowAfterAFilterPopupWasShownDoesNotKeepItInsideThatFilter(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertFilterEntries(COLUMN_2, List.of("1", "2"));

        removeBackingRow(rowB2T8);
        applyFilter(COLUMN_2, "2");

        assertEquals(List.of(rowA2T8, rowA2T9), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9, rowB1T9), getBackingRows());
    }

    @DisplayName("Removing a shown and a hidden backing row of a filtered table removes both")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRemovingAShownAndAHiddenBackingRowOfAFilteredTableRemovesBoth(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        removeBackingRow(rowA2T8);
        removeBackingRow(rowB2T8);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T9, rowA1T9), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T9, rowA1T9, rowB1T9), getBackingRows());
    }

    @DisplayName("Replacing a hidden backing row of a filtered table shows the new row at the same position")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testReplacingAHiddenBackingRowOfAFilteredTableShowsTheNewRowAtTheSamePosition(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());

        replaceBackingRow(rowB2T8, rowNewB3T7);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB3T7, rowA1T9), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB3T7, rowA1T9, rowB1T9), getBackingRows());
    }

    @DisplayName("Replacing a visible backing row of a filtered table shows the new row at the same position")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testReplacingAVisibleBackingRowOfAFilteredTableShowsTheNewRowAtTheSamePosition(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T9");
        Row valueRowA1Copy = new ValueRow("A", "1", "T8");
        Row valueRowB1 = new ValueRow("B", "1", "T8");
        Row valueRowC1 = new ValueRow("C", "1", "T8");

        showTable(tableType, List.of(valueRowA1, valueRowA2, valueRowA1Copy, valueRowB1));

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowA1Copy), getVisibleRows());

        // The first row is equal to the replaced one, so it must not be replaced instead.
        replaceBackingRow(valueRowA1Copy, valueRowC1);

        assertTrue(isFiltered());
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowC1), getVisibleRows());
        assertEquals(List.of(valueRowA1, valueRowA2, valueRowC1, valueRowB1), getBackingRows());
    }

    @DisplayName("Resetting all filters or setting the rows unmarks every filtered column")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingAllFiltersOrSettingTheRowsUnmarksEveryFilteredColumn(TableType tableType) {
        showTable(tableType);

        filter(COLUMN_1, List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9));
        applyFilter(COLUMN_2, "1");

        resetAllFilters(COLUMN_3);

        assertFalse(isColumnFiltered(COLUMN_1));
        assertFalse(isColumnFiltered(COLUMN_2));
        assertEquals(rows, getVisibleRows());

        filter(COLUMN_1, List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9));
        setRows(rows);

        assertFalse(isFiltered());
        assertFalse(isColumnFiltered(COLUMN_1));
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Resetting all filters at once shows all rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingAllFiltersShowsAllRows(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "2");
        applyFilter(COLUMN_3, "T8");

        resetAllFilters(COLUMN_3);

        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Resetting one filter keeps the remaining filters")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfAColumnShowsTheRowsMatchingTheRemainingFilters(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "2");
        applyFilter(COLUMN_3, "T8");
        assertEquals(List.of(rowA2T8), getVisibleRows());

        resetColumnFilter(COLUMN_2);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Resetting a filter which was applied after all filters were reset keeps the remaining filters")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfAColumnWhichWasFilteredAgainAfterAllFiltersWereResetShowsTheRemainingRows(
            TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_3, "T8");
        resetAllFilters(COLUMN_3);

        // The columns which were filtered before must not restrict the rows anymore, otherwise the table ends up
        // without a single row when a filter is reset later on.
        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_3, "T9");
        assertEquals(List.of(rowA2T9, rowA1T9), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    @DisplayName("Resetting a filter which was applied after the items were replaced keeps the remaining filters")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfAColumnWhichWasFilteredAgainAfterTheItemsWereReplacedShowsTheRemainingRows(
            TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T8, rowA1T9, rowB1T9), getVisibleRows());

        // Replacing the items drops every filter, so the second column must not restrict the rows anymore.
        setRows(rows);
        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());

        applyFilter(COLUMN_3, "T9");
        assertEquals(List.of(rowA2T9, rowA1T9, rowB1T9), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Resetting a narrowed down filter keeps the remaining filters")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfAColumnWhichWasNarrowedDownShowsTheRowsMatchingTheRemainingFilters(TableType tableType) {
        showTable(tableType, narrowableRows);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(rowA1T1, rowA1T2, rowA1T3, rowA2T1, rowA3T2), getVisibleRows());

        applyFilter(COLUMN_2, "1", "2");
        assertEquals(List.of(rowA1T1, rowA1T2, rowA1T3, rowA2T1), getVisibleRows());

        // Narrow the very same filter down to fewer rows.
        applyFilter(COLUMN_2, "2");
        assertEquals(List.of(rowA2T1), getVisibleRows());

        resetColumnFilter(COLUMN_2);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T1, rowA1T2, rowA1T3, rowA2T1, rowA3T2), getVisibleRows());
        assertEquals(narrowableRows, getBackingRows());
    }

    @DisplayName("Resetting a filter which was narrowed down twice keeps the remaining filters")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfAColumnWhichWasNarrowedDownTwiceShowsTheRowsMatchingTheRemainingFilters(
            TableType tableType) {
        showTable(tableType, narrowableRows);

        // Two filters are set before the third column is filtered.
        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T1, rowA1T2, rowA1T3), getVisibleRows());

        // The third column is filtered again and again, narrowing the rows down every time.
        applyFilter(COLUMN_3, "T1", "T2");
        assertEquals(List.of(rowA1T1, rowA1T2), getVisibleRows());

        applyFilter(COLUMN_3, "T1");
        assertEquals(List.of(rowA1T1), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T1, rowA1T2, rowA1T3), getVisibleRows());
        assertEquals(narrowableRows, getBackingRows());
    }

    @DisplayName("Resetting every filter shows all rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfEveryColumnShowsAllRows(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_3, "T8");
        assertEquals(List.of(rowA1T8, rowA2T8), getVisibleRows());

        resetColumnFilter(COLUMN_1);
        assertEquals(List.of(rowA1T8, rowA2T8, rowB2T8), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Resetting the only filter shows all rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheFilterOfTheOnlyFilteredColumnShowsAllRows(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_2, "2");
        assertEquals(List.of(rowA2T8, rowA2T9, rowB2T8), getVisibleRows());

        resetColumnFilter(COLUMN_2);

        assertFalse(isFiltered());
        assertEquals(rows, getVisibleRows());
    }

    @DisplayName("Resetting the narrowed down filters of every column shows all rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testResettingTheNarrowedDownFilterOfEveryColumnShowsAllRows(TableType tableType) {
        showTable(tableType, narrowableRows);

        applyFilter(COLUMN_3, "T1", "T2");
        assertEquals(List.of(rowA1T1, rowA1T2, rowA2T1, rowA3T2, rowB1T1), getVisibleRows());

        applyFilter(COLUMN_2, "1", "2");
        assertEquals(List.of(rowA1T1, rowA1T2, rowA2T1, rowB1T1), getVisibleRows());

        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T1, rowA1T2, rowB1T1), getVisibleRows());

        resetColumnFilter(COLUMN_2);
        assertEquals(List.of(rowA1T1, rowA1T2, rowA2T1, rowA3T2, rowB1T1), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertFalse(isFiltered());
        assertEquals(narrowableRows, getVisibleRows());
    }

    @DisplayName("Rows added and replaced while filtered stay shown when the filter of another column is reset")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRowsAddedAndReplacedWhileFilteredStayShownWhenTheFilterOfAnotherColumnIsReset(TableType tableType) {
        Row rowNewB4T6 = new Row("B", "4", "T6");

        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T8, rowA1T9), getVisibleRows());

        addBackingRow(6, rowNewB3T7);
        replaceBackingRow(rowB2T8, rowNewB4T6);
        assertEquals(List.of(rowA1T8, rowNewB4T6, rowA1T9, rowNewB3T7), getVisibleRows());

        // The filter of the first column never saw the new rows, so it must not filter them out.
        resetColumnFilter(COLUMN_2);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB4T6, rowA1T9, rowNewB3T7), getVisibleRows());
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowNewB4T6, rowA1T9, rowB1T9, rowNewB3T7), getBackingRows());
    }

    @DisplayName("Rows which are equal to each other are all kept when a filter is reset")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRowsWhichAreEqualToEachOtherAreAllKeptWhenTheFilterOfAColumnIsReset(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA1Copy = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T9");
        Row valueRowB1 = new ValueRow("B", "1", "T8");
        List<Row> valueRows = List.of(valueRowA1, valueRowA1Copy, valueRowA2, valueRowB1);

        showTable(tableType, valueRows);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(valueRowA1, valueRowA1Copy, valueRowA2), getVisibleRows());

        applyFilter(COLUMN_3, "T8");
        assertEquals(List.of(valueRowA1, valueRowA1Copy), getVisibleRows());

        resetColumnFilter(COLUMN_3);

        assertTrue(isFiltered());
        assertEquals(List.of(valueRowA1, valueRowA1Copy, valueRowA2), getVisibleRows());
        assertEquals(valueRows, getBackingRows());
    }

    @DisplayName("Rows which are equal to each other keep their position when filtered")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testRowsWhichAreEqualToEachOtherKeepTheirPositionWhenFiltered(TableType tableType) {
        Row valueRowA1 = new ValueRow("A", "1", "T8");
        Row valueRowA2 = new ValueRow("A", "2", "T9");
        Row valueRowA1Copy = new ValueRow("A", "1", "T8");
        Row valueRowB1 = new ValueRow("B", "1", "T8");
        List<Row> valueRows = List.of(valueRowA1, valueRowA2, valueRowA1Copy, valueRowB1);

        showTable(tableType, valueRows);

        applyFilter(COLUMN_1, "A");

        assertEquals(List.of(valueRowA1, valueRowA2, valueRowA1Copy), getVisibleRows());
        assertEquals(valueRows, getBackingRows());
    }

    @DisplayName("Setting all backing rows of a filtered table shows new rows in order and keeps the visibility of the others")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testSettingAllBackingRowsOfAFilteredTableShowsNewRowsAndKeepsTheVisibilityOfTheOthers(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");
        assertEquals(List.of(rowA1T8, rowA2T8, rowA2T9, rowA1T9), getVisibleRows());

        // The rows are reordered, shown and hidden rows are removed and a new row is added.
        setBackingRows(List.of(rowA1T9, rowB1T9, rowA1T8, rowNewB3T7));

        assertTrue(isFiltered());
        assertEquals(List.of(rowA1T9, rowA1T8, rowNewB3T7), getVisibleRows());
        assertEquals(List.of(rowA1T9, rowB1T9, rowA1T8, rowNewB3T7), getBackingRows());
    }

    @DisplayName("Setting all backing rows of an unfiltered table with the same row multiple times shows exactly them")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testSettingAllBackingRowsOfAnUnfilteredTableWithTheSameRowMultipleTimesShowsExactlyThem(TableType tableType) {
        showTable(tableType, List.of(rowA1T8, rowB2T8, rowA1T8));

        setBackingRows(List.of(rowB2T8, rowA1T8));

        assertFalse(isFiltered());
        assertEquals(List.of(rowB2T8, rowA1T8), getVisibleRows());
    }

    @DisplayName("Sorting the backing rows of a filtered table keeps every filter on the same rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testSortingTheBackingRowsOfAFilteredTableKeepsEveryFilterOnTheSameRows(TableType tableType) {
        showTable(tableType);

        applyFilter(COLUMN_1, "A");

        runOnFxThread(() -> fixture.sortBackingRows(Comparator.comparing(Row::third).reversed()));
        assertEquals(List.of(rowA2T9, rowA1T9, rowA1T8, rowA2T8), getVisibleRows());

        applyFilter(COLUMN_2, "1");
        assertEquals(List.of(rowA1T9, rowA1T8), getVisibleRows());

        resetColumnFilter(COLUMN_2);

        assertTrue(isFiltered());
        assertEquals(List.of(rowA2T9, rowA1T9, rowA1T8, rowA2T8), getVisibleRows());
        assertEquals(List.of(rowA2T9, rowA1T9, rowB1T9, rowA1T8, rowA2T8, rowB2T8), getBackingRows());
    }

    @DisplayName("A value no row has anymore loses its filter entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTheEntryOfAValueIsRemovedFromTheFilterWhenNoRowHasItAnymore(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_1, List.of("A", "B"));

        commitValue(3, COLUMN_1, "A");
        commitValue(5, COLUMN_1, "A");

        withFilterPopup(COLUMN_1, filterPopup -> {
            assertEquals(List.of("A"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "A");
            assertEquals(selectedMessage(6, 6), selectionText(filterPopup));
        });
    }

    @DisplayName("The only value of an entry edited to a new one replaces that entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testTheOnlyValueOfAnEntryEditedToANewOneReplacesTheEntryInsideTheFilter(TableType tableType) {
        showTable(tableType, narrowableRows);
        assertFilterEntries(COLUMN_2, List.of("1", "2", "3"));

        // The fifth row is the only one with a '3'.
        commitValue(4, COLUMN_2, "4");

        withFilterPopup(COLUMN_2, filterPopup -> {
            assertEquals(List.of("1", "2", "4"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));

            selectEntries(filterPopup, "4");
            assertEquals(selectedMessage(1, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_2, "4");

        assertEquals(List.of(rowA3T2), getVisibleRows());
        assertEquals(narrowableRows, getBackingRows());
    }

    @DisplayName("Empty and null values share one filter entry")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testValuesEditedToAnEmptyOrNullValueShareOneEntryInsideTheFilter(TableType tableType) {
        showTable(tableType);
        assertFilterEntries(COLUMN_3, List.of("T8", "T9"));

        commitValue(0, COLUMN_3, "");
        commitValue(1, COLUMN_3, null);

        withFilterPopup(COLUMN_3, filterPopup -> {
            assertEquals(List.of("", "T9", "T8"), entryTexts(filterPopup));
            assertEquals(selectedMessage(0, 6), selectionText(filterPopup));
        });

        applyFilter(COLUMN_3, Set.of(""));

        assertEquals(List.of(rowA1T8, rowA2T8), getVisibleRows());
        assertEquals(rows, getBackingRows());
    }

    private void showTable(TableType tableType) {
        showTable(tableType, rows);
    }
}
