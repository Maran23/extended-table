package tools.maran.extendedtable.tableview;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.List;
import java.util.function.ToDoubleFunction;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Pair;

import tools.maran.extendedtable.filter.FilterableColumn;
import tools.maran.extendedtable.filter.TableFilter;
import tools.maran.extendedtable.table.common.CommitEvent;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.table.common.ExtendedTableSkin;
import tools.maran.extendedtable.table.validation.TableValidator;
import tools.maran.extendedtable.tableview.column.AbstractFilterTableColumn;
import tools.maran.extendedtable.tableview.row.ExtendedTableRow;

/// Extended [TableView] with fixed columns and filter support.
///
/// # Implementation
///
/// The [ExtendedTableView] can replace a normal [TableView] and filtering will work out of the
/// box if [FilterableColumn]s are used. Additionally,
/// [#setFixedColumnCount(int)] can be used to set the amount of fixed columns of this table. The amount
/// counting from the first column will be fixed. Columns can be reordered, and the fixed columns change accordingly.
///
/// ## Filtering
///
/// The default (empty) items or items set via [#setItems(ObservableList)] are the 'backing' items, which is the
/// base for filtering. They can be retrieved by [#getBackingItems()] and can be used for further modification.
/// When items are filtered, the backing items can be restored via [#restoreBackingItems()].
/// A column can also be filtered in code via [#filter(FilterableColumn, java.util.Collection)] after the items were set,
/// e.g. to initially only show some items. This is the same as when the user filters the column.
///
/// # Limitation
///
/// Since the [items][#getItems()] of this table might just be a sub list of the actual [backing items][#getBackingItems()],
/// it is not allowed to either add or remove items to it. In fact this will throw an [IllegalStateException].
/// When the [items][#getItems()] should be replaced, use [#setItems(ObservableList)].
/// It is also possible to modify the backing items directly via [#getBackingItems()].
/// A change in the [backing items][#getBackingItems()] is immediately visible, no matter if the table is currently filtered or not.
/// That is, added (or replacing) items are always shown, while removed items disappear. Items which are removed and
/// added within the same change, e.g. via [ObservableList#setAll(java.util.Collection)], are not considered new and
/// keep their visibility. The table also stays filtered when every backing item is shown again due to such changes.
///
/// Items are tracked by identity, so the same item instance must not be contained multiple times in the backing items.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
/// @see #fixedColumnCountProperty()
/// @see #getItems()
/// @see #getBackingItems()
public class ExtendedTableView<S> extends TableView<S> implements ExtendedTable<S> {

    private final TableFilter<S> tableFilter = new TableFilter<>(this,
            items -> setItems(FXCollections.observableArrayList(items)), this::resetValidator);

    private final ListChangeListener<S> itemsChangedListener = tableFilter::itemsChanged;
    private final WeakListChangeListener<S> weakItemsChangedListener = new WeakListChangeListener<>(
            itemsChangedListener);

    private final InvalidationListener itemsInvalidatedListener = _ -> itemsInvalidated();
    private final WeakInvalidationListener weakItemsInvalidatedListener = new WeakInvalidationListener(
            itemsInvalidatedListener);

    // The number of columns that should be fixed starting from the first column.
    private IntegerProperty fixedColumnCount;
    // Provides the cell size of a row (item). Ignored when a fixed cell size is set.
    private ObjectProperty<ToDoubleFunction<S>> cellSizeProvider;

    private BooleanProperty editing;

    // If this table is valid or not. Specified by the column validators if the validation is enabled.
    private ReadOnlyBooleanWrapper valid;
    // If the table validation is enabled or not.
    private BooleanProperty validationEnabled;

    /// If the table header should be shown or not.
    private BooleanProperty showHeader;
    private ObservableList<Node> headerButtons;
    private StringProperty headerText;

    private WeakReference<ObservableList<S>> itemsRef;
    private TableValidator<S> tableValidator;

    private boolean isInEdit = false;

    /// Creates a new [ExtendedTableView] instance.
    public ExtendedTableView() {
        getStyleClass().add("extended-table-view");
        setRowFactory(_ -> new ExtendedTableRow<>());

        itemsProperty().addListener(weakItemsInvalidatedListener);

        // Attach listener to initial items set in the super constructor.
        itemsRef = new WeakReference<>(null);
        itemsInvalidated();
        addEventFilter(ExtendedTable.commitEvent(), this::onCommit);

        addEventFilter(KeyEvent.KEY_PRESSED, this::traverseToNextCell);
    }

