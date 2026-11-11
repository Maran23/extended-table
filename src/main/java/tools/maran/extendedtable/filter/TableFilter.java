package tools.maran.extendedtable.filter;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;

import tools.maran.extendedtable.table.common.ExtendedTable;

/// Holds the backing items of a table and shows the ones which are not filtered out by any filtered column.
///
/// The filtered out items of a column are stored as bits at the index of the backing items, so combining the filters
/// of many columns stays fast and small even for a huge amount of items. A change of the backing items is applied to
/// the items of the table, no matter if the table is filtered or not.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public final class TableFilter<S> {

    private final Map<FilterableColumn<S>, BitSet> filteredOutItems;

    private final ExtendedTable<S> table;
    private final Consumer<List<S>> itemsSetter;
    private final Runnable onItemsChanged;

    private ObservableList<S> backingItems;
    private ReadOnlyBooleanWrapper filtered;

    private boolean isInternalItemsChange;
    private boolean isInternalBackingItemsChange;

    /// Creates a new [TableFilter] instance.
    ///
    /// @param table
    ///         the table whose items are filtered
    /// @param itemsSetter
    ///         shows the given items in the table
    /// @param onItemsChanged
    ///         called after the backing items or the items shown in the table changed
    public TableFilter(ExtendedTable<S> table, Consumer<List<S>> itemsSetter, Runnable onItemsChanged) {
        this.table = table;
        this.itemsSetter = itemsSetter;
        this.onItemsChanged = onItemsChanged;
        filteredOutItems = new IdentityHashMap<>(48);

        table.getVisibleLeafColumns().addListener((InvalidationListener) _ -> resetRemovedColumns());
    }

    /// Filters by the given column, so that the column only lets the given items through.
    ///
    /// @param column
    ///         the column which filters
    /// @param filteredItems
    ///         the backing items the column lets through
    /// @throws IllegalArgumentException
    ///         when a filtered item is not inside the backing items
    public void filter(FilterableColumn<S> column, Collection<S> filteredItems) {
        List<S> backingItems = getBackingItems();
        Set<S> columnFilteredItems = newIdentitySet(filteredItems.size());
        columnFilteredItems.addAll(filteredItems);

        BitSet columnFilteredOutItems = new BitSet(backingItems.size());
        for (int i = 0; i < backingItems.size(); i++) {
            if (!columnFilteredItems.contains(backingItems.get(i))) {
                columnFilteredOutItems.set(i);
            }
        }

        // Less, as the same item may be contained multiple times inside the backing items.
        if (backingItems.size() - columnFilteredOutItems.cardinality() < columnFilteredItems.size()) {
            throw new IllegalArgumentException("The filtered items must be inside the backing items");
        }

        filteredOutItems.put(column, columnFilteredOutItems);
        column.setFiltered(true);
        applyFilters();
    }

    /// Returns the filtered property, which tells whether the table is filtered or not.
    ///
    /// @return the filtered property
    public ReadOnlyBooleanProperty filteredProperty() {
        return filteredImpl().getReadOnlyProperty();
    }

    /// Returns the backing items.
    ///
    /// @return the backing items
    public ObservableList<S> getBackingItems() {
        if (backingItems == null) {
            List<S> items = table.getItems();
            backingItems = FXCollections.observableArrayList(items == null ? List.of() : items);
            backingItems.addListener(this::backingItemsChanged);
        }
        return backingItems;
    }

    /// Returns true, when the table is filtered, false otherwise.
    ///
    /// @return true, when the table is filtered, false otherwise
    public boolean isFiltered() {
        return filtered != null && filtered.get();
    }

    /// Applies the change of the items of the table to the backing items. While the table is filtered, removed items
    /// are removed from the backing items, while added items are inserted after the item shown before them, or at the
    /// start. Sorted items keep their order, see [#sortBackingItems()].
    ///
    /// @param change
    ///         the change of the items of the table
    public void itemsChanged(Change<? extends S> change) {
        if (isInternalItemsChange) {
            return;
        }

        if (!isFiltered()) {
            setBackingItems(table.getItems());
            return;
        }

        // The backing index of every item of the table before the change. Positions are used instead of the items, as
        // the same item may be contained multiple times.
        int[] backingIndices = shownBackingIndices();
        BitSet removedItems = new BitSet();
        NavigableMap<Integer, List<S>> addedItems = new TreeMap<>();
        boolean sorted = false;
        // The indices of a change are the ones after the previous changes, so they are shifted by their size delta.
        int delta = 0;
        while (change.next()) {
            if (change.wasPermutated()) {
                permutate(backingIndices, change);
                sorted = true;
                continue;
            }

            int from = change.getFrom() - delta;
            for (int i = 0; i < change.getRemovedSize(); i++) {
                removedItems.set(backingIndices[from + i]);
            }
            if (change.wasAdded()) {
                // Inserted after the item shown before them, or at the start.
                int index = from == 0 ? 0 : backingIndices[from - 1] + 1;
                addedItems.computeIfAbsent(index, _ -> new ArrayList<>()).addAll(change.getAddedSubList());
            }
            delta += change.getAddedSize() - change.getRemovedSize();
        }

        if (!removedItems.isEmpty() || !addedItems.isEmpty()) {
            applyToBackingItems(removedItems, addedItems);
        }
        if (sorted) {
            sortBackingItems();
        }
    }

    /// Takes the new items of the table as backing items, which resets the filter of every column.
    ///
    /// @param items
    ///         the new items of the table, may be null
    public void itemsReplaced(List<S> items) {
        if (isInternalItemsChange) {
            return;
        }

        setBackingItems(items == null ? List.of() : items);
        resetFilters();
    }

    /// Resets the filter of the given column, so only the filters of the remaining filtered columns are applied.
    ///
    /// @param column
    ///         the column whose filter is reset
    public void resetFilter(FilterableColumn<S> column) {
        if (filteredOutItems.remove(column) == null) {
            return;
        }

        column.setFiltered(false);
        applyFilters();
    }

    /// Shows all backing items again and resets the filter of every column.
    public void restoreBackingItems() {
        if (isFiltered()) {
            showItems(getBackingItems());
        }

        resetFilters();
    }

    /// Resets the filter of every column and shows the given items, which become the new backing items.
    ///
    /// @param items
    ///         the new items of the table
    public void setItems(List<S> items) {
        // The items may be the backing items or the items of the table, which would be cleared when set to themselves.
        List<S> newItems = new ArrayList<>(items);
        resetFilters();

        isInternalItemsChange = true;
        itemsSetter.accept(newItems);
        isInternalItemsChange = false;

        setBackingItems(newItems);
    }

    /// Adds the items to the given set.
    ///
    /// @return the given set, or a new one sized for the items when it was null
    private static <T> Set<T> addItems(Set<T> set, List<? extends T> items) {
        if (set == null) {
            set = newIdentitySet(items.size());
        }
        set.addAll(items);
        return set;
    }

    /// Adds the removed items every column filters out, which needs to happen before their bits are replaced.
    ///
    /// @return the given map, or a new one when it was null and there are removed filtered out items
    private Map<BitSet, Set<S>> addRemovedFilteredOutItems(Map<BitSet, Set<S>> removedFilteredOutItems,
            List<? extends S> removed, int from) {
        for (BitSet bits : filteredOutItems.values()) {
            BitSet removedBits = bits.get(from, from + removed.size());
            if (removedBits.isEmpty()) {
                continue;
            }

            if (removedFilteredOutItems == null) {
                removedFilteredOutItems = new IdentityHashMap<>(filteredOutItems.size());
            }
            Set<S> items = removedFilteredOutItems.computeIfAbsent(bits,
                    _ -> newIdentitySet(removedBits.cardinality()));
            for (int i = removedBits.nextSetBit(0); i >= 0; i = removedBits.nextSetBit(i + 1)) {
                items.add(removed.get(i));
            }
        }
        return removedFilteredOutItems;
    }

    /// Applies the change of the source to the target, which contained the same items as the source before.
    private static <T> void applyChange(Change<? extends T> change, List<T> source, ObservableList<T> target) {
        while (change.next()) {
            if (change.wasPermutated()) {
                sortByOrderOf(target, source);
                continue;
            }
            if (change.wasRemoved()) {
                target.subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
            }
            if (change.wasAdded()) {
                target.addAll(change.getFrom(), change.getAddedSubList());
            }
        }
    }

    /// Removes the removed items from the backing items and inserts the added items at their index, while the filtered
    /// out items are moved along.
    private void applyToBackingItems(BitSet removedItems, NavigableMap<Integer, List<S>> addedItems) {
        List<S> backingItems = getBackingItems();
        List<S> items = new ArrayList<>(backingItems.size());
        int[] newIndices = new int[backingItems.size()];
        Iterator<Entry<Integer, List<S>>> added = addedItems.entrySet().iterator();
        Entry<Integer, List<S>> nextAdded = added.hasNext() ? added.next() : null;
        for (int i = 0; i <= backingItems.size(); i++) {
            if (nextAdded != null && nextAdded.getKey() == i) {
                items.addAll(nextAdded.getValue());
                nextAdded = added.hasNext() ? added.next() : null;
            }
            if (i < backingItems.size() && !removedItems.get(i)) {
                newIndices[i] = items.size();
                items.add(backingItems.get(i));
            }
        }

        // Removed items are shown, so the filtered out items are never removed.
        moveFilteredOutItems(newIndices, items.size());
        setBackingItems(items);
    }

    /// Shows all backing items which are not filtered out by any filtered column.
    private void applyFilters() {
        BitSet allFilteredOutItems = allFilteredOutItems();
        if (allFilteredOutItems.isEmpty()) {
            restoreBackingItems();
            return;
        }

        showItems(shownBackingItems(allFilteredOutItems));
        filteredImpl().set(true);
    }

    /// Applies the change of the backing items to the items of the table and moves the filtered out items along with
    /// it. Added (or replacing) items are unknown and therefore shown and not filtered out, while items which are
    /// removed and added again within the change are just moved and keep their visibility.
    private void backingItemsChanged(Change<? extends S> change) {
        if (isInternalBackingItemsChange) {
            return;
        }

        ObservableList<S> items = table.getItems();
        if (items == null) {
            return;
        }

        isInternalItemsChange = true;
        if (filteredOutItems.isEmpty()) {
            applyChange(change, getBackingItems(), items);
        } else {
            handleFilteredChange(change, items);
        }
        isInternalItemsChange = false;

        onItemsChanged.run();
    }

    /// Moves the backing indices of the items of the table along with their permutation.
    private static void permutate(int[] backingIndices, Change<?> change) {
        int[] permutated = backingIndices.clone();
        for (int i = change.getFrom(); i < change.getTo(); i++) {
            permutated[change.getPermutation(i)] = backingIndices[i];
        }
        System.arraycopy(permutated, 0, backingIndices, 0, backingIndices.length);
    }

    /// Returns the backing index of every item of the table, which are the backing items not filtered out.
    private int[] shownBackingIndices() {
        BitSet allFilteredOutItems = allFilteredOutItems();
        int size = getBackingItems().size();
        return IntStream.range(0, size).filter(i -> !allFilteredOutItems.get(i)).toArray();
    }

    /// Returns the backing items which are not filtered out.
    private List<S> shownBackingItems(BitSet allFilteredOutItems) {
        List<S> backingItems = getBackingItems();
        List<S> items = new ArrayList<>(backingItems.size() - allFilteredOutItems.cardinality());
        for (int i = allFilteredOutItems.nextClearBit(0); i < backingItems.size();
             i = allFilteredOutItems.nextClearBit(i + 1)) {
            items.add(backingItems.get(i));
        }
        return items;
    }

    private BitSet allFilteredOutItems() {
        BitSet allFilteredOutItems = new BitSet(getBackingItems().size());
        filteredOutItems.values().forEach(allFilteredOutItems::or);
        return allFilteredOutItems;
    }

    private ReadOnlyBooleanWrapper filteredImpl() {
        if (filtered == null) {
            filtered = new ReadOnlyBooleanWrapper(table, "filtered");
        }
        return filtered;
    }

    /// Applies the change by position, as the same item may be contained multiple times. Only items which are removed
    /// and added within the change are matched by identity, so they keep their visibility.
    private void handleFilteredChange(Change<? extends S> change, ObservableList<S> items) {
        BitSet allFilteredOutItems = allFilteredOutItems();
        Set<S> addedItems = null;
        Map<BitSet, Set<S>> removedFilteredOutItems = null;
        boolean permutated = false;
        while (change.next()) {
            int from = change.getFrom();
            if (change.wasPermutated()) {
                permutateBitSet(change);
                permutated = true;
                continue;
            }

            List<? extends S> removed = change.getRemoved();
            int shownFrom = from - allFilteredOutItems.get(0, from).cardinality();
            int shownRemovedSize = removed.size() - allFilteredOutItems.get(from, from + removed.size()).cardinality();
            items.subList(shownFrom, shownFrom + shownRemovedSize).clear();
            items.addAll(shownFrom, change.getAddedSubList());

            if (change.wasRemoved()) {
                removedFilteredOutItems = addRemovedFilteredOutItems(removedFilteredOutItems, removed, from);
            }
            if (change.wasAdded()) {
                addedItems = addItems(addedItems, change.getAddedSubList());
            }

            replace(allFilteredOutItems, from, removed.size(), change.getAddedSize());
            for (BitSet bits : filteredOutItems.values()) {
                replace(bits, from, removed.size(), change.getAddedSize());
            }
        }

        if (removedFilteredOutItems != null && addedItems != null) {
            restoreMovedFilteredOutItems(removedFilteredOutItems, addedItems, getBackingItems());
            hideFilteredOutItems(items, allFilteredOutItems);
        }

        if (permutated) {
            sortByOrderOf(items, shownBackingItems(allFilteredOutItems()));
        }
    }

    /// Removes the shown items which are filtered out now, but were not before.
    private void hideFilteredOutItems(ObservableList<S> items, BitSet previousFilteredOutItems) {
        BitSet hiddenItems = allFilteredOutItems();
        hiddenItems.andNot(previousFilteredOutItems);
        // From the last one, so the shown index of the ones before stays the same.
        for (int i = hiddenItems.length() - 1; i >= 0; i = hiddenItems.previousSetBit(i - 1)) {
            items.remove(i - previousFilteredOutItems.get(0, i).cardinality());
        }
    }

    /// Returns the index of every item, which is the first one for an item contained multiple times.
    private static <T> Map<T, Integer> indexMap(List<T> items) {
        Map<T, Integer> indexMap = new IdentityHashMap<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            indexMap.putIfAbsent(items.get(i), i);
        }
        return indexMap;
    }

    private boolean isInTable(TableColumnBase<?, ?> column) {
        while (column.getParentColumn() != null) {
            column = column.getParentColumn();
        }
        return table.getColumns().contains(column);
    }

    /// Moves the filtered out items of every column to their new index.
    private void moveFilteredOutItems(int[] newIndices, int size) {
        for (Entry<FilterableColumn<S>, BitSet> entry : filteredOutItems.entrySet()) {
            BitSet bits = entry.getValue();
            BitSet newBits = new BitSet(size);
            for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
                newBits.set(newIndices[i]);
            }
            entry.setValue(newBits);
        }
    }

    private static <T> Set<T> newIdentitySet(int expectedSize) {
        return Collections.newSetFromMap(new IdentityHashMap<>(expectedSize));
    }

    private void permutateBitSet(Change<? extends S> change) {
        int from = change.getFrom();
        int to = change.getTo();
        for (BitSet bits : filteredOutItems.values()) {
            BitSet permutatedBits = bits.get(from, to);
            bits.clear(from, to);
            for (int i = permutatedBits.nextSetBit(0); i >= 0; i = permutatedBits.nextSetBit(i + 1)) {
                bits.set(change.getPermutation(from + i));
            }
        }
    }

    /// Replaces the given amount of bits at the given index with the given amount of clear bits, shifting all bits
    /// after them accordingly.
    ///
    /// The tail after the replaced bits is moved to its new index a whole word (64 bits) at a time: each word is split
    /// at the bit offset, its lower part is shifted into the target word and its upper part into the next one.
    private static void replace(BitSet bits, int from, int removedSize, int addedSize) {
        int length = bits.length();
        if (removedSize == addedSize || from + removedSize >= length) {
            bits.clear(from, from + removedSize);
            return;
        }

        BitSet tail = bits.get(from + removedSize, length);
        bits.clear(from, length);

        int offset = from + addedSize;
        int wordOffset = offset / Long.SIZE;
        int bitOffset = offset % Long.SIZE;
        long[] words = tail.toLongArray();
        long[] shiftedWords = new long[wordOffset + words.length + 1];
        for (int i = 0; i < words.length; i++) {
            shiftedWords[wordOffset + i] |= words[i] << bitOffset;
            if (bitOffset != 0) {
                shiftedWords[wordOffset + i + 1] |= words[i] >>> (Long.SIZE - bitOffset);
            }
        }
        bits.or(BitSet.valueOf(shiftedWords));
    }

    private void resetFilters() {
        filteredOutItems.keySet().forEach(column -> column.setFiltered(false));
        filteredOutItems.clear();

        filteredImpl().set(false);
    }

    /// Resets the filter of every filtered column which was removed from the table.
    private void resetRemovedColumns() {
        boolean reset = false;
        for (Iterator<FilterableColumn<S>> iterator = filteredOutItems.keySet().iterator(); iterator.hasNext(); ) {
            FilterableColumn<S> column = iterator.next();
            if (!isInTable((TableColumnBase<?, ?>) column)) {
                iterator.remove();
                column.setFiltered(false);
                reset = true;
            }
        }

        if (reset) {
            applyFilters();
        }
    }

    /// Filters out the removed filtered out items again, which were added back and therefore just moved.
    private static <T> void restoreMovedFilteredOutItems(Map<BitSet, Set<T>> removedFilteredOutItems, Set<T> addedItems,
            List<T> backingItems) {
        for (Entry<BitSet, Set<T>> entry : removedFilteredOutItems.entrySet()) {
            BitSet bits = entry.getKey();
            Set<T> items = entry.getValue();
            items.retainAll(addedItems);
            if (items.isEmpty()) {
                continue;
            }

            for (int i = 0; i < backingItems.size(); i++) {
                if (items.contains(backingItems.get(i))) {
                    bits.set(i);
                }
            }
        }
    }

    private void setBackingItems(List<S> items) {
        isInternalBackingItemsChange = true;
        getBackingItems().setAll(items);
        isInternalBackingItemsChange = false;

        onItemsChanged.run();
    }

    /// Takes over the order of the sorted items of the table. The backing items are sorted by the comparator of the
    /// table first, so the filtered out items are sorted as well. The shown items then take the indices of the shown
    /// items in their order, as the table may sort them differently, e.g. with a custom sort policy or without any
    /// comparator.
    private void sortBackingItems() {
        List<S> backingItems = getBackingItems();
        List<S> items = new ArrayList<>(backingItems);
        Comparator<S> comparator = table.getComparator();
        if (comparator != null) {
            // The indices are sorted instead of the items, as the same item may be contained multiple times.
            List<Integer> order = new ArrayList<>(IntStream.range(0, items.size()).boxed().toList());
            order.sort(Comparator.comparing(backingItems::get, comparator));

            int[] newIndices = new int[items.size()];
            for (int i = 0; i < order.size(); i++) {
                items.set(i, backingItems.get(order.get(i)));
                newIndices[order.get(i)] = i;
            }
            moveFilteredOutItems(newIndices, items.size());
        }

        BitSet allFilteredOutItems = allFilteredOutItems();
        List<S> shownItems = table.getItems();
        int shownIndex = 0;
        for (int i = allFilteredOutItems.nextClearBit(0); i < items.size();
             i = allFilteredOutItems.nextClearBit(i + 1)) {
            items.set(i, shownItems.get(shownIndex++));
        }

        setBackingItems(items);
    }

    private void showItems(List<S> items) {
        isInternalItemsChange = true;
        itemsSetter.accept(items);
        isInternalItemsChange = false;

        onItemsChanged.run();
    }

    /// Sorts the items into the order of the given items, which contain the same items. The same item at multiple
    /// positions can not be sorted apart, so the items are replaced instead when sorting did not restore the order.
    private static <T> void sortByOrderOf(ObservableList<T> items, List<T> order) {
        if (isSameOrder(items, order)) {
            return;
        }

        items.sort(Comparator.comparingInt(indexMap(order)::get));
        if (!isSameOrder(items, order)) {
            items.setAll(order);
        }
    }

    private static <T> boolean isSameOrder(List<T> items, List<T> order) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != order.get(i)) {
                return false;
            }
        }
        return true;
    }
}
