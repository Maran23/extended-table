package tools.maran.extendedtable.table.common;

import java.util.Collection;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.TableColumnBase;

import tools.maran.extendedtable.filter.FilterableColumn;

/// Interface that shares common functionality for the extended table implementation(s).
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public interface ExtendedTable<S> {

    /// Pre Commit event.
    EventType<?> PRE_COMMIT_EVENT = new EventType<>(CommitEvent.ANY, "TABLE_PRE_COMMIT");

    /// Commit event.
    EventType<?> COMMIT_EVENT = new EventType<>(CommitEvent.ANY, "TABLE_COMMIT");

    /// Autosizes all columns. That is, all columns will be set to the best-fitting size,
    /// measured by probing the column header and some cells.
    void autosizeColumns();

    /// Returns the [EventType] of the commit event.
    ///
    /// @param <S>
    ///         the item type
    /// @return the [CommitEvent] [EventType]
    static <S> EventType<CommitEvent<S>> commitEvent() {
        return (EventType<CommitEvent<S>>) COMMIT_EVENT;
    }

    /// Returns the editing property.
    ///
    /// @return the editing property
    BooleanProperty editingProperty();

    /// Filters this table by the given column, so that column only lets the given items through.
    /// The items shown are all backing items which are not filtered out by any filtered column.
    ///
    /// Items which are added to (or replace items of) the backing items afterwards are unknown to the filter and
    /// therefore not filtered out by it. The filter is dropped when new items are set via [#setItems(ObservableList)].
    ///
    /// @param column
    ///         the column which filters
    /// @param filteredItems
    ///         the backing items the column lets through
    /// @throws IllegalArgumentException
    ///         when a filtered item is not inside the backing items
    void filter(FilterableColumn<S> column, Collection<S> filteredItems);

    /// Returns the backing items [ObservableList]. The last items set via [#setItems(ObservableList)] are
    /// considered the 'backing' items.
    ///
    /// **Note:** The result of this method can differ from [#getItems()].
    ///
    /// @return the backing items [ObservableList]
    ObservableList<S> getBackingItems();

    /// The [TableColumnBase]s are part of this table. As the user reorders the columns, this list will be updated
    /// to reflect the current visual ordering.
    ///
    /// @return the [TableColumnBase]s
    ObservableList<? extends TableColumnBase<S, ?>> getColumns();

    /// Returns the fixed cell size of this table.
    ///
    /// @return the fixed cell size, or a value of 0 or smaller when no fixed cell size is used
    double getFixedCellSize();

    /// Returns a list of all buttons that should be shown in the table header.
    ///
    /// @return a list containing all buttons currently in the table header, and allowing for further buttons to be
    /// added or removed
    ObservableList<Node> getHeaderButtons();

    /// Returns the text that is currently set in the header.
    ///
    /// @return the text that is currently set in the header
    String getHeaderText();

    /// Returns the current underlying data model.
    ///
    /// @return the current underlying data model
    ObservableList<S> getItems();

    /// Returns the visible leaf columns of this table.
    ///
    /// @return the visible leaf columns
    ObservableList<? extends TableColumnBase<S, ?>> getVisibleLeafColumns();

    /// Returns the header text property.
    ///
    /// @return the header text property
    StringProperty headerTextProperty();

    /// Returns true, when the table is considered to be in editing mode.
    ///
    /// @return true, when the table is considered to be in editing mode
    boolean isEditing();

    /// Returns true, when this table is considered filtered, false otherwise.
    ///
    /// @return true, when this table is considered filtered, false otherwise
    boolean isFiltered();

    /// Returns true, when the header is shown. False otherwise.
    ///
    /// @return true, when the header is shown, false otherwise.
    boolean isShowHeader();

    /// Returns the items property.
    ///
    /// @return items property
    ObjectProperty<ObservableList<S>> itemsProperty();

    /// Returns the [EventType] of the pre commit event.
    ///
    /// @param <S>
    ///         the item type
    /// @return the [CommitEvent] [EventType]
    static <S> EventType<CommitEvent<S>> preCommitEvent() {
        return (EventType<CommitEvent<S>>) PRE_COMMIT_EVENT;
    }

    /// Forces the TableView to update what it is showing to the user.
    /// This is useful in cases where the underlying data source has changed
    /// in a way that is not observed by the Table itself.
    void refresh();

    /// Refreshes the filter of all filterable columns.
    void refreshColumnFilters();

    /// Resets the filter of the given column, so only the filters of the remaining filtered columns are applied.
    ///
    /// @param column
    ///         the column whose filter is reset
    void resetFilter(FilterableColumn<S> column);

    /// Restores the underlying data model (backing items) which was last set via [#setItems(ObservableList)].
    void restoreBackingItems();

    /// Sets, whether the table is currently considered to be in editing mode.
    ///
    /// @param isEditing
    ///         true, when the table is currently considered to be in editing mode
    void setEditing(boolean isEditing);

    /// Sets the header text.
    ///
    /// @param headerText
    ///         the text should be set in the header
    void setHeaderText(String headerText);

    /// Sets the underlying data model (backing items).
    ///
    /// @param backingItems
    ///         the underlying data model (backing items)
    void setItems(ObservableList<S> backingItems);

    /// Sets an event handler to the table which is fired right after the commit is performed.
    ///
    /// @param eventHandler
    ///         the [EventHandler]
    void setOnCommit(EventHandler<CommitEvent<S>> eventHandler);

    /// Sets an event handler to the table which is fired right before the commit is performed.
    ///
    /// @param eventHandler
    ///         the [EventHandler]
    void setOnPreCommit(EventHandler<CommitEvent<S>> eventHandler);

    /// Sets, whether the header should be shown or not.
    ///
    /// @param showHeader
    ///         true, when the header should be shown, false otherwise
    void setShowHeader(boolean showHeader);

    /// Returns the show header property.
    ///
    /// @return the show header property
    BooleanProperty showHeaderProperty();
}
