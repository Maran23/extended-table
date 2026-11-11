package tools.maran.extendedtable.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.collections.FXCollections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.JavaFxTest;
import tools.maran.extendedtable.data.TableType;
import tools.maran.extendedtable.data.TableType.ItemTable;
import tools.maran.extendedtable.table.common.ExtendedTable;

/// Tests [TableFilter] directly on every table.
///
/// @author Marius Hanl
class TableFilterTest extends JavaFxTest {

    @DisplayName("The backing items are created from the current items when first needed")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testBackingItemsAreCreatedFromTheCurrentItems(TableType tableType) {
        runOnFxThread(() -> backingItemsAreCreatedFromTheCurrentItems(tableType.createItemTable()));
    }

    private static <T> void backingItemsAreCreatedFromTheCurrentItems(ItemTable<T> itemTable) {
        ExtendedTable<T> table = itemTable.table();
        Supplier<T> newItem = itemTable.newItemFactory();
        table.setItems(FXCollections.observableArrayList(newItems(newItem, 10)));
        table.getItems().remove(0, 3);
        table.getItems().addAll(newItems(newItem, 5));

        List<T> filteredItems = List.copyOf(table.getItems().subList(0, 4));
        table.filter(new KeyColumn<>(), filteredItems);

        assertEquals(12, table.getBackingItems().size());
        assertEquals(filteredItems, table.getItems());
    }

    @DisplayName("Setting the backing items or the items of the table to the table keeps them")
    @ParameterizedTest
    @EnumSource(TableType.class)
    void testSettingTheBackingItemsOrTheItemsKeepsThem(TableType tableType) {
        runOnFxThread(() -> settingTheBackingItemsOrTheItemsKeepsThem(tableType.createItemTable()));
    }

    private static <T> void settingTheBackingItemsOrTheItemsKeepsThem(ItemTable<T> itemTable) {
        ExtendedTable<T> table = itemTable.table();
        List<T> items = newItems(itemTable.newItemFactory(), 3);
        table.setItems(FXCollections.observableArrayList(items));
        table.filter(new KeyColumn<>(), items.subList(0, 1));

        table.setItems(table.getBackingItems());
        assertEquals(items, table.getItems());
        assertEquals(items, table.getBackingItems());

        table.setItems(table.getItems());
        assertEquals(items, table.getItems());
        assertEquals(items, table.getBackingItems());
    }

    private static <T> List<T> newItems(Supplier<T> newItem, int count) {
        return Stream.generate(newItem).limit(count).toList();
    }
}
