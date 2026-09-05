package tools.maran.extendedtable.filter.random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.filter.FilterTableTestBase;
import tools.maran.extendedtable.filter.data.Row;
import tools.maran.extendedtable.filter.data.TableType;

/// Tests the filterable columns of every table with random sequences of actions.
///
/// @author Marius Hanl
class TableRandomTest extends FilterTableTestBase {

    private long seed;
    private Random random;

    @BeforeEach
    void createRandom() {
        seed = new Random().nextLong();
        random = new Random(seed);
    }

    @DisplayName("Random sequences of filter actions always show the expected rows")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void randomSequencesOfFilterActionsAlwaysShowTheExpectedRows(TableType tableType) {
        for (int run = 0; run < 10; run++) {
            randomSequenceOfFilterActions(tableType, run);
        }
    }

    /// Shows a new table and asserts after every random filter action that the expected rows are shown.
    private void randomSequenceOfFilterActions(TableType tableType, int run) {
        List<Row> backingRows = randomizableRows();
        showTable(tableType, backingRows);

        Map<Integer, Set<Row>> filteredOutRows = new HashMap<>();
        List<String> actions = new ArrayList<>();

        for (int step = 0; step < 10; step++) {
            assertDoesNotThrow(() -> performRandomAction(backingRows, filteredOutRows, actions),
                    () -> "seed: " + seed + ", run: " + run + ", actions: " + actions);

            String message = "seed: " + seed + ", run: " + run + ", actions: " + actions;
            assertEquals(rowsNotFilteredOut(backingRows, filteredOutRows), getVisibleRows(), message);
            assertEquals(backingRows, getBackingRows(), message);
            assertEquals(!filteredOutRows.isEmpty(), isFiltered(), message);
        }

        closeStage();
    }

    /// Performs a random filter action on the table, updates the expected rows and records it inside the actions.
    private void performRandomAction(List<Row> backingRows, Map<Integer, Set<Row>> filteredOutRows,
            List<String> actions) {
        List<Row> visibleRows = rowsNotFilteredOut(backingRows, filteredOutRows);
        int columnIndex = random.nextInt(3);

        switch (random.nextInt(20)) {
            case 0 -> {
                actions.add("resetColumn(" + columnIndex + ")");
                // The table is not filtered anymore when the remaining filters do not filter any row.
                if (filteredOutRows.remove(columnIndex) != null
                        && rowsNotFilteredOut(backingRows, filteredOutRows).size() == backingRows.size()) {
                    filteredOutRows.clear();
                }

                resetColumnFilter(columnIndex);
            }
            case 1 -> {
                actions.add("resetAll(" + columnIndex + ")");
                filteredOutRows.clear();

                resetAllFilters(columnIndex);
            }
            case 2 -> {
                actions.add("setRows()");
                filteredOutRows.clear();

                setRows(backingRows);
            }
            case 3 -> {
                if (!visibleRows.isEmpty()) {
                    int rowIndex = random.nextInt(visibleRows.size());
                    String value = "New" + random.nextInt(3);

                    actions.add("commit(" + rowIndex + ", " + columnIndex + ", " + value + ")");
                    commitValue(rowIndex, columnIndex, value);
                }
            }
            case 4 -> {
                actions.add("dismiss(" + columnIndex + ")");
                withFilterPopup(columnIndex, _ -> {
                });
            }
            case 5 -> {
                actions.add("refreshFilter(" + columnIndex + ")");
                refreshFilter(columnIndex);
            }
            case 6 -> {
                int index = random.nextInt(backingRows.size() + 1);
                Row row = randomRow();

                actions.add("add(" + index + ", " + row + ")");
                backingRows.add(index, row);

                addBackingRow(index, row);
            }
            case 7 -> {
                if (!backingRows.isEmpty()) {
                    Row row = backingRows.remove(random.nextInt(backingRows.size()));

                    actions.add("remove(" + row + ")");
                    removeBackingRow(row);
                }
            }
            case 8 -> {
                if (!backingRows.isEmpty()) {
                    Row newRow = randomRow();
                    Row oldRow = backingRows.set(random.nextInt(backingRows.size()), newRow);

                    actions.add("replace(" + oldRow + ", " + newRow + ")");
                    replaceBackingRow(oldRow, newRow);
                }
            }
            default -> {
                List<String> selectableValues = distinctValues(visibleRows, columnIndex);
                // Selecting every value is not a filter, so it can't be applied.
                if (selectableValues.size() < 2) {
                    return;
                }

                Set<String> selection = randomSelection(selectableValues);

                actions.add("apply(" + columnIndex + ", " + selection + ")");
                filteredOutRows.put(columnIndex,
                        rowsNotMatchingSelection(backingRows, columnIndex, selection));

                applyFilter(columnIndex, selection);
            }
        }
    }

    /// Returns rows with three different values in every column, so every column can be narrowed down multiple times.
    static List<Row> randomizableRows() {
        List<Row> rows = new ArrayList<>();
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                rows.add(new Row(String.valueOf("ABC".charAt(first)), String.valueOf(1 + second),
                        String.valueOf("XYZ".charAt((first + second) % 3))));
            }
        }
        return rows;
    }

    /// Returns a new row with random values out of the values of [#randomizableRows()].
    Row randomRow() {
        return new Row(String.valueOf("ABC".charAt(random.nextInt(3))), String.valueOf(1 + random.nextInt(3)),
                String.valueOf("XYZ".charAt(random.nextInt(3))));
    }

    /// Returns a non empty, strict subset of the given values, so the selection can be applied.
    Set<String> randomSelection(List<String> values) {
        List<String> shuffled = new ArrayList<>(values);
        Collections.shuffle(shuffled, random);

        return new LinkedHashSet<>(shuffled.subList(0, 1 + random.nextInt(values.size() - 1)));
    }

    /// Returns the distinct values of the given rows in the column, that is the entries of its filter popup.
    static List<String> distinctValues(List<Row> rows, int columnIndex) {
        return rows.stream().map(row -> value(row, columnIndex)).distinct().toList();
    }

    static List<Row> rowsNotFilteredOut(List<Row> backingRows, Map<Integer, Set<Row>> filteredOutRows) {
        return backingRows.stream()
                .filter(row -> filteredOutRows.values().stream().noneMatch(rows -> rows.contains(row)))
                .toList();
    }

    static Set<Row> rowsNotMatchingSelection(List<Row> backingRows, int columnIndex, Set<String> selection) {
        return new HashSet<>(backingRows.stream().filter(row -> !selection.contains(value(row, columnIndex))).toList());
    }

    static String value(Row row, int columnIndex) {
        return switch (columnIndex) {
            case COLUMN_1 -> row.first();
            case COLUMN_2 -> row.second();
            default -> row.third();
        };
    }
}
