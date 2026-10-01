package tools.maran.extendedtable.tableview.column;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;

import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.cell.CheckBoxTableCell;
import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;
import tools.maran.extendedtable.table.common.TableI18N;

/// Implementation of a [GenericTableColumn] with a [CheckBox] in the header that supports only [Boolean].
/// Null is considered an empty not changeable value.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class CheckBoxTableColumn<S> extends GenericTableColumn<S, Boolean> {

    private InvalidationListener columnsChangedListener;
    private WeakInvalidationListener weakColumnsChangedListener;

    private CheckBox checkBox;
    private boolean isSelectingAll;

    private BooleanProperty allSelectableProperty;

    /// Creates a new [CheckBoxTableColumn] instance.
    public CheckBoxTableColumn() {
        super();
    }

    /// Creates a new [CheckBoxTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public CheckBoxTableColumn(String text) {
        super(text);
    }

    @Override
    public void refreshFilter() {
        super.refreshFilter();

        decideCheckBoxState();
    }

    @Override
    protected ExtendedTableCell<S, Boolean> createTableCell() {
        return new CheckBoxTableCell<>();
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
                        tableViewProperty().flatMap(TableView::editableProperty).orElse(false)).and(editableProperty())
                .and(allSelectableProperty);
        checkBox.disableProperty().bind(checkBoxEditableBinding.not());

        columnsChangedListener = _ -> onNestedColumnsChanged(!getColumns().isEmpty());
        weakColumnsChangedListener = new WeakInvalidationListener(columnsChangedListener);

        getColumns().addListener(weakColumnsChangedListener);
    }

    @Override
    protected void onItemsChanged(ObservableList<S> items) {
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
    protected void postCommit(S item, Boolean oldValue, Boolean newValue) {
        super.postCommit(item, oldValue, newValue);

        if (isSelectingAll) {
            return;
        }

        decideCheckBoxState(newValue);
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

        Boolean opposite = !isSelected;
        boolean mixed = getItems().stream().anyMatch(item -> opposite.equals(readValue(item)));
        checkBox.setIndeterminate(mixed);
        if (!mixed) {
            checkBox.setSelected(isSelected);
        }
    }

    private void decideCheckBoxState() {
        if (!isCheckBoxUsed()) {
            return;
        }

        boolean unselectedFound = false;
        boolean selectedFound = false;

        for (S item : getItems()) {
            Boolean value = readValue(item);
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

    private boolean isCheckBoxUsed() {
        return checkBox != null && getGraphic() == checkBox;
    }

    private void selectAllItems(boolean newValue) {
        isSelectingAll = true;
        ObservableList<S> items = getItems();
        for (int index = 0; index < items.size(); index++) {
            Boolean oldValue = readValue(items.get(index));
            // We can not change a null value.
            if (oldValue != null) {
                // Fire an event the same way JavaFX does so that all listeners will be triggered.
                CellEditEvent<S, Boolean> editEvent = new CellEditEvent<>(getTableView(),
                        new TablePosition<>(getTableView(), index, this), TableColumn.editCommitEvent(), newValue);
                Event.fireEvent(this, editEvent);
            }
        }
        isSelectingAll = false;

        // Since we changed the state of all cells of this column, we need to refresh them.
        ((ExtendedTableView<S>) getTableView()).refreshColumn(this);
    }
}
