package tools.maran.extendedtable.treetableview.column;

import java.util.Comparator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.scene.control.TreeItem;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import tools.maran.extendedtable.filter.FilterPopupControl;
import tools.maran.extendedtable.filter.FilterPopupControl.Status;
import tools.maran.extendedtable.filter.FilterableColumn;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;

/// Abstract implementation of the [ExtendedTreeTableColumnBase] which adds filter support.
///
/// - Call [#setToStringConverter(Callback)] to specify how the filter items should be displayed. By default, this
///   also specifies how the table cells should render their text. The default is toString(),
///   which will work out of the box for String and all primitives, but otherwise should always be specified.
/// - Call [#setFromStringConverter(Callback)] to specify how a text which the user typed in and committed should be
///   converted to the specified object. There is no default for this, but subclasses may specify one.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
/// @author Marius Hanl
/// @see #getCellText(int)
public abstract class AbstractFilterTreeTableColumn<S, T> extends ExtendedTreeTableColumnBase<S, T>
        implements FilterableColumn {

    private EventHandler<WindowEvent> popupHiddenHandler;
    private WeakEventHandler<WindowEvent> weakPopupHiddenHandler;

    private FilterPopupControl<TreeItem<S>> filterPopupControl;
    private BooleanProperty filteringDisabled;
    private ReadOnlyBooleanWrapper filtered;

    private Callback<T, String> toStringConverter;
    private Callback<String, T> fromStringConverter;

    private Comparator<S> itemComparator;

    /// Creates a new [AbstractFilterTreeTableColumn] instance.
    protected AbstractFilterTreeTableColumn() {
        super();
    }

    /// Creates a new [AbstractFilterTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    protected AbstractFilterTreeTableColumn(String text) {
        super(text);
    }

    @Override
    public final ReadOnlyBooleanProperty filteredProperty() {
        return filteredPropertyImpl().getReadOnlyProperty();
    }

    /// Returns the filtering disabled property.
    ///
    /// @return the filtering disabled property
    public final BooleanProperty filteringDisabledProperty() {
        if (filteringDisabled == null) {
            filteringDisabled = new SimpleBooleanProperty(this, "filteringDisabled");
        }
        return filteringDisabled;
    }

    @Override
    public String getCellText(int rowIndex) {
        ObservableValue<T> cellObservableValue = getCellObservableValue(rowIndex);
        if (cellObservableValue == null) {
            return null;
        }

        T value = cellObservableValue.getValue();
        return convertToString(value);
    }

    @Override
    public final FilterPopupControl<TreeItem<S>> getFilterPopup() {
        if (filterPopupControl == null) {
            filterPopupControl = createFilterPopupControl();
        }
        return filterPopupControl;
    }

    /// Returns the item [Comparator].
    ///
    /// @return the item [Comparator]
    public final Comparator<S> getItemComparator() {
        return itemComparator;
    }

    public boolean isFiltered() {
        return filteredProperty().get();
    }

    @Override
    public final boolean isFilteringDisabled() {
        return filteringDisabled != null && filteringDisabled.get();
    }

    /// Called to tell the column filter to refresh. Similar how the table can be refreshed,
    /// this is needed when something changed where this column is not notified and therefore needs to refresh.
    public void refreshFilter() {
        if (filterPopupControl != null) {
            filterPopupControl.refreshItem(null);
        }
    }

    @Override
    public final void setFiltered(boolean filtered) {
        filteredPropertyImpl().set(filtered);
    }

    /// Sets the filtering disabled property.
    ///
    /// @param filteringDisabled
    ///         the filtering disabled property
    public final void setFilteringDisabled(boolean filteringDisabled) {
        filteringDisabledProperty().set(filteringDisabled);
    }

    /// Sets the converter which is used to convert a committed text back to the value of this column.
    ///
    /// @param stringConverter
    ///         the converter from a [String] to the target type
    public final void setFromStringConverter(Callback<String, T> stringConverter) {
        this.fromStringConverter = stringConverter;
    }

    /// Sets the item [Comparator]. By default, the item [Comparator] is used inside the filter popup.
    ///
    /// @param itemComparator
    ///         the item [Comparator]
    public final void setItemComparator(Comparator<S> itemComparator) {
        this.itemComparator = itemComparator;
    }

    /// Sets the converter which is used to display the value of this column as text.
    ///
    /// @param stringConverter
    ///         the converter from the target type to a [String]
    public final void setToStringConverter(Callback<T, String> stringConverter) {
        this.toStringConverter = stringConverter;
    }

    /// Converts the given text to a value using the [fromStringConverter][#setFromStringConverter(Callback)].
    ///
    /// @param item
    ///         the text to convert
    /// @return the converted value, or null if no converter is set
    protected final T convertFromString(String item) {
        if (fromStringConverter == null) {
            return null;
        }

        return fromStringConverter.call(item);
    }

    /// Converts the given value to a [String] using the [toStringConverter][#setToStringConverter(Callback)].
    ///
    /// @param item
    ///         the value to convert
    /// @return the converted value, or null if no converter is set
    protected final String convertToString(T item) {
        if (toStringConverter == null) {
            return null;
        }

        return toStringConverter.call(item);
    }

    /// Creates the [FilterPopupControl] which is shown when the filter of this column is opened.
    ///
    /// @return the newly created [FilterPopupControl]
    protected FilterPopupControl<TreeItem<S>> createFilterPopupControl() {
        FilterPopupControl<TreeItem<S>> popupControl = new FilterPopupControl<>();
        popupControl.fixedCellSizeProperty().bind(getTreeTableView().fixedCellSizeProperty());
        popupControl.setReadFunction(item -> convertToString(readValue(item.getValue())));
        if (getItemComparator() != null) {
            popupControl.setComparator((t1, t2) -> getItemComparator().compare(t1.getValue(), t2.getValue()));
        }

        popupHiddenHandler = _ -> doAction(popupControl.getStatus());
        weakPopupHiddenHandler = new WeakEventHandler<>(popupHiddenHandler);

        popupControl.setBackingItems(getBackingItems());
        popupControl.setItems(getItems());
        popupControl.setOnHidden(weakPopupHiddenHandler);

        return popupControl;
    }

    @Override
    protected void onBackingItemsChanged(ObservableList<TreeItem<S>> items) {
        if (filterPopupControl != null) {
            filterPopupControl.setBackingItems(items);
        }
    }

    @Override
    protected void onItemsChanged(ObservableList<TreeItem<S>> items) {
        if (filterPopupControl != null) {
            filterPopupControl.setItems(items);
        }
    }

    @Override
    protected void postCommit(TreeItem<S> item, T oldValue, T newValue) {
        if (filterPopupControl != null) {
            filterPopupControl.refreshItem(item);
        }
    }

    private void applyFilter() {
        getExtendedTreeTableView().filter(this, filterPopupControl.getSelectedItems());
    }

    private void doAction(Status status) {
        switch (status) {
            case APPLY -> applyFilter();
            case RESET_ALL -> resetFilter();
            case RESET_COLUMN -> resetColumnFilter();
            case UNCHANGED -> {
                // noop
            }
        }
    }

    private ReadOnlyBooleanWrapper filteredPropertyImpl() {
        if (filtered == null) {
            filtered = new ReadOnlyBooleanWrapper(this, "filtered");
        }
        return filtered;
    }

    private ObservableList<TreeItem<S>> getBackingItems() {
        return getExtendedTreeTableView().getBackingItems();
    }

    private ExtendedTreeTableView<S> getExtendedTreeTableView() {
        return (ExtendedTreeTableView<S>) getTreeTableView();
    }

    private void resetColumnFilter() {
        getExtendedTreeTableView().resetFilter(this);
    }

    private void resetFilter() {
        getExtendedTreeTableView().restoreBackingItems();
    }
}
