package tools.maran.extendedtable.filter.data;

import java.util.Comparator;
import java.util.List;

import javafx.scene.layout.Region;

import tools.maran.extendedtable.filter.FilterPopupControl;

/// Hides the concrete table, so a test runs against a set of methods both tables implement.
/// Every table has three editable [String] columns, one for each value of a [Row].
///
/// @author Marius Hanl
public interface FilterTableFixture {

    void addBackingRow(int index, Row row);

    /// Commits the value like the table does when a user finishes editing a cell.
    void commitValue(int rowIndex, int columnIndex, String value);

    /// Filters the column in code, so it only lets the given rows through.
    void filter(int columnIndex, List<Row> rows);

    List<Row> getBackingRows();

    FilterPopupControl<?> getFilterPopup(int columnIndex);

    Region getTable();

    List<Row> getVisibleRows();

    boolean isColumnFiltered(int columnIndex);

    boolean isFiltered();

    void moveColumn(int fromIndex, int toIndex);

    void refreshFilter(int columnIndex);

    void removeBackingRow(Row row);

    void removeColumn(int columnIndex);

    void replaceBackingRow(Row oldRow, Row newRow);

    void setBackingRows(List<Row> rows);

    void setColumnVisible(int columnIndex, boolean visible);

    /// Sets the comparator of the filter popup entries, before the popup is shown for the first time.
    void setItemComparator(int columnIndex, Comparator<Row> comparator);

    void setRows(List<Row> rows);

    void sortBackingRows(Comparator<Row> comparator);

}
