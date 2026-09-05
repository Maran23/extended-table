package tools.maran.extendedtable.tableview.cell;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TableCell;

import tools.maran.extendedtable.tableview.ExtendedTableView;

/// [TableCell] with extended functionality.
/// This cell improves and streamlines the default JavaFX editing workflow,
/// as well as having the opportunity to [#refresh()] the cell content
/// without further propagation to e.g. the VirtualFlow.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class ExtendedTableCell<S, T> extends TableCell<S, T> {

    private static final PseudoClass INVALID_PSEUDOCLASS = PseudoClass.getPseudoClass("invalid");

    /// Creates a new [ExtendedTableCell] instance.
    public ExtendedTableCell() {
        // Override the cell alignment as otherwise JavaFX is doing it in the layout of a table row for whatever reason.
        setAlignment(Pos.CENTER_LEFT);
    }

    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }

        Node focusOwner = getFocusOwner();

        super.cancelEdit();

        // A commitEdit(..) refreshes its cell (updateItem(..)), but a cancel does not, we therefore call it manually.
        updateItem(getItem(), isEmpty());

        requestFocusBackToTable(focusOwner);

        if (!isEditing() && getTableView() instanceof ExtendedTableView<S> tableView) {
            tableView.setEditing(false);
        }
    }

    @Override
    public void commitEdit(T newValue) {
        if (!isEditing()) {
            return;
        }

        Node focusOwner = getFocusOwner();

        // Will also call updateItem(..)
        super.commitEdit(newValue);

        requestFocusBackToTable(focusOwner);

        if (!isEditing() && getTableView() instanceof ExtendedTableView<S> tableView) {
            tableView.setEditing(false);
        }
    }

    /// Refreshes this cell by calling [#updateItem(Object, boolean)] with the newest value.
    /// Note that unused rows (index = -1) or empty rows (index >= count) are not updated.
    public void refresh() {
        int index = getIndex();
        if (index < 0 || index >= getTableView().getItems().size()) {
            return;
        }

        ObservableValue<T> currentObservableValue = getTableColumn().getCellObservableValue(index);
        final T newValue = currentObservableValue == null ? null : currentObservableValue.getValue();

        updateItem(newValue, false);
    }

    @Override
    public void startEdit() {
        if (isEditing()) {
            return;
        }

        // Shift focus to the table now, so other cells can cancel or commit their value.
        // Subclasses can request focus on their node, which will then override the table focus.
        getTableView().requestFocus();

        super.startEdit();

        if (isEditing() && getTableView() instanceof ExtendedTableView<S> tableView) {
            tableView.setEditing(true);
        }
    }

    /// Updates the valid state of this cell.
    /// Will call [#updateInvalidPseudoclass(boolean)] with the valid result.
    public final void updateValidState() {
        boolean isValid = true;
        if (getTableView() instanceof ExtendedTableView<S> tableView) {
            if (tableView.isValidationEnabled() && !isEmpty() && isEverythingEditable()) {
                isValid = tableView.isValid(getTableRow().getItem(), getTableColumn());
            }
        }

        updateInvalidPseudoclass(isValid);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ExtendedTableCellSkin<>(this);
    }

    @Override
    protected boolean isItemChanged(T oldItem, T newItem) {
        // JavaFX does not mark a cell as changed when the item was empty but is not anymore (table row != null)
        if (isEmpty() && getTableRow().getItem() != null) {
            return true;
        }

        return oldItem != newItem;
    }

    /// Returns whether this cell is considered valid currently.
    /// An invalid cell has the `:invalid` pseudo class.
    ///
    /// @return the valid flag
    protected final boolean isValid() {
        return !getPseudoClassStates().contains(INVALID_PSEUDOCLASS);
    }

    /// Updates the [#INVALID_PSEUDOCLASS] state with the provided valid flag.
    ///
    /// @param isValid
    ///         the valid flag
    protected void updateInvalidPseudoclass(boolean isValid) {
        pseudoClassStateChanged(INVALID_PSEUDOCLASS, !isValid);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        updateValidState();
    }

    private Node getFocusOwner() {
        return getScene() != null ? getScene().getFocusOwner() : null;
    }

    private boolean isEverythingEditable() {
        return isEditable() && getTableColumn().isEditable() && getTableRow().isEditable()
                && getTableView().isEditable();
    }

    private void requestFocusBackToTable(Node focusOwner) {
        // Request focus back to the table when the current focus owner has no parent anymore,
        // which happens if subclasses remove their graphic.
        // Otherwise, we may steal focus from another node the user just clicked.
        if (focusOwner == null || focusOwner.getScene() == null) {
            getTableView().requestFocus();
        }
    }
}