    /// Creates a new [ExtendedTableView] instance.
    ///
    /// @param items
    ///         the items which are used as underlying table model
    public ExtendedTableView(ObservableList<S> items) {
        this();

        setItems(items);
    }

    /// Autosizes all [TableColumn]s.
    public final void autosizeColumns() {
        getProperties().put(ExtendedTableSkin.AUTOSIZE_COLUMNS, null);
    }

    /// Returns the cell size provider property, which provides the cell size of a row (item).
    /// If the cell size is 0 or smaller, the row will calculate the cell size as usual.
    ///
    /// **Note:** This property only has an effect, if the [#fixedCellSizeProperty()] value is not greater than 0 (e.g. -1).
    ///
    /// @return the cell size provider property
    public final ObjectProperty<ToDoubleFunction<S>> cellSizeProviderProperty() {
        if (cellSizeProvider == null) {
            cellSizeProvider = new SimpleObjectProperty<>(this, "cellSizeProvider");
        }
        return cellSizeProvider;
    }

    @Override
    public void edit(int row, TableColumn<S, ?> column) {
        // An edit(..) may trigger another edit(..) again, which we do ignore here.
        if (isInEdit) {
            return;
        }

        isInEdit = true;
        super.edit(row, column);
        isInEdit = false;
    }

    @Override
    public BooleanProperty editingProperty() {
        if (editing == null) {
            editing = new SimpleBooleanProperty(this, "editing", false);
        }

        return editing;
    }

    @Override
    public void filter(FilterableColumn<S> column, Collection<S> filteredItems) {
        tableFilter.filter(column, filteredItems);
    }

    /// Returns the filtered property, which tells whether this table is filtered or not.
    ///
    /// @return the filtered property
    public final ReadOnlyBooleanProperty filteredProperty() {
        return tableFilter.filteredProperty();
    }

    /// Returns the fixed column count property, that is the number of columns which should be fixed starting
    /// from the first column.
    ///
    /// @return the fixed column count property
    public final IntegerProperty fixedColumnCountProperty() {
        if (fixedColumnCount == null) {
            fixedColumnCount = new SimpleIntegerProperty(this, "fixedColumnCount", 0);
        }
        return fixedColumnCount;
    }

    @Override
    public final ObservableList<S> getBackingItems() {
        return tableFilter.getBackingItems();
    }

    /// Returns the cell size provider.
    ///
    /// @return the cell size provider
    public final ToDoubleFunction<S> getCellSizeProvider() {
        return cellSizeProvider == null ? _ -> -1 : cellSizeProviderProperty().get();
    }

    /// Returns the fixed column count.
    ///
    /// @return the fixed column count
    public final int getFixedColumnCount() {
        return fixedColumnCount == null ? 0 : fixedColumnCountProperty().get();
    }

    @Override
    public ObservableList<Node> getHeaderButtons() {
        if (headerButtons == null) {
            headerButtons = FXCollections.observableArrayList();
        }
        return headerButtons;
    }

    @Override
    public final String getHeaderText() {
        return headerText == null ? null : headerTextProperty().get();
    }

    @Override
    public String getUserAgentStylesheet() {
        return ExtendedTableView.class.getResource("extended-table-view.css").toExternalForm();
    }

    @Override
    public final StringProperty headerTextProperty() {
        if (headerText == null) {
            headerText = new SimpleStringProperty(this, "headerText", null);
        }
        return headerText;
    }

    @Override
    public boolean isEditing() {
        return editingProperty().get();
    }

    @Override
    public final boolean isFiltered() {
        return tableFilter.isFiltered();
    }

    @Override
    public final boolean isShowHeader() {
        return showHeader != null && showHeaderProperty().get();
    }

    /// Check whether the item is valid or not.
    ///
    /// @param item
    ///         the item
    /// @return true, if the item is valid (validator)
    public final boolean isValid(S item) {
        if (tableValidator == null) {
            return true;
        }
        return tableValidator.isValid(item);
    }

    /// Check whether the item for this [TableColumn] is valid or not.
    ///
    /// @param item
    ///         the item
    /// @param tableColumn
    ///         the [TableColumn]
    /// @return true, if the item is valid for the specified [TableColumn] (validator)
    public final boolean isValid(S item, TableColumn<S, ?> tableColumn) {
        if (tableValidator == null) {
            return true;
        }
        return tableValidator.isValid(item, tableColumn);
    }

