package tools.maran.extendedtable.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.layout.Region;

import tools.maran.extendedtable.filter.popup.FilterPopupControl;
import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.column.AbstractFilterTableColumn;
import tools.maran.extendedtable.tableview.cell.ComboBoxTableCell;
import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;
import tools.maran.extendedtable.tableview.column.GenericTableColumn;
import tools.maran.extendedtable.tableview.column.CheckBoxTableColumn;
import tools.maran.extendedtable.tableview.column.StringTableColumn;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.cell.ComboBoxTreeTableCell;
import tools.maran.extendedtable.treetableview.cell.ExtendedTreeTableCell;
import tools.maran.extendedtable.treetableview.column.GenericTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.AbstractFilterTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.CheckBoxTreeTableColumn;
import tools.maran.extendedtable.treetableview.column.StringTreeTableColumn;

/// The table types every table test runs against.
public enum TableType {
    TABLE_VIEW(".table-cell") {
        @Override
        public ItemTable<Object> createItemTable() {
            return new ItemTable<>(new ExtendedTableView<>(), Object::new);
        }

        @Override
        public FilterTableFixture createFixture() {
            return new TableViewFixture();
        }

        @Override
        public TableColumnBase<?, ?> createColumn(String text, String value, Supplier<Node> cellGraphicFactory) {
            TableColumn<Row, String> column = new TableColumn<>(text);
            column.setCellValueFactory(_ -> new SimpleStringProperty(value));
            column.setCellFactory(_ -> new ExtendedTableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(empty ? null : item);
                    setGraphic(empty ? null : cellGraphicFactory.get());
                }
            });
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createNestedColumn(String text, TableColumnBase<?, ?>... children) {
            TableColumn<Row, ?> column = new TableColumn<>(text);
            column.getColumns().addAll((List) List.of(children));
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createCheckBoxColumn() {
            CheckBoxTableColumn<Row> column = new CheckBoxTableColumn<>();
            column.setReadFunction(row -> Boolean.valueOf(row.first()));
            column.setWriteFunction((row, value) -> row.setFirst(value.toString()));
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createComboBoxColumn(List<String> values) {
            GenericTableColumn<Row, String> column = new GenericTableColumn<>() {
                @Override
                protected ExtendedTableCell<Row, String> createTableCell() {
                    ComboBoxTableCell<Row, String> cell = new ComboBoxTableCell<>(
                            FXCollections.observableArrayList(values));
                    cell.setReadFunction(value -> value);
                    return cell;
                }
            };
            column.setReadFunction(Row::second);
            column.setWriteFunction(Row::setSecond);
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createStringColumn() {
            StringTableColumn<Row> column = new StringTableColumn<>();
            column.setReadFunction(Row::first);
            column.setWriteFunction(Row::setFirst);
            return column;
        }

        @Override
        public Control createTable(List<TableColumnBase<?, ?>> columns, List<Row> rows) {
            ExtendedTableView<Row> table = new ExtendedTableView<>(FXCollections.observableArrayList(rows));
            columns.forEach(column -> table.getColumns().add((TableColumn) column));
            return table;
        }

        @Override
        public void setEditable(Control table, boolean editable) {
            ((ExtendedTableView<?>) table).setEditable(editable);
        }
    }, TREE_TABLE_VIEW(".tree-table-cell") {
        @Override
        public ItemTable<TreeItem<Object>> createItemTable() {
            return new ItemTable<>(new ExtendedTreeTableView<>(), () -> new TreeItem<>(new Object()));
        }

        @Override
        public FilterTableFixture createFixture() {
            return new TreeTableViewFixture();
        }

        @Override
        public TableColumnBase<?, ?> createColumn(String text, String value, Supplier<Node> cellGraphicFactory) {
            TreeTableColumn<Row, String> column = new TreeTableColumn<>(text);
            column.setCellValueFactory(_ -> new SimpleStringProperty(value));
            column.setCellFactory(_ -> new ExtendedTreeTableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    setText(empty ? null : item);
                    setGraphic(empty ? null : cellGraphicFactory.get());
                }
            });
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createNestedColumn(String text, TableColumnBase<?, ?>... children) {
            TreeTableColumn<Row, ?> column = new TreeTableColumn<>(text);
            column.getColumns().addAll((List) List.of(children));
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createCheckBoxColumn() {
            CheckBoxTreeTableColumn<Row> column = new CheckBoxTreeTableColumn<>();
            column.setReadFunction(row -> Boolean.valueOf(row.first()));
            column.setWriteFunction((row, value) -> row.setFirst(value.toString()));
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createComboBoxColumn(List<String> values) {
            GenericTreeTableColumn<Row, String> column = new GenericTreeTableColumn<>() {
                @Override
                protected ExtendedTreeTableCell<Row, String> createTreeTableCell() {
                    ComboBoxTreeTableCell<Row, String> cell = new ComboBoxTreeTableCell<>(
                            FXCollections.observableArrayList(values));
                    cell.setReadFunction(value -> value);
                    return cell;
                }
            };
            column.setReadFunction(Row::second);
            column.setWriteFunction(Row::setSecond);
            return column;
        }

        @Override
        public TableColumnBase<?, ?> createStringColumn() {
            StringTreeTableColumn<Row> column = new StringTreeTableColumn<>();
            column.setReadFunction(Row::first);
            column.setWriteFunction(Row::setFirst);
            return column;
        }

        @Override
        public Control createTable(List<TableColumnBase<?, ?>> columns, List<Row> rows) {
            ExtendedTreeTableView<Row> table = new ExtendedTreeTableView<>();
            table.setItems(FXCollections.observableArrayList(rows.stream().map(TreeItem::new).toList()));
            columns.forEach(column -> table.getColumns().add((TreeTableColumn) column));
            return table;
        }

        @Override
        public void setEditable(Control table, boolean editable) {
            ((ExtendedTreeTableView<?>) table).setEditable(editable);
        }
    };

    private final String cellSelector;

    TableType(String cellSelector) {
        this.cellSelector = cellSelector;
    }

    /// Creates a checkbox column, which reads and writes the first value of a [Row].
    public abstract TableColumnBase<?, ?> createCheckBoxColumn();

    /// Creates an editable combobox column with the given values, which reads and writes the second value of a [Row].
    public abstract TableColumnBase<?, ?> createComboBoxColumn(List<String> values);

    /// Creates an editable string column, which reads and writes the first value of a [Row].
    public abstract TableColumnBase<?, ?> createStringColumn();

    /// Creates a column with an [ExtendedTableCell] or [ExtendedTreeTableCell], which shows the given value and a
    /// graphic from the given factory in every non-empty cell.
    public abstract TableColumnBase<?, ?> createColumn(String text, String value, Supplier<Node> cellGraphicFactory);

    /// Creates a column with an [ExtendedTableCell] or [ExtendedTreeTableCell], which shows the given value.
    public TableColumnBase<?, ?> createColumn(String text, String value) {
        return createColumn(text, value, () -> null);
    }

    public abstract FilterTableFixture createFixture();

    public abstract ItemTable<?> createItemTable();

    /// Creates a column which contains the given child columns.
    public abstract TableColumnBase<?, ?> createNestedColumn(String text, TableColumnBase<?, ?>... children);

    /// Creates a table with the given columns and rows.
    public abstract Control createTable(List<TableColumnBase<?, ?>> columns, List<Row> rows);

    /// Creates a table with the given columns and two rows.
    public Control createTable(List<TableColumnBase<?, ?>> columns) {
        return createTable(columns, List.of(new Row("1", "1", "1"), new Row("2", "2", "2")));
    }

    /// Returns the CSS selector of the cells inside a table created by [#createTable(List)].
    public String getCellSelector() {
        return cellSelector;
    }

    public abstract void setEditable(Control table, boolean editable);

    public record ItemTable<T>(ExtendedTable<T> table, Supplier<T> newItemFactory) { }

    /// [FilterTableFixture] for the [ExtendedTableView].
    static final class TableViewFixture implements FilterTableFixture {

        private final ExtendedTableView<Row> tableView = new ExtendedTableView<>();

        TableViewFixture() {
            tableView.getColumns().addAll(List.of(createColumn("Column 1", Row::first, Row::setFirst),
                    createColumn("Column 2", Row::second, Row::setSecond),
                    createColumn("Column 3", Row::third, Row::setThird)));
        }

        @Override
        public void addBackingRow(int index, Row row) {
            tableView.getBackingItems().add(index, row);
        }

        @Override
        public void addVisibleRow(int index, Row row) {
            tableView.getItems().add(index, row);
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
        public boolean isSortedBy(int columnIndex) {
            return tableView.getSortOrder().contains(tableView.getColumns().get(columnIndex));
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
        public void removeVisibleRow(Row row) {
            tableView.getItems().removeIf(item -> item == row);
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
        public void setFilterComparator(int columnIndex, Comparator<Row> comparator) {
            ((AbstractFilterTableColumn<Row, ?>) tableView.getColumns().get(columnIndex)).setFilterPopupFactory(() -> {
                FilterPopupControl<Row> popupControl = new FilterPopupControl<>();
                popupControl.setComparator(comparator);
                return popupControl;
            });
        }

        @Override
        public void setRows(List<Row> rows) {
            tableView.setItems(FXCollections.observableArrayList(rows));
        }

        @Override
        public void sortBackingRows(Comparator<Row> comparator) {
            FXCollections.sort(tableView.getBackingItems(), comparator);
        }

        @Override
        public void sortVisibleRows(Comparator<Row> comparator) {
            tableView.getItems().sort(comparator);
        }

        @Override
        public void sortDescendingBy(int columnIndex) {
            TableColumn<Row, ?> column = tableView.getColumns().get(columnIndex);
            column.setSortable(true);
            column.setSortType(TableColumn.SortType.DESCENDING);
            tableView.getSortOrder().setAll(column);
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

        private final ExtendedTreeTableView<Row> treeTableView = new ExtendedTreeTableView<>();

        TreeTableViewFixture() {
            treeTableView.getColumns().addAll(List.of(createColumn("Column 1", Row::first, Row::setFirst),
                    createColumn("Column 2", Row::second, Row::setSecond),
                    createColumn("Column 3", Row::third, Row::setThird)));
        }

        @Override
        public void addBackingRow(int index, Row row) {
            treeTableView.getBackingItems().add(index, new TreeItem<>(row));
        }

        @Override
        public void addVisibleRow(int index, Row row) {
            treeTableView.getRoot().getChildren().add(index, new TreeItem<>(row));
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
            treeTableView.filter((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex),
                    toTreeItems(rows));
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
        public boolean isSortedBy(int columnIndex) {
            return treeTableView.getSortOrder().contains(treeTableView.getColumns().get(columnIndex));
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
        public void removeVisibleRow(Row row) {
            treeTableView.getRoot().getChildren().removeIf(item -> item.getValue() == row);
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
            treeTableView.getBackingItems().setAll(toTreeItems(rows));
        }

        @Override
        public void setColumnVisible(int columnIndex, boolean visible) {
            treeTableView.getColumns().get(columnIndex).setVisible(visible);
        }

        @Override
        public void setFilterComparator(int columnIndex, Comparator<Row> comparator) {
            ((AbstractFilterTreeTableColumn<Row, ?>) treeTableView.getColumns().get(columnIndex)).setFilterPopupFactory(
                    () -> {
                        FilterPopupControl<TreeItem<Row>> popupControl = new FilterPopupControl<>();
                        popupControl.setComparator(Comparator.comparing(TreeItem::getValue, comparator));
                        return popupControl;
                    });
        }

        @Override
        public void setRows(List<Row> rows) {
            treeTableView.setItems(FXCollections.observableArrayList(rows.stream().map(TreeItem::new).toList()));
        }

        @Override
        public void sortBackingRows(Comparator<Row> comparator) {
            FXCollections.sort(treeTableView.getBackingItems(), Comparator.comparing(TreeItem::getValue, comparator));
        }

        @Override
        public void sortVisibleRows(Comparator<Row> comparator) {
            treeTableView.getRoot().getChildren().sort(Comparator.comparing(TreeItem::getValue, comparator));
        }

        @Override
        public void sortDescendingBy(int columnIndex) {
            TreeTableColumn<Row, ?> column = treeTableView.getColumns().get(columnIndex);
            column.setSortable(true);
            column.setSortType(TreeTableColumn.SortType.DESCENDING);
            treeTableView.getSortOrder().setAll(column);
        }

        private StringTreeTableColumn<Row> createColumn(String text, Function<Row, String> readFunction,
                BiConsumer<Row, String> writeFunction) {
            StringTreeTableColumn<Row> column = new StringTreeTableColumn<>(text);
            column.setReadFunction(readFunction::apply);
            column.setWriteFunction(writeFunction);
            return column;
        }

        private List<TreeItem<Row>> toTreeItems(List<Row> rows) {
            Map<Row, TreeItem<Row>> treeItems = new IdentityHashMap<>();
            treeTableView.getBackingItems().forEach(item -> treeItems.put(item.getValue(), item));

            return rows.stream().map(row -> treeItems.computeIfAbsent(row, TreeItem::new)).toList();
        }
    }
}
