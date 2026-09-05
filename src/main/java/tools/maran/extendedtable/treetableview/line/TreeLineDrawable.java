package tools.maran.extendedtable.treetableview.line;

import javafx.scene.Node;
import javafx.scene.control.TreeItem;

/// Interface for nodes which can provide tree information.
///
/// @author Marius Hanl
public interface TreeLineDrawable {

    /// Returns the height of the cell.
    ///
    /// @return the height of the cell
    double getCellHeight();

    /// Returns the width of the cell.
    ///
    /// @return the width of the cell
    double getCellWidth();

    /// Returns the disclosure node or null, if none.
    ///
    /// @return the disclosure node or null, if none.
    Node getDisclosureNode();

    /// Returns the indent.
    /// The indent is the amount of space to multiply by the treeItem level to get the left margin.
    ///
    /// @return the indent
    double getIndent();

    /// Returns the underlying [TreeItem].
    ///
    /// @return the [TreeItem]
    TreeItem<?> getTreeItem();

    /// Returns the number of levels of 'indentation' of the given TreeItem, based on how many times getParent() can
    /// be recursively called. If the given TreeItem is the root node or if the TreeItem does not have any parent set,
    /// the returned value will be zero. For each time getParent() is recursively called, the returned value is
    /// incremented by one.
    ///
    /// @param treeItem
    ///         the [TreeItem] for which the level is needed.
    /// @return an integer representing the number of parents above the given node, or -1 if the given TreeItem is null.
    int getTreeItemLevel(TreeItem<?> treeItem);

    /// Returns true if the root of the tree is shown, and false if it is not.
    ///
    /// @return true if the root of the tree is shown
    boolean isShowRoot();

}