    /// Returns whether this table is valid, see [#validProperty()].
    ///
    /// @return true, when this table is valid, false otherwise
    public final boolean isValid() {
        return valid == null || validProperty().get();
    }

    /// Returns whether the table validation is enabled.
    ///
    /// @return true, when the table validation is enabled, false otherwise
    public final boolean isValidationEnabled() {
        return validationEnabled != null && validationEnabledProperty().get();
    }

    @Override
    public void refresh() {
        revalidate();
        refreshColumnFilters();
        refreshCells();
    }

    /// Refreshes a single [TableColumn].
    ///
    /// @param tableColumn
    ///         a [TableColumn]
    public final void refreshColumn(TableColumn<S, ?> tableColumn) {
        getProperties().put(ExtendedTableSkin.REFRESH_COLUMN, tableColumn);
        refreshFilter(tableColumn);
    }

    @Override
    public void refreshColumnFilters() {
        for (TableColumn<S, ?> column : getColumns()) {
            refreshFilter(column);
        }
    }

    /// Called to tell the column filter to refresh. Similar how the table can be refreshed,
    /// this is needed when something changed where this column is not notified and therefore needs to refresh.
    ///
    /// @param tableColumn
    ///         the [TableColumn] which should be refreshed
    public void refreshFilter(TableColumn<S, ?> tableColumn) {
        if (tableColumn instanceof AbstractFilterTableColumn<?, ?> column) {
            column.refreshFilter();
        }
        for (TableColumn<S, ?> column : tableColumn.getColumns()) {
            refreshFilter(column);
        }
    }

    @Override
    public final void resetFilter(FilterableColumn<S> column) {
        tableFilter.resetFilter(column);
    }

    @Override
    public final void restoreBackingItems() {
        tableFilter.restoreBackingItems();
    }

    /// Revalidates all items if the validation is enabled.
    public void revalidate() {
        if (tableValidator != null) {
            tableValidator.revalidate(getItems());
            tableValidator.evaluateValidity();
        }
    }

    /// Sets the cell size provider.
    ///
    /// @param cellSizeProvider
    ///         the cell size provider
    public final void setCellSizeProvider(ToDoubleFunction<S> cellSizeProvider) {
        cellSizeProviderProperty().set(cellSizeProvider);
    }

    @Override
    public void setEditing(boolean isEditing) {
        editingProperty().set(isEditing);
    }

    /// Sets the fixed column count.
    ///
    /// @param count
    ///         the fixed column count
    public final void setFixedColumnCount(int count) {
        fixedColumnCountProperty().set(count);
    }

    @Override
    public final void setHeaderText(String headerText) {
        headerTextProperty().set(headerText);
    }

    @Override
    public void setOnCommit(EventHandler<CommitEvent<S>> eventHandler) {
        setEventHandler(ExtendedTable.commitEvent(), eventHandler);
    }

    @Override
    public void setOnPreCommit(EventHandler<CommitEvent<S>> eventHandler) {
        setEventHandler(ExtendedTable.preCommitEvent(), eventHandler);
    }

    @Override
    public final void setShowHeader(boolean showHeader) {
        showHeaderProperty().set(showHeader);
    }

    /// Sets the validation enabled flag.
    ///
    /// @param validationEnabled
    ///         the validation enabled flag
    public final void setValidationEnabled(boolean validationEnabled) {
        validationEnabledProperty().set(validationEnabled);
    }

    @Override
    public final BooleanProperty showHeaderProperty() {
        if (showHeader == null) {
            showHeader = new SimpleBooleanProperty(this, "showHeader", true);
        }
        return showHeader;
    }

    /// Returns the valid property, which tells whether this table is valid or not.
    /// It is specified by the column validators if the validation is enabled.
    ///
    /// @return the valid property
    public final ReadOnlyBooleanProperty validProperty() {
        return validPropertyImpl().getReadOnlyProperty();
    }

    /// Returns the validation enabled property, which tells whether the table validation is enabled or not.
    ///
    /// @return the validation enabled property
    public final BooleanProperty validationEnabledProperty() {
        if (validationEnabled == null) {
            validationEnabled = new SimpleBooleanProperty(this, "validationEnabled", false) {
                @Override
                protected void invalidated() {
                    if (get()) {
                        tableValidator = new TableValidator<>(ExtendedTableView.this.getVisibleLeafColumns(),
                                getItems());

                        validPropertyImpl().bind(tableValidator.validProperty());
                    } else {
                        validPropertyImpl().unbind();
                        tableValidator = null;
                    }
                    refreshCells();
                }
            };
        }
        return validationEnabled;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new ExtendedTableViewSkin<>(this);
    }

