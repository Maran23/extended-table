package tools.maran.extendedtable.filter;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.stage.PopupWindow;

/// [FilterableColumn] which is only used as key for the filter.
///
/// @author Marius Hanl
public final class KeyColumn implements FilterableColumn {

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
    public PopupWindow getFilterPopup() {
        return null;
    }

    @Override
    public boolean isFilteringDisabled() {
        return false;
    }

    @Override
    public void setFiltered(boolean filtered) {
        // Nothing to do.
    }
}
