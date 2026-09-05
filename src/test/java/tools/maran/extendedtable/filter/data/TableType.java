package tools.maran.extendedtable.filter.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.layout.Region;

import tools.maran.extendedtable.filter.FilterPopupControl;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.column.AbstractFilterTableColumn;
import tools.maran.extendedtable.tableview.column.StringTableColumn;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.column.AbstractFilterTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.StringTreeTableColumn;

/// The tables which support filtering.
public enum TableType {
    TABLE_VIEW {
        @Override
        public ItemTable<Object> createItemTable() {
            return new ItemTable<>(new ExtendedTableView<>(), Object::new);
        }

        @Override
        public FilterTableFixture createFixture() {
            return new TableViewFixture(new ExtendedTableView<>());
        }
    }, TREE_TABLE_VIEW {
        @Override
        public ItemTable<TreeItem<Object>> createItemTable() {
            return new ItemTable<>(new ExtendedTreeTableView<>(), () -> new TreeItem<>(new Object()));
        }

        @Override
        public FilterTableFixture createFixture() {
            return new TreeTableViewFixture(new ExtendedTreeTableView<>());
        }
    };

    public abstract FilterTableFixture createFixture();

    public abstract ItemTable<?> createItemTable();

    public record ItemTable<T>(ExtendedTable<T> table, Supplier<T> newItemFactory) { }

    /// [FilterTableFixture] for the [ExtendedTableView].
    static final class TableViewFixture implements FilterTableFixture {

        private final ExtendedTableView<Row> tableView;

        TableViewFixture(ExtendedTableView<Row> table) {
            tableView = table;
            tableView.getColumns().addAll(List.of(createColumn("Column 1", Row::first, Row::setFirst),
                    createColumn("Column 2", Row::second, Row::setSecond),
                    createColumn("Column 3", Row::third, Row::setThird)));
        }

        @Override
        public void addBackingRow(int index, Row row) {
            tableView.getBackingItems().add(index, row);
        }

        @Override
        public void commitValue(int rowIndex, int columnIndex, String value) {
            TableColumn<Row, String> column = (TableColumn<Row, String>) tableView.getColumns().get(columnIndex);
            TablePosition<Row, String> position = new TablePosition<>(tableView, rowIndex, column);

            Event.fireEvent(column,
                    new TableColumn.CellEditEvent<>(tableView, position, TableColumn.editCommitEvent(), value));
        }

        @Override
        public void filter(int columnIndex, List<Row> rows) {
            tableView.filter((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex), rows);
        }

        @Override
        public List<Row> getBackingRows() {
            return List.copyOf(tableView.getBackingItems());
        }

        @Override
        public FilterPopupControl<?> getFilterPopup(int columnIndex) {
            return ((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex)).getFilterPopup();
        }

        @Override
        public Region getTable() {
            return tableView;
        }

        @Override
        public List<Row> getVisibleRows() {
            return List.copyOf(tableView.getItems());
        }

        @Override
        public boolean isColumnFiltered(int columnIndex) {
            return ((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex)).isFiltered();
        }

        @Override
        public boolean isFiltered() {
            return tableView.isFiltered();
        }

        @Override
        public void moveColumn(int fromIndex, int toIndex) {
            List<TableColumn<Row, ?>> columns = new ArrayList<>(tableView.getColumns());
            columns.add(toIndex, columns.remove(fromIndex));
            tableView.getColumns().setAll(columns);
        }

        @Override
        public void refreshFilter(int columnIndex) {
            ((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex)).refreshFilter();
        }

        @Override
        public void removeBackingRow(Row row) {
            tableView.getBackingItems().removeIf(item -> item == row);
        }

        @Override
        public void removeColumn(int columnIndex) {
            tableView.getColumns().remove(columnIndex);
        }

        @Override
        public void replaceBackingRow(Row oldRow, Row newRow) {
            ObservableList<Row> backingItems = tableView.getBackingItems();
            for (int i = 0; i < backingItems.size(); i++) {
                if (backingItems.get(i) == oldRow) {
                    backingItems.set(i, newRow);
                    return;
                }
            }
        }

        @Override
        public void setBackingRows(List<Row> rows) {
            tableView.getBackingItems().setAll(rows);
        }

        @Override
        public void setColumnVisible(int columnIndex, boolean visible) {
            tableView.getColumns().get(columnIndex).setVisible(visible);
        }

        @Override
        public void setItemComparator(int columnIndex, Comparator<Row> comparator) {
            ((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex)).setItemComparator(comparator);
        }

        @Override
        public void setRows(List<Row> rows) {
            tableView.setItems(FXCollections.observableArrayList(rows));
        }

        @Override
        public void sortBackingRows(Comparator<Row> comparator) {
            FXCollections.sort(tableView.getBackingItems(), comparator);
        }

        private StringTableColumn<Row> createColumn(String text, Function<Row, String> readFunction,
                BiConsumer<Row, String> writeFunction) {
            StringTableColumn<Row> column = new StringTableColumn<>(text);
            column.setReadFunction(readFunction::apply);
            column.setWriteFunction(writeFunction);
            return column;
        }
    }

