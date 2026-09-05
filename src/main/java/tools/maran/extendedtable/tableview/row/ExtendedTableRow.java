package tools.maran.extendedtable.tableview.row;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;

/// Implementation of the [TableRow] which uses the [ExtendedTableRowSkin] to support fixed cells.
/// This row also has the opportunity to [#refresh()] the cell content
/// without further propagation to e.g. the VirtualFlow.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class ExtendedTableRow<S> extends TableRow<S> {

    /// Creates a new [ExtendedTableRow] instance.
    public ExtendedTableRow() {
    }

    /// Refreshes this row by calling [#updateItem(Object, boolean)] with the newest value.
    /// Note that unused rows (index = -1) or empty rows (index >= count) are not updated.
    public void refresh() {
        int index = getIndex();
        ObservableList<S> items = getTableView().getItems();
        if (items == null) {
            return;
        }

        if (index < 0 || index >= items.size()) {
            return;
        }

        S newItem = items.get(index);
        updateItem(newItem, false);
    }

    @Override
    public void updateIndex(int newIndex) {
        int oldIndex = getIndex();
        S oldItem = getItem();

        super.updateIndex(newIndex);

        // This is a workaround for a JavaFX bug.
        // It affects us when we filter the bottom items (e.g. 63, 64, 65) while also scrolled to the bottom.
        // Those entries will then be at the top (0, 1, 2).
        // -----------------------------------------------
        // What happens here is:
        // - The index changes (e.g. from 65 -> 2)
        // - The item is the same
        // -> JavaFX does NOT update the underlying cell indices (only this row), as there is no item change.
        // So in the example above, the row has index 2, the cells still index 65.
        // This does not happen often, but when it happens everything is literally unusable,
        // and it can not be fixed by the user.
        if (!isEmpty() && oldIndex != newIndex && oldItem == getItem()) {
            for (Node node : getChildren()) {
                if (node instanceof TableCell<?, ?> tableCell) {
                    tableCell.updateIndex(newIndex);
                }
            }
        }
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ExtendedTableRowSkin<>(this);
    }

    @Override
    protected boolean isItemChanged(S oldItem, S newItem) {
        return oldItem != newItem;
    }

}
