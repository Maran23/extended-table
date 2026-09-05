package tools.maran.extendedtable.table.header;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkinBase;

import tools.maran.extendedtable.table.common.ExtendedTableSkin;

/// Extended [TableHeaderRow] which supports fixed columns.
///
/// @author Marius Hanl
public class ExtendedTableHeaderRow extends TableHeaderRow {

    private final ChangeListener<Number> fixedColumnChangeListener = (_, _, _) -> getRootHeader().requestLayout();
    private final WeakChangeListener<Number> weakFixedColumnChangeListener = new WeakChangeListener<>(
            fixedColumnChangeListener);

    private ExtendedTableSkin<?> extendedTableSkin;

    /// Creates a new [ExtendedTableHeaderRow] instance.
    ///
    /// @param skin
    ///         the [TableViewSkinBase]
    public ExtendedTableHeaderRow(TableViewSkinBase<?, ?, ?, ?, ?> skin) {
        super(skin);

        if (skin instanceof ExtendedTableSkin<?> tableSkin) {
            extendedTableSkin = tableSkin;
            // Request layout so the fixed columns will be removed/added.
            extendedTableSkin.fixedColumnCountProperty().addListener(weakFixedColumnChangeListener);
        }
    }

    @Override
    protected ExtendedRootHeader createRootHeader() {
        return new ExtendedRootHeader();
    }

    @Override
    protected void updateScrollX() {
        // Layout fixed columns when the user scrolls the table.
        ((ExtendedRootHeader) getRootHeader()).layoutFixedColumns();

        super.updateScrollX();

        if (extendedTableSkin != null) {
            extendedTableSkin.requestCellLayout();
        }
    }
}