    /// [FilterTableFixture] for the [ExtendedTreeTableView], with the rows as flat children of the root.
    static final class TreeTableViewFixture implements FilterTableFixture {

        private final ExtendedTreeTableView<Row> treeTableView;

        TreeTableViewFixture(ExtendedTreeTableView<Row> table) {
            treeTableView = table;
            treeTableView.getColumns().addAll(List.of(createColumn("Column 1", Row::first, Row::setFirst),
                    createColumn("Column 2", Row::second, Row::setSecond),
                    createColumn("Column 3", Row::third, Row::setThird)));
        }

        @Override
        public void addBackingRow(int index, Row row) {
            treeTableView.getBackingItems().add(index, new TreeItem<>(row));
        }

        @Override
        public void commitValue(int rowIndex, int columnIndex, String value) {
            TreeTableColumn<Row, String> column = (TreeTableColumn<Row, String>) treeTableView.getColumns()
                    .get(columnIndex);
            TreeTablePosition<Row, String> position = new TreeTablePosition<>(treeTableView, rowIndex, column);

            Event.fireEvent(column,
                    new TreeTableColumn.CellEditEvent<>(treeTableView, position, TreeTableColumn.editCommitEvent(),
                            value));
        }

        @Override
        public void filter(int columnIndex, List<Row> rows) {
            // Rows which are not a backing row get a new tree item, which is not inside the backing items as well.
            Map<Row, TreeItem<Row>> treeItems = new IdentityHashMap<>();
            treeTableView.getBackingItems().forEach(item -> treeItems.put(item.getValue(), item));

            treeTableView.filter((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex),
                    rows.stream().map(row -> treeItems.computeIfAbsent(row, TreeItem::new)).toList());
        }

        @Override
        public List<Row> getBackingRows() {
            return treeTableView.getBackingItems().stream().map(TreeItem::getValue).toList();
        }

        @Override
        public FilterPopupControl<?> getFilterPopup(int columnIndex) {
            return ((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns()
                    .get(columnIndex)).getFilterPopup();
        }

        @Override
        public Region getTable() {
            return treeTableView;
        }

        @Override
        public List<Row> getVisibleRows() {
            return treeTableView.getItems().stream().map(TreeItem::getValue).toList();
        }

        @Override
        public boolean isColumnFiltered(int columnIndex) {
            return ((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex)).isFiltered();
        }

        @Override
        public boolean isFiltered() {
            return treeTableView.isFiltered();
        }

        @Override
        public void moveColumn(int fromIndex, int toIndex) {
            List<TreeTableColumn<Row, ?>> columns = new ArrayList<>(treeTableView.getColumns());
            columns.add(toIndex, columns.remove(fromIndex));
            treeTableView.getColumns().setAll(columns);
        }

        @Override
        public void refreshFilter(int columnIndex) {
            ((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex)).refreshFilter();
        }

        @Override
        public void removeBackingRow(Row row) {
            treeTableView.getBackingItems().removeIf(item -> item.getValue() == row);
        }

        @Override
        public void removeColumn(int columnIndex) {
            treeTableView.getColumns().remove(columnIndex);
        }

        @Override
        public void replaceBackingRow(Row oldRow, Row newRow) {
            ObservableList<TreeItem<Row>> backingItems = treeTableView.getBackingItems();
            for (int i = 0; i < backingItems.size(); i++) {
                if (backingItems.get(i).getValue() == oldRow) {
                    backingItems.set(i, new TreeItem<>(newRow));
                    return;
                }
            }
        }

        @Override
        public void setBackingRows(List<Row> rows) {
            // Rows which are already a backing row keep their tree item, just like they keep their instance.
            Map<Row, TreeItem<Row>> treeItems = new IdentityHashMap<>();
            treeTableView.getBackingItems().forEach(item -> treeItems.put(item.getValue(), item));

            treeTableView.getBackingItems()
                    .setAll(rows.stream().map(row -> treeItems.computeIfAbsent(row, TreeItem::new)).toList());
        }

        @Override
        public void setColumnVisible(int columnIndex, boolean visible) {
            treeTableView.getColumns().get(columnIndex).setVisible(visible);
        }

        @Override
        public void setItemComparator(int columnIndex, Comparator<Row> comparator) {
            ((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex)).setItemComparator(
                    comparator);
        }

        @Override
        public void setRows(List<Row> rows) {
            treeTableView.setItems(FXCollections.observableArrayList(rows.stream().map(TreeItem::new).toList()));
        }

        @Override
        public void sortBackingRows(Comparator<Row> comparator) {
            FXCollections.sort(treeTableView.getBackingItems(), Comparator.comparing(TreeItem::getValue, comparator));
        }

        private StringTreeTableColumn<Row> createColumn(String text, Function<Row, String> readFunction,
                BiConsumer<Row, String> writeFunction) {
            StringTreeTableColumn<Row> column = new StringTreeTableColumn<>(text);
            column.setReadFunction(readFunction::apply);
            column.setWriteFunction(writeFunction);
            return column;
        }
    }
}
