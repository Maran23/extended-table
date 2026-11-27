package tools.maran.extendedtable.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Tests the [FilteredOutItems].
///
/// @author Marius Hanl
class FilteredOutItemsTest {

    private final Object itemA = new Object();
    private final Object itemB = new Object();
    private final Object itemC = new Object();
    private final List<Object> backingItems = new ArrayList<>();

    private long seed;
    private Random random;

    @BeforeEach
    void createRandom() {
        seed = new Random().nextLong();
        random = new Random(seed);
    }

    @DisplayName("A filtered out item removed by a change stays filtered out when the next change adds it again")
    @Test
    void testRemovedItemAddedByTheNextChangeStaysFilteredOut() {
        FilteredOutItems<Object> filteredOutItems = filterOutTheSecondItem();

        remove(filteredOutItems, 1);

        assertTrue(add(filteredOutItems, 0, itemB));
        assertTrue(filteredOutItems.all(backingItems.size()).get(0));
    }

    @DisplayName("A filtered out item removed by a change is shown when added again after a filter changed")
    @Test
    void testRemovedItemIsShownWhenAddedAgainAfterAFilterChanged() {
        FilteredOutItems<Object> filteredOutItems = filterOutTheSecondItem();

        remove(filteredOutItems, 1);
        filteredOutItems.put(new KeyColumn<>(), new BitSet());

        assertFalse(add(filteredOutItems, 0, itemB));
        assertTrue(filteredOutItems.all(backingItems.size()).isEmpty());
    }

    @DisplayName("A filtered out item removed by a change is shown when added again after another change")
    @Test
    void testRemovedItemIsShownWhenAddedAgainAfterAnotherChange() {
        FilteredOutItems<Object> filteredOutItems = filterOutTheSecondItem();

        remove(filteredOutItems, 1);
        add(filteredOutItems, 0, new Object());

        assertFalse(add(filteredOutItems, 0, itemB));
        assertTrue(filteredOutItems.all(backingItems.size()).isEmpty());
    }

    @DisplayName("Replacing bits shifts the tail like a list would, also across word boundaries")
    @Test
    void testReplaceShiftsTheTailLikeAList() {
        for (int run = 0; run < 2_000; run++) {
            BitSet bits = new BitSet();
            int size = random.nextInt(200);
            for (int i = 0; i < size; i++) {
                bits.set(i, random.nextBoolean());
            }
            int from = random.nextInt(size + 1);
            int removedSize = random.nextInt(size - from + 1);
            int addedSize = random.nextInt(130);

            BitSet expected = new BitSet();
            for (int i = 0; i < size + addedSize - removedSize; i++) {
                // The bits before stay, the added ones are clear and the tail is shifted.
                expected.set(i, i < from ? bits.get(i) : i >= from + addedSize && bits.get(i - addedSize + removedSize));
            }

            FilteredOutItems.replaceBits(bits, from, removedSize, addedSize);

            assertEquals(expected, bits,
                    "seed: " + seed + ", from: " + from + ", removed: " + removedSize + ", added: " + addedSize);
        }
    }

    private boolean add(FilteredOutItems<Object> filteredOutItems, int index, Object item) {
        backingItems.add(index, item);
        filteredOutItems.replace(index, 0, 1);
        return filteredOutItems.restoreMoved(Set.of(item), backingItems);
    }

    private FilteredOutItems<Object> filterOutTheSecondItem() {
        backingItems.addAll(List.of(itemA, itemB, itemC));
        BitSet bits = new BitSet();
        bits.set(1);
        FilteredOutItems<Object> filteredOutItems = new FilteredOutItems<>();
        filteredOutItems.put(new KeyColumn<>(), bits);
        return filteredOutItems;
    }

    private void remove(FilteredOutItems<Object> filteredOutItems, int index) {
        filteredOutItems.rememberRemoved(List.of(backingItems.remove(index)), index);
        filteredOutItems.replace(index, 1, 0);
        filteredOutItems.restoreMoved(Set.of(), backingItems);
    }
}
