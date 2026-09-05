package tools.maran.extendedtable.treetableview.row;

import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;

/// Implementation of the [TreeTableRow] which uses the [ExtendedTreeTableRowSkin] to support fixed cells.
/// This row also has the opportunity to [#refresh()] the cell content
/// without further propagation to e.g. the VirtualFlow.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class ExtendedTreeTableRow<S> extends TreeTableRow<S> {

    /// Creates a new [ExtendedTreeTableRow] instance.
    public ExtendedTreeTableRow() {
    }

    /// Refreshes this row by calling [#updateItem(Object, boolean)] with the newest value.
    /// Note that unused rows (index = -1) or empty rows (index >= count) are not updated.
    public void refresh() {
        int index = getIndex();
        if (index < 0 || index >= getTreeTableView().getExpandedItemCount()) {
            return;
        }

        TreeItem<S> newTreeItem = getTreeTableView().getTreeItem(index);
        if (getTreeItem() != newTreeItem) {
            updateTreeItem(newTreeItem);
        }

        updateItem(newTreeItem.getValue(), false);
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
                if (node instanceof TreeTableCell<?, ?> treeTableCell) {
                    treeTableCell.updateIndex(newIndex);
                }
            }
        }
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ExtendedTreeTableRowSkin<>(this);
    }

    @Override
    protected boolean isItemChanged(S oldItem, S newItem) {
        return oldItem != newItem;
    }

}
