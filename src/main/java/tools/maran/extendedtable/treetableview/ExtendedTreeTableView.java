package tools.maran.extendedtable.treetableview;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyEvent;

import tools.maran.extendedtable.filter.FilterableColumn;
import tools.maran.extendedtable.filter.TableFilter;
import tools.maran.extendedtable.table.common.CellKeyboard;
import tools.maran.extendedtable.table.common.CommitEvent;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.table.common.ExtendedTableSkin;
import tools.maran.extendedtable.table.validation.TableValidator;
import tools.maran.extendedtable.treetableview.column.AbstractFilterTreeTableColumn;
import tools.maran.extendedtable.treetableview.row.ExtendedTreeTableRow;

/// Extended [TreeTableView] with fixed columns and filter support.
///
/// # Implementation
///
/// The [ExtendedTreeTableView] can replace a normal [TreeTableView] and filtering will work out of the
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
/// # Modification
///
/// The [items][#getItems()] of this table might just be a sub list of the actual [backing items][#getBackingItems()].
/// Items added to or removed from them while filtered are added to or removed from the backing items as well.
/// Added items are inserted after the item shown before them, or at the start, and are always shown.
/// When the [items][#getItems()] should be replaced either use [#setRoot(TreeItem)] or [#setItems(ObservableList)].
/// The [#itemsProperty()] can not be bound. To keep the items in sync with another list, bind the content of the
/// [backing items][#getBackingItems()] instead: `Bindings.bindContent(table.getBackingItems(), items)`.
/// It is also possible to modify the backing items directly via [#getBackingItems()].
/// A change in the [backing items][#getBackingItems()] is immediately visible, no matter if the table is currently filtered or not.
/// That is, added (or replacing) items are always shown, while removed items disappear. Items which are removed and
/// added within the same change, e.g. via [ObservableList#setAll(java.util.Collection)], are not considered new and
/// keep their visibility. The table also stays filtered when every backing item is shown again due to such changes.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
/// @see #fixedColumnCountProperty()
/// @see #getItems()
/// @see #getBackingItems()
public class ExtendedTreeTableView<S> extends TreeTableView<S> implements ExtendedTable<TreeItem<S>> {

    private final TableFilter<TreeItem<S>> tableFilter = new TableFilter<>(this, this::showFilteredItems,
            this::resetValidator);

    private final CellKeyboard<TreeItem<S>> cellKeyboard = new CellKeyboard<>(this, this::getExpandedItemCount);

    private final ListChangeListener<TreeItem<S>> childrenListener = tableFilter::itemsChanged;
    private final WeakListChangeListener<TreeItem<S>> weakChildrenListener = new WeakListChangeListener<>(
            childrenListener);

    private final InvalidationListener rootInvalidatedListener = _ -> rootInvalidated();
    private final WeakInvalidationListener weakRootInvalidatedListener = new WeakInvalidationListener(
            rootInvalidatedListener);

    private final EventHandler<TreeModificationEvent<S>> treeItemsChangedHandler = this::addRemoveTreeItems;
    private final WeakEventHandler<TreeModificationEvent<S>> weakTreeItemsChangedHandler = new WeakEventHandler<>(
            treeItemsChangedHandler);

    private final Set<TreeItem<S>> allTreeItems;

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

    // If tree lines should be shown or not.
    private BooleanProperty showTreeLines;

    /// Items property just like the normal TableView. The items are always in sync with the root children.
    private ObjectProperty<ObservableList<TreeItem<S>>> items;

    private WeakReference<TreeItem<S>> rootRef;
    private TableValidator<TreeItem<S>> tableValidator;

    private boolean isInEdit = false;

    /// Creates a new [ExtendedTreeTableView] instance.
    public ExtendedTreeTableView() {
        getStyleClass().add("extended-tree-table-view");
        setRowFactory(_ -> new ExtendedTreeTableRow<>());
        setShowRoot(false);

        allTreeItems = new HashSet<>();

        rootProperty().addListener(weakRootInvalidatedListener);

        rootRef = new WeakReference<>(null);

        // Set a default empty root when everything is initialized.
        TreeItem<S> root = new TreeItem<>();
        root.setExpanded(true);
        setRoot(root);

        addEventFilter(ExtendedTable.commitEvent(), this::onCommit);

        addEventFilter(KeyEvent.KEY_PRESSED, cellKeyboard::handle);
        addEventFilter(KeyEvent.KEY_TYPED, cellKeyboard::handle);
    }

    /// Creates a new [ExtendedTreeTableView] instance.
    ///
    /// @param root
    ///         the [root][TreeItem]
    public ExtendedTreeTableView(TreeItem<S> root) {
        this();

        setRoot(root);
    }

    @Override
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
    public void edit(int row, TreeTableColumn<S, ?> column) {
        // An edit(..) may trigger another edit(..) again, which we do ignore here.
        if (isInEdit) {
            return;
        }

        isInEdit = true;
        super.edit(row, column);
        isInEdit = false;
    }

