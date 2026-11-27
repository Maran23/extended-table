package tools.maran.extendedtable.filter;

import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javafx.collections.ListChangeListener.Change;

/// The filtered out items of every filtered column, stored as bits at the index of the backing items, so combining the
/// filters of many columns stays fast and small even for a huge amount of items. The bits are moved along with every
/// change of the backing items.
///
/// Items which are removed and added again by a change of the backing items, or by the change right after it, are just
/// moved and keep their visibility, as `Bindings.bindContent(..)` moves sorted items with a removal and an addition.
/// The removed items are forgotten whenever the filters or the bits are replaced.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
final class FilteredOutItems<S> {

    private final Map<FilterableColumn<S>, BitSet> columnBits = new IdentityHashMap<>();

    // The filtered out items removed by the current change and the ones removed by the last change, per column bits.
    private Map<BitSet, Set<S>> removedItems = new IdentityHashMap<>();
    private Map<BitSet, Set<S>> lastRemovedItems = Map.of();

    /// Returns the items filtered out by any column.
    ///
    /// @param size
    ///         the amount of backing items
    /// @return the items filtered out by any column, as bits at the index of the backing items
    BitSet all(int size) {
        BitSet allBits = new BitSet(size);
        columnBits.values().forEach(allBits::or);
        return allBits;
    }

    /// Removes the filtered out items of every column.
    void clear() {
        columnBits.clear();
        forgetRemoved();
    }

    /// Returns the columns which filter out items.
    ///
    /// @return the unmodifiable columns
    Set<FilterableColumn<S>> columns() {
        return Collections.unmodifiableSet(columnBits.keySet());
    }

    /// Forgets the removed items, so they are shown when added again.
    void forgetRemoved() {
        removedItems = new IdentityHashMap<>();
        lastRemovedItems = Map.of();
    }

    /// Returns true, when no column filters out items, false otherwise.
    ///
    /// @return true, when no column filters out items, false otherwise
    boolean isEmpty() {
        return columnBits.isEmpty();
    }

    /// Moves the filtered out items to their new index.
    ///
    /// @param newIndices
    ///         the new index of every backing item
    /// @param size
    ///         the amount of backing items afterwards
    void move(int[] newIndices, int size) {
        columnBits.replaceAll((_, bits) -> move(bits, newIndices, size));
        forgetRemoved();
    }

    /// Moves the filtered out items along with the permutation of the given change.
    ///
    /// @param change
    ///         the change of the backing items, which must be a permutation
    void permutate(Change<?> change) {
        int from = change.getFrom();
        int to = change.getTo();
        for (BitSet bits : columnBits.values()) {
            BitSet permutatedBits = bits.get(from, to);
            bits.clear(from, to);
            for (int i = permutatedBits.nextSetBit(0); i >= 0; i = permutatedBits.nextSetBit(i + 1)) {
                bits.set(change.getPermutation(from + i));
            }
        }
    }

    /// Sets the filtered out items of the given column.
    ///
    /// @param column
    ///         the column
    /// @param bits
    ///         the filtered out items, as bits at the index of the backing items
    void put(FilterableColumn<S> column, BitSet bits) {
        columnBits.put(column, bits);
        forgetRemoved();
    }

    /// Remembers the removed items which are filtered out, which needs to happen before their bits are replaced.
    ///
    /// @param removed
    ///         the removed backing items
    /// @param from
    ///         the index of the first removed item
    void rememberRemoved(List<? extends S> removed, int from) {
        for (BitSet bits : columnBits.values()) {
            BitSet removedBits = bits.get(from, from + removed.size());
            if (removedBits.isEmpty()) {
                continue;
            }

            Set<S> items = removedItems.computeIfAbsent(bits,
                    _ -> Collections.newSetFromMap(new IdentityHashMap<>(removedBits.cardinality())));
            for (int i = removedBits.nextSetBit(0); i >= 0; i = removedBits.nextSetBit(i + 1)) {
                items.add(removed.get(i));
            }
        }
    }

    /// Removes the filtered out items of the given column.
    ///
    /// @param column
    ///         the column
    /// @return true, when the column filtered out items, false otherwise
    boolean remove(FilterableColumn<S> column) {
        forgetRemoved();
        return columnBits.remove(column) != null;
    }

    /// Replaces the given amount of backing items at the given index with the given amount of shown items.
    ///
    /// @param from
    ///         the index of the first replaced item
    /// @param removedSize
    ///         the amount of replaced items
    /// @param addedSize
    ///         the amount of shown items replacing them
    void replace(int from, int removedSize, int addedSize) {
        columnBits.values().forEach(bits -> replaceBits(bits, from, removedSize, addedSize));
    }

    /// Replaces the given amount of bits at the given index with the given amount of clear bits, shifting all bits
    /// after them accordingly.
    ///
    /// The tail after the replaced bits is moved to its new index a whole word (64 bits) at a time: each word is split
    /// at the bit offset, its lower part is shifted into the target word and its upper part into the next one.
    ///
    /// @param bits
    ///         the bits
    /// @param from
    ///         the index of the first replaced bit
    /// @param removedSize
    ///         the amount of replaced bits
    /// @param addedSize
    ///         the amount of clear bits replacing them
    static void replaceBits(BitSet bits, int from, int removedSize, int addedSize) {
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

    /// Filters out the added items again which were filtered out when removed by this or the last change, as they were
    /// just moved. Ends the change, so the items removed by it are remembered for the next one.
    ///
    /// @param addedItems
    ///         the added backing items
    /// @param backingItems
    ///         the backing items after the change
    /// @return true, when an added item is filtered out again, false otherwise
    boolean restoreMoved(Set<S> addedItems, List<S> backingItems) {
        Map<BitSet, Set<S>> removed = removedItems;
        Map<BitSet, Set<S>> lastRemoved = lastRemovedItems;
        removedItems = new IdentityHashMap<>();
        lastRemovedItems = addedItems.isEmpty() ? removed : Map.of();
        if (addedItems.isEmpty()) {
            return false;
        }

        boolean isRestored = restore(removed, addedItems, backingItems);
        isRestored |= restore(lastRemoved, addedItems, backingItems);
        return isRestored;
    }

    private static BitSet move(BitSet bits, int[] newIndices, int size) {
        BitSet movedBits = new BitSet(size);
        for (int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
            movedBits.set(newIndices[i]);
        }
        return movedBits;
    }

    /// Sets the bits of the removed items which were added again.
    private static <T> boolean restore(Map<BitSet, Set<T>> removedItems, Set<T> addedItems, List<T> backingItems) {
        boolean isRestored = false;
        for (Entry<BitSet, Set<T>> entry : removedItems.entrySet()) {
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
            isRestored = true;
        }
        return isRestored;
    }
}
