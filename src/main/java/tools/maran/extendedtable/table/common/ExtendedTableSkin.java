package tools.maran.extendedtable.table.common;

import java.util.function.ToDoubleFunction;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;

/// Bundles common methods extended table skins need to implement.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public interface ExtendedTableSkin<S> {
    /// Key for refreshing the table. That is every row with its cells.
    String REFRESH = "refresh";
    /// Key for autosizing columns. That is every column is sized to match the header width and/or the first 15 cells.
    String AUTOSIZE_COLUMNS = "autosizeColumns";
    /// Key for refreshing a column. That is every cell that belongs to the column.
    String REFRESH_COLUMN = "refreshColumn";
    /// Pseudo class for the table header when shown.
    PseudoClass PSEUDO_CLASS_SHOW_HEADER = PseudoClass.getPseudoClass("show-header");

    /// Returns the cell size property.
    ///
    /// @return the cell size property
    ObjectProperty<ToDoubleFunction<S>> cellSizeProviderProperty();

    /// Returns the fixed column count property.
    ///
    /// @return the fixed column count property
    IntegerProperty fixedColumnCountProperty();

    /// Returns the fixed column count.
    ///
    /// @return the fixed column count
    int getFixedColumnCount();

    /// Returns the value of the hbar.
    ///
    /// @return the value of the hbar
    double getHBarValue();

    /// Requests a cell layout which might be done when really needed.
    void requestCellLayout();
}
