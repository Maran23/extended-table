package tools.maran.extendedtable.treetableview.column;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.Event;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;

import tools.maran.extendedtable.table.common.CommitEvent;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.table.validation.ValidatableColumn;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;

/// Abstract [TreeTableColumn] with a skeleton for all subclasses.
/// It extends the normal [TreeTableColumn] with validation support and notifies the subclasses when the underlying
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
public abstract class ExtendedTreeTableColumnBase<S, T> extends TreeTableColumn<S, T>
        implements ValidatableColumn<TreeItem<S>> {

    private final InvalidationListener treeTableViewListener = _ -> treeTableViewChanged();
    private final WeakInvalidationListener weakTreeTableViewListener = new WeakInvalidationListener(
            treeTableViewListener);

    private final InvalidationListener rootChangedListener = _ -> rootChanged();
    private final WeakInvalidationListener weakRootChangedListener = new WeakInvalidationListener(rootChangedListener);

    private final InvalidationListener backingItemsListener = _ -> backingItemsChanged();
    private final WeakInvalidationListener weakBackingItemsListener = new WeakInvalidationListener(
            backingItemsListener);

    private final ListChangeListener<TreeItem<S>> childrenListener = _ -> rootChildrenChanged();
    private final WeakListChangeListener<TreeItem<S>> weakChildrenListener = new WeakListChangeListener<>(
            childrenListener);

    private Callback<S, T> readFunction;
    private BiConsumer<S, T> writeFunction;

    private WeakReference<TreeTableView<S>> treeTableViewRef;
    private WeakReference<TreeItem<S>> treeTableRootRef;

    private BiPredicate<S, T> validator;

    /// Creates a new [ExtendedTreeTableColumnBase] instance.
    protected ExtendedTreeTableColumnBase() {
        this(null);
    }

    /// Creates a new [ExtendedTreeTableColumnBase] instance.
    ///
    /// @param text
    ///         the text
    protected ExtendedTreeTableColumnBase(String text) {
        super(text);

        setSortable(false);
        setEditable(false);
        addEventHandler(TreeTableColumn.<S, T>editCommitEvent(), this::doCommit);

        treeTableViewRef = new WeakReference<>(null);
        treeTableRootRef = new WeakReference<>(null);

        treeTableViewProperty().addListener(weakTreeTableViewListener);

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
    public final boolean validate(TreeItem<S> item) {
        if (validator == null) {
            return true;
        }

        S itemValue = item.getValue();
        return validator.test(itemValue, readValue(itemValue));
    }

    /// Initiate a commit.
    ///
    /// @param editEvent
    ///         the [CellEditEvent] where the value will be used from
    protected final void doCommit(CellEditEvent<S, T> editEvent) {
        // Normally these values should never be null, but since CellEditEvent works on the live table/data,
        // we can't guarantee it.
        if (editEvent.getTreeTablePosition() == null) {
            return;
        }

        TreeItem<S> treeItem = editEvent.getRowValue();
        if (treeItem == null) {
            return;
        }

        S item = treeItem.getValue();
        if (item == null) {
            return;
        }

        doCommit(treeItem, editEvent.getOldValue(), editEvent.getNewValue());
    }

    /// Returns the items of the table this column belongs to.
    ///
    /// @return the items of the table, or null if this column is not part of a table
    protected final ObservableList<TreeItem<S>> getItems() {
        TreeItem<S> root = treeTableRootRef.get();
        if (root == null) {
            return FXCollections.emptyObservableList();
        }

        return root.getChildren();
    }

    /// Initializes this column. Called once by the constructor after the column was set up.
    protected abstract void init();

    /// Hook up method for subclasses to always receive the current backing items.
    ///
    /// @param items
    ///         the new items
    protected abstract void onBackingItemsChanged(ObservableList<TreeItem<S>> items);

    /// Hook up method for subclasses to always receive the current table view items.
    ///
    /// @param items
    ///         the new items
    protected abstract void onItemsChanged(ObservableList<TreeItem<S>> items);

    /// Hook up method for subclasses which is called after a commit was performed.
    ///
    /// @param item
    ///         the item (row) that was committed
    /// @param oldValue
    ///         the value before the commit
    /// @param value
    ///         the value after the commit
    protected void postCommit(TreeItem<S> item, T oldValue, T value) {
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
        onBackingItemsChanged(((ExtendedTreeTableView<S>) getTreeTableView()).getBackingItems());
    }

    private void doCommit(TreeItem<S> treeItem, T oldValue, T newValue) {
        if (writeFunction == null) {
            return;
        }

        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        S item = treeItem.getValue();

        TreeTableView<S> treeTableView = getTreeTableView();

        Event.fireEvent(treeTableView,
                new CommitEvent<>(this, treeTableView, ExtendedTable.preCommitEvent(), treeItem));
        writeValue(item, newValue);
        Event.fireEvent(treeTableView, new CommitEvent<>(this, treeTableView, ExtendedTable.commitEvent(), treeItem));

        postCommit(treeItem, oldValue, newValue);
    }

    private void rootChanged() {
        TreeItem<S> oldRoot = treeTableRootRef.get();
        if (oldRoot != null) {
            oldRoot.getChildren().removeListener(weakChildrenListener);
        }

        TreeItem<S> root = getTreeTableView().getRoot();
        treeTableRootRef = new WeakReference<>(root);

        if (root != null) {
            root.getChildren().addListener(weakChildrenListener);
        }
        rootChildrenChanged();
    }

    private void rootChildrenChanged() {
        TreeItem<S> root = getTreeTableView().getRoot();

        if (root == null) {
            onItemsChanged(FXCollections.emptyObservableList());
            return;
        }

        onItemsChanged(root.getChildren());
    }

    private void treeTableViewChanged() {
        TreeTableView<S> newTreeTableView = getTreeTableView();

        TreeTableView<S> oldTreeTableView = treeTableViewRef.get();

        // No change, so we don't proceed further.
        if (oldTreeTableView == newTreeTableView) {
            return;
        }

        if (oldTreeTableView != null) {
            oldTreeTableView.rootProperty().removeListener(weakRootChangedListener);
            ((ExtendedTreeTableView<S>) oldTreeTableView).getBackingItems().removeListener(weakBackingItemsListener);

            TreeItem<S> oldRoot = treeTableRootRef.get();
            if (oldRoot != null) {
                oldRoot.getChildren().removeListener(weakChildrenListener);
            }
            treeTableRootRef = new WeakReference<>(null);
        }

        treeTableViewRef = new WeakReference<>(newTreeTableView);

        // This usually happens when the user re-orders a column or the column is removed.
        if (newTreeTableView == null) {
            return;
        }

        if (!(newTreeTableView instanceof ExtendedTreeTableView)) {
            throw new IllegalStateException("TreeTableView is not an ExtendedTreeTableView");
        }

        newTreeTableView.rootProperty().addListener(weakRootChangedListener);
        ((ExtendedTreeTableView<S>) newTreeTableView).getBackingItems().addListener(weakBackingItemsListener);

        backingItemsChanged();
        rootChanged();
    }

    private void writeValue(S item, T newValue) {
        if (writeFunction != null) {
            writeFunction.accept(item, newValue);
        }
    }

}
