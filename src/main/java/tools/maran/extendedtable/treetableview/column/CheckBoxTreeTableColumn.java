package tools.maran.extendedtable.treetableview.column;

import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableView;

import tools.maran.extendedtable.table.common.TableI18N;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.cell.CheckBoxTreeTableCell;
import tools.maran.extendedtable.treetableview.cell.ExtendedTreeTableCell;

/// Implementation of a [GenericTreeTableColumn] with a [CheckBox] in the header that supports only [Boolean].
/// Null is considered an empty not changeable value.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class CheckBoxTreeTableColumn<S> extends GenericTreeTableColumn<S, Boolean> {

    private InvalidationListener columnsChangedListener;
    private WeakInvalidationListener weakColumnsChangedListener;

    private CheckBox checkBox;
    private boolean isSelectingAll;

    private BooleanProperty allSelectableProperty;

    /// Creates a new [CheckBoxTreeTableColumn] instance.
    public CheckBoxTreeTableColumn() {
        super();
    }

    /// Creates a new [CheckBoxTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public CheckBoxTreeTableColumn(String text) {
        super(text);
    }

    @Override
    public void refreshFilter() {
        super.refreshFilter();

        decideCheckBoxState();
    }

    @Override
    protected ExtendedTreeTableCell<S, Boolean> createTreeTableCell() {
        return new CheckBoxTreeTableCell<>();
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(
                bool -> bool == null ? "" : bool ? TableI18N.message("selected") : TableI18N.message("unselected"));

        checkBox = new CheckBox();
        checkBox.setOnAction(_ -> selectAllItems(checkBox.isSelected()));
        setGraphic(checkBox);

        allSelectableProperty = new SimpleBooleanProperty(true);

        BooleanBinding checkBoxEditableBinding = BooleanExpression.booleanExpression(
                        treeTableViewProperty().flatMap(TreeTableView::editableProperty).orElse(false)).and(editableProperty())
                .and(allSelectableProperty);
        checkBox.disableProperty().bind(checkBoxEditableBinding.not());

        columnsChangedListener = _ -> onNestedColumnsChanged(!getColumns().isEmpty());
        weakColumnsChangedListener = new WeakInvalidationListener(columnsChangedListener);

        getColumns().addListener(weakColumnsChangedListener);
    }

    @Override
    protected void onItemsChanged(ObservableList<TreeItem<S>> items) {
        super.onItemsChanged(items);

        decideCheckBoxState();
    }

    /// Shows or hides the header [CheckBox] depending on whether this column has nested columns.
    /// A column with nested columns has no cells of its own, so no [CheckBox] is shown.
    ///
    /// @param hasNestedColumns
    ///         true, when this column has nested columns, false otherwise
    protected void onNestedColumnsChanged(boolean hasNestedColumns) {
        if (hasNestedColumns) {
            setGraphic(null);
        } else {
            setGraphic(checkBox);

            decideCheckBoxState();
        }
    }

    @Override
    protected void postCommit(TreeItem<S> item, Boolean oldValue, Boolean newValue) {
        super.postCommit(item, oldValue, newValue);

        if (isSelectingAll) {
            return;
        }

        decideCheckBoxState(newValue);
    }

    private boolean allItemsSelected() {
        return getItems().parallelStream().map(tr -> readValue(tr.getValue())).filter(Objects::nonNull)
                .allMatch(bool -> bool);
    }

    private boolean allItemsUnselected() {
        return getItems().parallelStream().map(tr -> readValue(tr.getValue())).filter(Objects::nonNull)
                .noneMatch(bool -> bool);
    }

    private void decideCheckBoxState() {
        if (!isCheckBoxUsed()) {
            return;
        }

        boolean unselectedFound = false;
        boolean selectedFound = false;

        for (TreeItem<S> item : getItems()) {
            Boolean value = readValue(item.getValue());
            // We can not determine a null value.
            if (value == null) {
                continue;
            }

            if (value) {
                selectedFound = true;
            } else {
                unselectedFound = true;
            }

            if (selectedFound && unselectedFound) {
                allSelectableProperty.set(true);
                checkBox.setIndeterminate(true);
                return;
            }
        }

        if (!selectedFound && !unselectedFound) {
            allSelectableProperty.set(false);
            checkBox.setIndeterminate(false);
            checkBox.setSelected(false);
            return;
        }

        allSelectableProperty.set(true);
        checkBox.setIndeterminate(false);
        checkBox.setSelected(selectedFound);
    }

    /// Faster method than [#decideCheckBoxState()] as we already have a hint of what we need to check.
    ///
    /// @param isSelected
    ///         the selected flag
    private void decideCheckBoxState(Boolean isSelected) {
        if (!isCheckBoxUsed()) {
            return;
        }

        if (isSelected == null) {
            // We need to call the expensive method as the state is more complex.
            decideCheckBoxState();
            return;
        }

        if (isSelected) {
            if (allItemsSelected()) {
                checkBox.setIndeterminate(false);
                checkBox.setSelected(true);
                return;
            }
        } else {
            if (allItemsUnselected()) {
                checkBox.setIndeterminate(false);
                checkBox.setSelected(false);
                return;
            }
        }

        checkBox.setIndeterminate(true);
    }

    private boolean isCheckBoxUsed() {
        return checkBox != null && getGraphic() == checkBox;
    }

    private void selectAllItems(boolean newValue) {
        TreeTableView<S> treeTableView = getTreeTableView();

        isSelectingAll = true;
        // TreeTableView.getRow() is linear, so the row is tracked and only looked up after an expanded item.
        int row = -1;
        for (TreeItem<S> item : getItems()) {
            if (row < 0) {
                row = treeTableView.getRow(item);
            }

            Boolean oldValue = readValue(item.getValue());
            // We can not change a null value.
            if (oldValue != null) {
                // Fire an event the same way JavaFX does so that all listeners will be triggered.
                CellEditEvent<S, Boolean> editEvent = new CellEditEvent<>(treeTableView,
                        new TreeTablePosition<>(treeTableView, row, this), TreeTableColumn.editCommitEvent(), newValue);
                Event.fireEvent(this, editEvent);
            }

            row = item.isExpanded() && !item.isLeaf() ? -1 : row + 1;
        }
        isSelectingAll = false;

        // Since we changed the state of all cells of this column, we need to refresh them.
        ((ExtendedTreeTableView<S>) treeTableView).refreshColumn(this);
    }

}
