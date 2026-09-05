package tools.maran.extendedtable.filter.random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.JavaFxTest;
import tools.maran.extendedtable.filter.data.TableType;
import tools.maran.extendedtable.filter.KeyColumn;
import tools.maran.extendedtable.filter.TableFilter;
import tools.maran.extendedtable.filter.data.TableType.ItemTable;
import tools.maran.extendedtable.table.common.ExtendedTable;

/// Tests [TableFilter] of every table with random changes of many items, so the bits span over multiple words.
///
/// @author Marius Hanl
class TableFilterRandomTest extends JavaFxTest {

    private long seed;
    private Random random;

    @BeforeEach
    void createRandom() {
        seed = new Random().nextLong();
        random = new Random(seed);
    }

    @DisplayName("Random changes of the backing items keep the same items filtered out")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void randomChangesOfTheBackingItemsKeepTheFilteredOutItems(TableType tableType) {
        runOnFxThread(() -> {
            for (int run = 0; run < 20; run++) {
                randomChangesOfTheBackingItems(tableType.createItemTable(), run);
            }
        });
    }

    /// Filters the table and asserts after every random change of its backing items that the same items are
    /// filtered out.
    private <T> void randomChangesOfTheBackingItems(ItemTable<T> itemTable, int run) {
        ExtendedTable<T> table = itemTable.table();
        Supplier<T> newItem = itemTable.newItemFactory();
        table.setItems(FXCollections.observableArrayList(newItems(newItem, 300)));
        ObservableList<T> backingItems = table.getBackingItems();

        List<T> filteredItems = backingItems.stream().filter(_ -> random.nextInt(3) == 0).toList();
        table.filter(new KeyColumn(), filteredItems);

        Set<T> filteredOutItems = Collections.newSetFromMap(new IdentityHashMap<>());
        backingItems.stream().filter(item -> !filteredItems.contains(item)).forEach(filteredOutItems::add);

        List<String> actions = new ArrayList<>();
        for (int step = 0; step < 30; step++) {
            assertDoesNotThrow(() -> changeRandomly(backingItems, newItem, actions),
                    () -> "seed " + seed + ", run " + run + ", actions " + actions);

            List<T> expectedItems = backingItems.stream()
                    .filter(item -> !filteredOutItems.contains(item))
                    .toList();
            String message = "seed " + seed + ", run " + run + ", actions " + actions;
            assertEquals(expectedItems, table.getItems(), message);
        }
    }

    /// Applies a random change to the backing items and records it inside the actions.
    private <T> void changeRandomly(ObservableList<T> backingItems, Supplier<T> newItem, List<String> actions) {
        int index = random.nextInt(backingItems.size() + 1);
        int count = 1 + random.nextInt(70);

        switch (random.nextInt(5)) {
            case 0 -> {
                actions.add("add(" + index + ", " + count + ")");
                backingItems.addAll(index, newItems(newItem, count));
            }
            case 1 -> {
                int to = Math.min(backingItems.size(), index + count);
                actions.add("remove(" + index + ", " + to + ")");
                backingItems.remove(index, to);
            }
            case 2 -> {
                if (!backingItems.isEmpty()) {
                    int setIndex = random.nextInt(backingItems.size());
                    actions.add("set(" + setIndex + ")");
                    backingItems.set(setIndex, newItem.get());
                }
            }
            case 3 -> {
                // Moves the items and replaces some of them with new ones.
                List<T> items = new ArrayList<>(backingItems.subList(0, backingItems.size() / 2));
                items.addAll(newItems(newItem, count));
                Collections.shuffle(items, random);
                actions.add("setAll(" + items.size() + ")");
                backingItems.setAll(items);
            }
            default -> {
                // Sorts by random keys, which permutates the items.
                Map<T, Integer> keys = new IdentityHashMap<>();
                backingItems.forEach(item -> keys.put(item, random.nextInt()));
                actions.add("sort()");
                FXCollections.sort(backingItems, Comparator.comparing(keys::get));
            }
        }
    }

    private static <T> List<T> newItems(Supplier<T> newItem, int count) {
        return Stream.generate(newItem).limit(count).toList();
    }
}
