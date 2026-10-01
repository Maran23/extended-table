package tools.maran.extendedtable.filter;

import java.util.function.Supplier;

import javafx.beans.property.ReadOnlyBooleanProperty;

import tools.maran.extendedtable.filter.popup.FilterPopupControl;

/// Bundles common methods filterable columns need to implement.
///
/// @param <S>
///         the item type of the table
/// @author Marius Hanl
public interface FilterableColumn<S> {

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

    /// Returns the [FilterPopupControl], which is created by the
    /// [filter popup factory][#setFilterPopupFactory(Supplier)] on the first call.
    ///
    /// @return the [FilterPopupControl], or null if the factory is null or returns null
    FilterPopupControl<S> getFilterPopup();

    /// Returns whether the filtering of this column is disabled or not.
    ///
    /// @return true, when the filtering is disabled, false otherwise
    boolean isFilteringDisabled();

    /// Sets the factory which creates the [FilterPopupControl] when the filter of this column is opened
    /// for the first time. The column connects the created popup to the table afterward.
    ///
    /// The default factory creates a [FilterPopupControl]
    /// and binds its [FilterPopupControl#fixedCellSizeProperty()] to the fixed cell size of the table.
    ///
    /// @param filterPopupFactory
    ///         the filter popup factory
    void setFilterPopupFactory(Supplier<FilterPopupControl<S>> filterPopupFactory);

    /// Sets whether this column is currently filtered or not. Called by the table, which keeps track of the filters.
    ///
    /// @param filtered
    ///         true, when this column is filtered, false otherwise
    void setFiltered(boolean filtered);
}
