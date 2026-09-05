package tools.maran.extendedtable.tableview.column;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

import tools.maran.extendedtable.table.common.CommitEvent;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.table.validation.ValidatableColumn;
import tools.maran.extendedtable.tableview.ExtendedTableView;

/// Abstract [TableColumn] with a skeleton for all subclasses.
/// It extends the normal [TableColumn] with validation support and notifies the subclasses when the underlying
/// table has any item changes.
///
/// - Call [#setReadFunction(Callback)] to specify how this column can transform the value from S to T.
///   This value is propagated to the cells, that may need to transform T to a readable string to display something, when needed.
///   This can be overridden or reused by setting a custom [cellFactory][#setCellFactory(Callback)].
/// - Call [#setWriteFunction(BiConsumer)] to specify what this column should do when a commit is triggered by the user.
///   A handler is registered that will trigger when a commit event is fired and therefore execute the write function (when editable).
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
/// @author Marius Hanl
public abstract class ExtendedTableColumnBase<S, T> extends TableColumn<S, T> implements ValidatableColumn<S> {

    private final InvalidationListener tableViewListener = _ -> tableViewChanged();
    private final WeakInvalidationListener weakTableViewListener = new WeakInvalidationListener(tableViewListener);

    private final ListChangeListener<S> itemsListener = _ -> onItemsChanged(getTableView().getItems());
    private final WeakListChangeListener<S> weakItemsListener = new WeakListChangeListener<>(itemsListener);

    private final InvalidationListener backingItemsListener = _ -> backingItemsChanged();
    private final WeakInvalidationListener weakBackingItemsListener = new WeakInvalidationListener(
            backingItemsListener);

    private final InvalidationListener tableItemsListener = _ -> itemsChanged();
    private final WeakInvalidationListener weakTableItemsListener = new WeakInvalidationListener(tableItemsListener);

    private Callback<S, T> readFunction;
    private BiConsumer<S, T> writeFunction;

    private WeakReference<TableView<S>> tableViewRef;
    private WeakReference<ObservableList<S>> tableItemsRef;

    private BiPredicate<S, T> validator;

    /// Creates a new [ExtendedTableColumnBase] instance.
    protected ExtendedTableColumnBase() {
        this(null);
    }

    /// Creates a new [ExtendedTableColumnBase] instance.
    ///
    /// @param text
    ///         the text
    protected ExtendedTableColumnBase(String text) {
        super(text);

        setSortable(false);
        setEditable(false);
        addEventHandler(TableColumn.<S, T>editCommitEvent(), this::doCommit);

        tableViewRef = new WeakReference<>(null);
        tableItemsRef = new WeakReference<>(null);
        tableViewProperty().addListener(weakTableViewListener);

        init();
    }

    /// Returns the validator.
    ///
    /// @return the validator
    public final BiPredicate<S, T> getValidator() {
        return validator;
    }

    /// Sets the read function which transforms the item (row) into the value of this column.
    ///
    /// @param readFunction
    ///         the read function
    public final void setReadFunction(Callback<S, T> readFunction) {
        this.readFunction = readFunction;
    }

    /// Sets the validator which supplies the item (row) and its value (cell).
    ///
    /// @param validator
    ///         the validator
    public final void setValidator(BiPredicate<S, T> validator) {
        this.validator = validator;
    }

    /// Sets the validator which supplies the value (cell).
    ///
    /// @param validator
    ///         the validator
    public final void setValidator(Predicate<T> validator) {
        this.validator = (_, value) -> validator.test(value);
    }

    /// Sets the write function which is called with the item (row) and the new value when a commit is performed.
    /// Setting a write function makes this column editable, removing it makes it read only again.
    ///
    /// @param writeFunction
    ///         the write function
    public final void setWriteFunction(BiConsumer<S, T> writeFunction) {
        this.writeFunction = writeFunction;

        setEditable(writeFunction != null);
    }

    @Override
    public final boolean validate(S item) {
        if (validator == null) {
            return true;
        }

        return validator.test(item, readValue(item));
    }

