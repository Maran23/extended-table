package tools.maran.extendedtable.filter.popup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.Styleable;
import javafx.scene.Node;
import javafx.scene.control.PopupControl;
import javafx.scene.control.Skin;
import javafx.util.Callback;

import tools.maran.extendedtable.filter.popup.strategy.FilterStrategy;

/// Implementation of a [PopupControl] specialized for filtering a given amount of items.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class FilterPopupControl<S> extends PopupControl {

    /// Property key to inform the skin that an item should be refreshed.
    protected static final String REFRESH_ITEM = "refreshItemKey";

    private Callback<S, String> readFunction;

    private DoubleProperty fixedCellSize;
    private ObjectProperty<Comparator<S>> comparator;
    private ObjectProperty<FilterStrategy> filterStrategy;

    private final ObservableList<S> items = FXCollections.observableList(new ArrayList<>(128));
    private final ObservableList<S> backingItems = FXCollections.observableList(new ArrayList<>(128));

    private ObservableList<S> selectedItems;
    private Status status;
    private Node ownerNode;

    /// Creates a new [FilterPopupControl] instance.
    public FilterPopupControl() {
        getStyleClass().add("filter-popup");
        setConsumeAutoHidingEvents(true);
        setAutoHide(true);
    }

    /// Returns the [Comparator] property, which is used to sort the items inside this filter.
    ///
    /// @return the [Comparator] property
    public final ObjectProperty<Comparator<S>> comparatorProperty() {
        if (comparator == null) {
            comparator = new SimpleObjectProperty<>(this, "comparator");
        }
        return comparator;
    }

    /// Returns the [FilterStrategy] property, which matches the items against the filter text.
    /// Defaults to [FilterStrategy#DEFAULT].
    ///
    /// @return the [FilterStrategy] property
    public final ObjectProperty<FilterStrategy> filterStrategyProperty() {
        if (filterStrategy == null) {
            filterStrategy = new SimpleObjectProperty<>(this, "filterStrategy", FilterStrategy.DEFAULT);
        }
        return filterStrategy;
    }

    /// Returns the fixed cell size property.
    ///
    /// @return the fixed cell size property
    public DoubleProperty fixedCellSizeProperty() {
        if (fixedCellSize == null) {
            fixedCellSize = new SimpleDoubleProperty(this, "fixedCellSize", 24);
        }
        return fixedCellSize;
    }

    /// Returns the backing items, which are all items of the table, including the ones currently filtered out.
    ///
    /// @return the backing items
    public final ObservableList<S> getBackingItems() {
        return backingItems;
    }

    /// Returns the [Comparator] which is used to sort the items inside this filter.
    ///
    /// @return the item [Comparator]
    public final Comparator<S> getComparator() {
        return comparator == null ? null : comparator.get();
    }

    /// Returns the [FilterStrategy] which matches the items against the filter text.
    ///
    /// @return the [FilterStrategy]
    public final FilterStrategy getFilterStrategy() {
        return filterStrategy == null ? FilterStrategy.DEFAULT : filterStrategy.get();
    }

    /// Returns the fixed cell size.
    ///
    /// @return the fixed cell size
    public double getFixedCellSize() {
        return fixedCellSize == null ? 24 : fixedCellSizeProperty().get();
    }

    /// Returns the items, which are the items currently shown in the table.
    ///
    /// @return the items
    public final ObservableList<S> getItems() {
        return items;
    }

    /// Returns the backing items of all entries which were selected in the [FilterPopupControl], including the items
    /// which are currently not shown.
    ///
    /// @return the selected items
    public final ObservableList<S> getSelectedItems() {
        return selectedItems;
    }

    /// Returns the [Status].
    ///
    /// @return the [Status]
    public final Status getStatus() {
        return status;
    }

    @Override
    public Styleable getStyleableParent() {
        return ownerNode == null ? super.getStyleableParent() : ownerNode;
    }

    /// Refreshes the given item.
    ///
    /// @param item
    ///         the item
    public void refreshItem(S item) {
        getProperties().put(REFRESH_ITEM, item);
    }

    /// Sets the backing items.
    ///
    /// @param value
    ///         the backing items
    public final void setBackingItems(Collection<S> value) {
        backingItems.setAll(value);
    }

    /// Sets the [Comparator] which is used to sort the items inside this filter.
    ///
    /// @param comparator
    ///         the item [Comparator]
    public final void setComparator(Comparator<S> comparator) {
        comparatorProperty().set(comparator);
    }

    /// Sets the [FilterStrategy] which matches the items against the filter text.
    ///
    /// @param filterStrategy
    ///         the [FilterStrategy]
    public final void setFilterStrategy(FilterStrategy filterStrategy) {
        filterStrategyProperty().set(filterStrategy);
    }

    /// Sets the fixed cell size.
    ///
    /// @param fixedCellSize
    ///         the fixed cell size
    public void setFixedCellSize(double fixedCellSize) {
        fixedCellSizeProperty().set(fixedCellSize);
    }

    /// Sets the items.
    ///
    /// @param value
    ///         the items
    public final void setItems(Collection<S> value) {
        items.setAll(value);
    }

    /// Sets the read function which transforms an item into the text shown inside this filter.
    ///
    /// @param readFunction
    ///         the read function
    public final void setReadFunction(Callback<S, String> readFunction) {
        this.readFunction = readFunction;
    }

    /// Sets the selected items.
    ///
    /// @param selectedItems
    ///         the selected items
    public final void setSelectedItems(ObservableList<S> selectedItems) {
        this.selectedItems = selectedItems;
    }

    @Override
    public void show(Node ownerNode, double anchorX, double anchorY) {
        // This is a bit hacky:
        // We need the owner node as styleable parent, so we will use the same style(sheet).
        // But we want the window to be the owner of this popup as otherwise the closing behavior is bad.
        // Normally with setAutoHide(true), the popup will be dismissed when a click outside the popup is performed.
        // But when an owner node is set, it will not be dismissed when the click was inside the owner node itself.
        // See also #autoHideProperty()
        this.ownerNode = ownerNode;
        show(ownerNode.getScene().getWindow(), anchorX, anchorY);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new FilterPopupControlSkin<>(this);
    }

    /// Returns the read function.
    ///
    /// @return the read function
    protected final Callback<S, String> getReadFunction() {
        return readFunction;
    }

    /// Sets the [Status].
    ///
    /// @param status
    ///         the [Status]
    protected final void setStatus(Status status) {
        this.status = status;
    }

    /// Enum for the status this [FilterPopupControl] can have when closed.
    ///
    /// @author Marius Hanl
    public enum Status {
        /// The filter of the column should be applied.
        APPLY,
        /// The filter of the column should be reset.
        RESET_COLUMN,
        /// The filter of all columns of the table should be reset.
        RESET_ALL,
        /// Nothing changed, so the current filter should be kept.
        UNCHANGED
    }
}