    @Override
    public void edit(int row, TableColumnBase<TreeItem<S>, ?> column) {
        edit(row, (TreeTableColumn<S, ?>) column);
    }

    @Override
    public BooleanProperty editingProperty() {
        if (editing == null) {
            editing = new SimpleBooleanProperty(this, "editing", false);
        }

        return editing;
    }

    @Override
    public void filter(FilterableColumn<TreeItem<S>> column, Collection<TreeItem<S>> filteredItems) {
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

    /// Returns all [TreeItem]s, regardless if they are visible (expanded) or not.
    ///
    /// @return all [TreeItem]s
    public final Collection<TreeItem<S>> getAllTreeItems() {
        return allTreeItems;
    }

    @Override
    public final ObservableList<TreeItem<S>> getBackingItems() {
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
    public TableColumnBase<TreeItem<S>, ?> getFocusedColumn() {
        return getFocusModel().getFocusedCell().getTableColumn();
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
    public final ObservableList<TreeItem<S>> getItems() {
        return itemsProperty().get();
    }

    /// Returns the values of the backing items, that is the [TreeItem]s of the root unwrapped to their value.
    ///
    /// @return the unwrapped backing items
    public List<S> getUnwrappedBackingItems() {
        return getRoot().getChildren().stream().map(TreeItem::getValue).toList();
    }

    @Override
    public String getUserAgentStylesheet() {
        return ExtendedTreeTableView.class.getResource("extended-tree-table-view.css").toExternalForm();
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

    /// Returns, whether tree lines should be shown or not.
    ///
    /// @return true, when tree lines should be shown, false otherwise
    public final boolean isShowTreeLines() {
        return showTreeLines != null && showTreeLinesProperty().get();
    }

    /// Check whether the item is valid or not.
    ///
    /// @param item
    ///         the item
    /// @return true, if the item is valid (validator)
    public final boolean isValid(TreeItem<S> item) {
        if (tableValidator == null) {
            return true;
        }
        return tableValidator.isValid(item);
    }

    /// Check whether the item for this [TreeTableColumn] is valid or not.
    ///
    /// @param item
    ///         the item
    /// @param treeTableColumn
    ///         the [TreeTableColumn]
    /// @return true, if the item is valid for the specified [TreeTableColumn] (validator)
    public final boolean isValid(TreeItem<S> item, TreeTableColumn<S, ?> treeTableColumn) {
        if (tableValidator == null) {
            return true;
        }
        return tableValidator.isValid(item, treeTableColumn);
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
    public final ObjectProperty<ObservableList<TreeItem<S>>> itemsProperty() {
        if (items == null) {
            items = new SimpleObjectProperty<>(this, "items") {
                @Override
                public void bind(ObservableValue<? extends ObservableList<TreeItem<S>>> observable) {
                    throw new UnsupportedOperationException("""
                            The items property must not be bound, as the table shows its own items to filter them. \
                            Use 'setItems(..)', or 'Bindings.bindContent(table.getBackingItems(), items)' to keep \
                            the items in sync.
                            """);
                }
            };
        }
        return items;
    }

    @Override
    public void refresh() {
        revalidate();
        refreshColumnFilters();
        refreshCells();
    }

    /// Refreshes a single [TreeTableColumn].
    ///
    /// @param treeTableColumn
    ///         a [TreeTableColumn]
    public final void refreshColumn(TreeTableColumn<S, ?> treeTableColumn) {
        getProperties().put(ExtendedTableSkin.REFRESH_COLUMN, treeTableColumn);
        refreshFilter(treeTableColumn);
    }

    @Override
    public void refreshColumnFilters() {
        for (TreeTableColumn<S, ?> column : getColumns()) {
            refreshFilter(column);
        }
    }

    /// Called to tell the column filter to refresh. Similar how the table can be refreshed,
    /// this is needed when something changed where this column is not notified and therefore needs to refresh.
    ///
    /// @param treeTableColumn
    ///         the [TreeTableColumn] which should be refreshed
    public void refreshFilter(TreeTableColumn<S, ?> treeTableColumn) {
        if (treeTableColumn instanceof AbstractFilterTreeTableColumn<?, ?> column) {
            column.refreshFilter();
        }
        for (TreeTableColumn<S, ?> column : treeTableColumn.getColumns()) {
            refreshFilter(column);
        }
    }

    @Override
    public final void resetFilter(FilterableColumn<TreeItem<S>> column) {
        tableFilter.resetFilter(column);
    }

    @Override
    public final void restoreBackingItems() {
        tableFilter.restoreBackingItems();
    }

    @Override
    public final void scrollIntoView(int index) {
        getProperties().put(ExtendedTableSkin.SCROLL_INTO_VIEW, index);
    }

    @Override
    public void scrollToColumn(TableColumnBase<TreeItem<S>, ?> column) {
        scrollToColumn((TreeTableColumn<S, ?>) column);
    }

    /// Revalidates all items if the validation is enabled.
    public void revalidate() {
        if (tableValidator != null) {
            tableValidator.revalidate(getAllTreeItems());
            tableValidator.evaluateValidity();
        }
    }

    /// Scrolls to the item. If the item does not exist, nothing will happen.
    ///
    /// @param item
    ///         the item
    public final void scrollTo(S item) {
        allTreeItems.stream().filter(treeItem -> Objects.equals(treeItem.getValue(), item)).findFirst()
                .map(this::getRow).filter(row -> row >= 0).ifPresent(this::scrollTo);
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
    public final void setItems(ObservableList<TreeItem<S>> items) {
        // Note: We do not need to change the itemsProperty() as it is already set to getRoot().getChildren() (reference).
        tableFilter.setItems(items == null ? List.of() : items);
    }

    @Override
    public void setOnCommit(EventHandler<CommitEvent<TreeItem<S>>> eventHandler) {
        setEventHandler(ExtendedTable.commitEvent(), eventHandler);
    }

    @Override
    public void setOnPreCommit(EventHandler<CommitEvent<TreeItem<S>>> eventHandler) {
        setEventHandler(ExtendedTable.preCommitEvent(), eventHandler);
    }

    @Override
    public final void setShowHeader(boolean showHeader) {
        showHeaderProperty().set(showHeader);
    }

    /// Sets, whether tree lines should be shown or not.
    ///
    /// @param showTreeLines
    ///         true, when tree lines should be shown, false otherwise
    public final void setShowTreeLines(boolean showTreeLines) {
        showTreeLinesProperty().set(showTreeLines);
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

    /// Returns the show tree lines property, which tells whether tree lines should be shown or not.
    ///
    /// @return the show tree lines property
    public final BooleanProperty showTreeLinesProperty() {
        if (showTreeLines == null) {
            showTreeLines = new SimpleBooleanProperty(this, "showTreeLines", false);
        }
        return showTreeLines;
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
                        tableValidator = new TableValidator<>(ExtendedTreeTableView.this.getVisibleLeafColumns(),
                                getAllTreeItems());

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
        return new ExtendedTreeTableViewSkin<>(this);
    }

    private void addRemoveTreeItems(TreeModificationEvent<S> e) {
        // Removed first, as items which are moved (e.g. by setAll(..)) are contained in both.
        e.getRemovedChildren().forEach(this::removeTreeItem);
        e.getAddedChildren().forEach(this::addTreeItem);
    }

    private void addTreeItem(TreeItem<S> treeItem) {
        allTreeItems.add(treeItem);
        treeItem.getChildren().forEach(this::addTreeItem);
    }

    private void onCommit(CommitEvent<TreeItem<S>> event) {
        if (tableValidator != null) {
            tableValidator.revalidate(event.getItem());
            tableValidator.evaluateValidity();
        }
    }

    /// Request the cells to refresh themselves.
    private void refreshCells() {
        getProperties().put(ExtendedTableSkin.REFRESH, true);
    }

    private void removeTreeItem(TreeItem<S> treeItem) {
        allTreeItems.remove(treeItem);
        treeItem.getChildren().forEach(this::removeTreeItem);
    }

    /// Revalidates all items if the validation is enabled.
    private void resetValidator() {
        if (tableValidator != null) {
            tableValidator.setAll(getAllTreeItems());
            tableValidator.evaluateValidity();
        }
    }

    private void rootInvalidated() {
        TreeItem<S> oldRoot = rootRef.get();

        if (oldRoot != null) {
            oldRoot.removeEventHandler(TreeItem.childrenModificationEvent(), weakTreeItemsChangedHandler);

            ObservableList<TreeItem<S>> oldChildren = oldRoot.getChildren();
            oldChildren.removeListener(weakChildrenListener);

            allTreeItems.clear();
        }

        TreeItem<S> root = getRoot();
        if (root == null) {
            rootRef = new WeakReference<>(null);
        } else {
            root.addEventHandler(TreeItem.childrenModificationEvent(), weakTreeItemsChangedHandler);

            ObservableList<TreeItem<S>> newChildren = root.getChildren();
            newChildren.addListener(weakChildrenListener);

            newChildren.forEach(this::addTreeItem);

            rootRef = new WeakReference<>(root);
        }

        ObservableList<TreeItem<S>> items = root == null ? null : root.getChildren();
        setItemsImpl(items);
        tableFilter.itemsReplaced(items);
    }

    private void setItemsImpl(ObservableList<TreeItem<S>> items) {
        itemsProperty().set(items);
    }


    /// Shows the items as children of the root, if there is one.
    private void showFilteredItems(List<TreeItem<S>> items) {
        TreeItem<S> root = getRoot();
        if (root != null) {
            root.getChildren().setAll(items);
        }
    }

    private ReadOnlyBooleanWrapper validPropertyImpl() {
        if (valid == null) {
            valid = new ReadOnlyBooleanWrapper(this, "valid", true);
        }
        return valid;
    }
}
