package tools.maran.extendedtable.filter;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.stage.PopupWindow;

/// Bundles common methods filterable columns need to implement.
///
/// @author Marius Hanl
public interface FilterableColumn {

    /// Returns whether this column is currently filtered or not.
    ///
    /// @return the [ReadOnlyBooleanProperty]
    ReadOnlyBooleanProperty filteredProperty();

    /// Returns a readable text for the cell which is shown at the given row index.
    ///
    /// @param rowIndex
    ///         the row index
    /// @return a readable text which is also displayed in the corresponding cell
    String getCellText(int rowIndex);

    /// Returns the filter [PopupWindow].
    ///
    /// @return the filter [PopupWindow]
    PopupWindow getFilterPopup();

    /// Returns whether the filtering of this column is disabled or not.
    ///
    /// @return true, when the filtering is disabled, false otherwise
    boolean isFilteringDisabled();

    /// Sets whether this column is currently filtered or not. Called by the table, which keeps track of the filters.
    ///
    /// @param filtered
    ///         true, when this column is filtered, false otherwise
    void setFiltered(boolean filtered);
}