    /// Initiate a commit.
    ///
    /// @param editEvent
    ///         the [CellEditEvent] where the value will be used from
    protected final void doCommit(CellEditEvent<S, T> editEvent) {
        // Normally these values should never be null, but since CellEditEvent works on the live table/data,
        // we can't guarantee it.
        if (editEvent.getTablePosition() == null) {
            return;
        }

        S item = editEvent.getRowValue();
        if (item == null) {
            return;
        }

        doCommit(item, editEvent.getOldValue(), editEvent.getNewValue());
    }

    /// Returns the items of the table this column belongs to.
    ///
    /// @return the items of the table, or null if this column is not part of a table
    protected final ObservableList<S> getItems() {
        return tableItemsRef.get();
    }

    /// Initializes this column. Called once by the constructor after the column was set up.
    protected abstract void init();

    /// Hook up method for subclasses to always receive the current backing items.
    ///
    /// @param items
    ///         the new items
    protected abstract void onBackingItemsChanged(ObservableList<S> items);

    /// Hook up method for subclasses to always receive the current table view items.
    ///
    /// @param items
    ///         the new items
    protected abstract void onItemsChanged(ObservableList<S> items);

    /// Hook up method for subclasses which is called after a commit was performed.
    ///
    /// @param item
    ///         the item (row) that was committed
    /// @param oldValue
    ///         the value before the commit
    /// @param value
    ///         the value after the commit
    protected void postCommit(S item, T oldValue, T value) {
        // noop
    }

    /// Calls the read function of this column to get the value of the given item.
    ///
    /// @param item
    ///         the item (row)
    /// @return the column value, or null if no read function is set
    protected final T readValue(S item) {
        if (readFunction == null) {
            return null;
        }

        return readFunction.call(item);
    }

    private void backingItemsChanged() {
        onBackingItemsChanged(((ExtendedTableView<S>) getTableView()).getBackingItems());
    }

    private void doCommit(S item, T oldValue, T newValue) {
        if (writeFunction == null) {
            return;
        }

        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        TableView<S> tableView = getTableView();

        Event.fireEvent(tableView, new CommitEvent<>(this, tableView, ExtendedTable.preCommitEvent(), item));
        writeValue(item, newValue);
        Event.fireEvent(tableView, new CommitEvent<>(this, tableView, ExtendedTable.commitEvent(), item));

        postCommit(item, oldValue, newValue);
    }

    private void itemsChanged() {
        ObservableList<S> oldTableItems = tableItemsRef.get();
        if (oldTableItems != null) {
            oldTableItems.removeListener(weakItemsListener);
        }

        ObservableList<S> items = getTableView().getItems();
        tableItemsRef = new WeakReference<>(items);

        if (items != null) {
            items.addListener(weakItemsListener);
        }

        onItemsChanged(items);
    }

    private void tableViewChanged() {
        TableView<S> oldTableView = tableViewRef.get();
        TableView<S> newTableView = getTableView();

        // No change, so we don't proceed further.
        if (oldTableView == newTableView) {
            return;
        }

        if (oldTableView != null) {
            ((ExtendedTableView<S>) oldTableView).getBackingItems().removeListener(weakBackingItemsListener);
            oldTableView.itemsProperty().removeListener(weakTableItemsListener);

            ObservableList<S> oldTableItems = tableItemsRef.get();
            if (oldTableItems != null) {
                oldTableItems.removeListener(weakItemsListener);
            }
            tableItemsRef = new WeakReference<>(null);
        }

        tableViewRef = new WeakReference<>(newTableView);

        // This usually happens when the user re-orders a column or the column is removed.
        if (newTableView == null) {
            return;
        }

        if (!(newTableView instanceof ExtendedTableView)) {
            throw new IllegalStateException("TableView is not an ExtendedTableView");
        }

        ((ExtendedTableView<S>) newTableView).getBackingItems().addListener(weakBackingItemsListener);
        newTableView.itemsProperty().addListener(weakTableItemsListener);

        backingItemsChanged();
        itemsChanged();
    }

    private void writeValue(S item, T newValue) {
        if (writeFunction != null) {
            writeFunction.accept(item, newValue);
        }
    }

}