    /// Finds the previous or next cell to select/edit.
    /// In case we reach the last cell and want to select the next, we return the first column.
    ///
    /// @param visibleLeafColumns
    ///         all visible and editable leaf columns
    /// @param forward
    ///         true if we want to move forward (right); false otherwise (left)
    /// @return the next table column
    private Pair<TableColumn<S, ?>, Integer> findNextTablePosition(List<TableColumn<S, ?>> visibleLeafColumns,
            boolean forward) {
        TableColumn<S, ?> currentColumn = getFocusModel().getFocusedCell().getTableColumn();
        int nextColumnIndex = visibleLeafColumns.indexOf(currentColumn);
        int nextRowIndex = getSelectionModel().getSelectedIndex();

        if (forward) {
            nextColumnIndex++;
            if (nextColumnIndex > visibleLeafColumns.size() - 1) {
                nextColumnIndex = 0;
                nextRowIndex++;

                if (nextRowIndex == getItems().size()) {
                    nextRowIndex = 0;
                }
            }
        } else {
            nextColumnIndex--;
            if (nextColumnIndex < 0) {
                nextColumnIndex = visibleLeafColumns.size() - 1;
                nextRowIndex--;

                if (nextRowIndex == -1) {
                    nextRowIndex = getItems().size() - 1;
                }
            }
        }

        return new Pair<>(visibleLeafColumns.get(nextColumnIndex), nextRowIndex);
    }

    private void itemsInvalidated() {
        ObservableList<S> oldItems = itemsRef.get();

        if (oldItems != null) {
            oldItems.removeListener(weakItemsChangedListener);
        }

        ObservableList<S> newItems = getItems();
        if (newItems == null) {
            itemsRef = new WeakReference<>(null);
        } else {
            newItems.addListener(weakItemsChangedListener);
            itemsRef = new WeakReference<>(newItems);
        }

        tableFilter.itemsReplaced(newItems);
    }

    private void onCommit(CommitEvent<S> event) {
        if (tableValidator != null) {
            tableValidator.revalidate(event.getItem());
            tableValidator.evaluateValidity();
        }
    }

    /// Request the cells to refresh themselves.
    private void refreshCells() {
        getProperties().put(ExtendedTableSkin.REFRESH, true);
    }

    /// Revalidates all items if the validation is enabled.
    private void resetValidator() {
        if (tableValidator != null) {
            tableValidator.setAll(getItems());
            tableValidator.evaluateValidity();
        }
    }

    private void traverseToNextCell(KeyEvent event) {
        if (event.getCode() != KeyCode.TAB) {
            return;
        }

        List<TableColumn<S, ?>> visibleLeafColumns = getVisibleLeafColumns().stream()
                .filter(TableColumnBase::isEditable).toList();

        // There is no column that supports editing.
        if (visibleLeafColumns.isEmpty()) {
            return;
        }

        TableColumn<S, ?> currentColumn = getFocusModel().getFocusedCell().getTableColumn();
        if (currentColumn == null) {
            return;
        }

        boolean wasEditing = isEditing();
        boolean isForward = !event.isShiftDown();

        // Bounds the search when no cell can be edited.
        int remainingCells = getItems().size() * visibleLeafColumns.size();
        boolean isDone;
        Pair<TableColumn<S, ?>, Integer> tablePair;
        do {
            tablePair = findNextTablePosition(visibleLeafColumns, isForward);

            TableColumn<S, ?> nextColumn = tablePair.getKey();
            int nextRow = tablePair.getValue();

            // Flush any editing cell to commit or cancel his value by changing the focus and selection.
            getSelectionModel().focus(nextRow);
            getSelectionModel().select(nextRow, nextColumn);

            if (wasEditing) {
                edit(nextRow, nextColumn);

                isDone = isEditing();
            } else {
                isDone = true;
            }
        } while (!isDone && --remainingCells > 0);

        TableColumn<S, ?> nextColumn = tablePair.getKey();
        scrollToColumn(getVisibleLeafColumn(getVisibleLeafIndex(nextColumn) - 1));
        event.consume();
    }

    private ReadOnlyBooleanWrapper validPropertyImpl() {
        if (valid == null) {
            valid = new ReadOnlyBooleanWrapper(this, "valid", true);
        }
        return valid;
    }
}
