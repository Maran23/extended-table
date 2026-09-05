package tools.maran.extendedtable.filter;

import java.util.function.Supplier;

import javafx.beans.property.ReadOnlyBooleanProperty;

import tools.maran.extendedtable.filter.popup.FilterPopupControl;

/// [FilterableColumn] which is only used as key for the filter.
///
/// @author Marius Hanl
public final class KeyColumn<S> implements FilterableColumn<S> {

    public KeyColumn() {
    }

    @Override
    public ReadOnlyBooleanProperty filteredProperty() {
        return null;
    }

    @Override
    public String getCellText(int rowIndex) {
        return null;
    }

    @Override
    public FilterPopupControl<S> getFilterPopup() {
        return null;
    }

    @Override
    public boolean isFilteringDisabled() {
        return false;
    }

    @Override
    public void setFilterPopupFactory(Supplier<FilterPopupControl<S>> filterPopupFactory) {
        // Nothing to do.
    }

    @Override
    public void setFiltered(boolean filtered) {
        // Nothing to do.
    }
}
