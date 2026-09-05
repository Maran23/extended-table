package tools.maran.extendedtable.tableview.row;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.ToDoubleFunction;

import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.control.Skin;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.skin.TableRowSkin;

import tools.maran.extendedtable.table.common.ExtendedTableSkin;
import tools.maran.extendedtable.tableview.ExtendedTableView;
import tools.maran.extendedtable.tableview.ExtendedTableViewSkin;
import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;

/// Extended [TableRowSkin] for the [ExtendedTableRow] to support fixed cells.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class ExtendedTableRowSkin<S> extends TableRowSkin<S> {

    private static final String FIXED_TABLE_CELL = "fixed-table-cell";

    private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

    private WeakHashMap<TableColumn<S, ?>, WeakReference<TableCell<S, ?>>> cellMap;

    /// Creates a new [ExtendedTableRowSkin] instance.
    ///
    /// @param extendedTableRow
    ///         the [ExtendedTableRow]
    public ExtendedTableRowSkin(ExtendedTableRow<S> extendedTableRow) {
        super(extendedTableRow);

        TableView<S> tableView = getSkinnable().getTableView();
        if (tableView == null) {
            return;
        }

        ObservableMap<Object, Object> properties = tableView.getProperties();
        properties.addListener(propertiesMapListener);

        Skin<?> skin = tableView.getSkin();
        if (skin instanceof ExtendedTableViewSkin<?> extendedTableViewSkin) {
            // Request layout so the fixed cells will be removed/added.
            registerChangeListener(extendedTableViewSkin.fixedColumnCountProperty(),
                    _ -> getSkinnable().requestLayout());
            // Request layout so the cell size is recalculated.
            registerChangeListener(extendedTableViewSkin.cellSizeProviderProperty(),
                    _ -> getSkinnable().requestLayout());
        }
    }

    @Override
    public void dispose() {
        if (getSkinnable() == null) {
            return;
        }
        TableView<S> tableView = getSkinnable().getTableView();

        if (tableView != null) {
            tableView.getProperties().removeListener(propertiesMapListener);
        }

        super.dispose();
    }

    @Override
    protected double computeMaxHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return calculateCellSize().orElse(super.computeMaxHeight(width, topInset, rightInset, bottomInset, leftInset));
    }

    @Override
    protected double computeMinHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return calculateCellSize().orElse(super.computeMinHeight(width, topInset, rightInset, bottomInset, leftInset));
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset, double bottomInset,
            double leftInset) {
        return calculateCellSize().orElse(super.computePrefHeight(width, topInset, rightInset, bottomInset, leftInset));
    }

    // Raw type, as declared by JavaFX.
    @Override
    @SuppressWarnings("rawtypes")
    protected TableCell<S, ?> createCell(TableColumnBase tcb) {
        TableCell<S, ?> cell = super.createCell(tcb);
        if (cellMap == null) {
            cellMap = new WeakHashMap<>();
        }
        cellMap.put(cell.getTableColumn(), new WeakReference<>(cell));
        return cell;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        TableView<S> tableView = getSkinnable().getTableView();
        if (tableView == null) {
            return;
        }

        Skin<?> skin = tableView.getSkin();
        if (skin instanceof ExtendedTableViewSkin<?> extendedTableViewSkin) {
            int fixedColumnCount = extendedTableViewSkin.getFixedColumnCount();
            if (fixedColumnCount > 0) {
                double hScrollValue = extendedTableViewSkin.getHBarValue();
                layoutFixedCells(x, y, fixedColumnCount, hScrollValue);
            }
        }
    }

    private Optional<Double> calculateCellSize() {
        TableView<S> tableView = getSkinnable().getTableView();
        if (!(tableView instanceof ExtendedTableView)) {
            return Optional.empty();
        }
        // Let the super method calculate.
        if (tableView.getFixedCellSize() > 0) {
            return Optional.empty();
        }
        // We can't provide an item, therefore we can't call the cell size function.
        S item = getSkinnable().getItem();
        if (item == null) {
            return Optional.empty();
        }

        ToDoubleFunction<S> cellSizeProvider = ((ExtendedTableView<S>) tableView).getCellSizeProvider();
        if (cellSizeProvider == null) {
            return Optional.empty();
        }

        double cellSize = cellSizeProvider.applyAsDouble(item);
        if (cellSize > 0) {
            return Optional.of(cellSize);
        }
        return Optional.empty();
    }

    private TableColumnBase<S, ?> getTopMostColumn(TableColumnBase<S, ?> column) {
        TableColumnBase<S, ?> topMostColumn = column;
        while (topMostColumn.getParentColumn() != null) {
            topMostColumn = topMostColumn.getParentColumn();
        }

        return topMostColumn;
    }

    private void layoutFixedCells(double x, double y, int fixedColumnCount, double hScrollValue) {
        TableView<S> tableView = getSkinnable().getTableView();
        double fixedCellSize = tableView.getFixedCellSize();

        double fixedColumnWidth = 0;
        int fixedColumnCounter = fixedColumnCount;
        double newX = snapPositionX(x + Math.abs(hScrollValue - x));

        TableColumnBase<S, ?> previousTopMostColumn = null;
        List<? extends TableColumn<S, ?>> columns = getVisibleLeafColumns();

        for (TableColumn<S, ?> column : columns) {
            if (!column.isVisible()) {
                continue;
            }

            Reference<TableCell<S, ?>> tableCellReference = cellMap.get(column);
            if (tableCellReference == null) {
                cellMap.remove(column);
                continue;
            }

            TableCell<S, ?> tableCell = tableCellReference.get();
            if (tableCell == null) {
                cellMap.remove(column);
                continue;
            }

            TableColumnBase<S, ?> currentTopMostColumn = getTopMostColumn(column);
            boolean differentTopMostColumn = currentTopMostColumn != previousTopMostColumn;
            if (fixedColumnCounter <= 0 && differentTopMostColumn) {
                tableCell.getStyleClass().remove(FIXED_TABLE_CELL);
                continue;
            }

            // When this is the cell of the same top most column we don't decrement the counter as they belong
            // together.
            if (differentTopMostColumn) {
                fixedColumnCounter--;
            }

            if (!tableCell.getStyleClass().contains(FIXED_TABLE_CELL)) {
                tableCell.getStyleClass().add(FIXED_TABLE_CELL);
            }

            previousTopMostColumn = currentTopMostColumn;

            double widthCol = snapSizeX(column.getWidth());

            if (hScrollValue + fixedColumnWidth > x) {
                fixedColumnWidth = snapSpaceX(fixedColumnWidth + widthCol);

                // Re-add table cell when removed by the layoutChildren(..) method in super class.
                if (!getChildren().contains(tableCell)) {
                    getChildren().add(tableCell);
                } else {
                    tableCell.toFront();
                }

                double height;
                if (fixedCellSize > 0) {
                    height = snapSizeY(fixedCellSize);
                } else {
                    // Same as in JavaFX api.
                    height = Math.max(snapSpaceY(getSkinnable().getHeight()), snapSizeY(tableCell.prefHeight(-1)));
                }

                tableCell.resize(widthCol, height);
                tableCell.relocate(newX, y);
                tableCell.requestLayout();

                newX = snapPositionX(newX + widthCol);
            }
        }
    }

    private void onPropertiesAdded(Change<?, ?> change) {
        if (!change.wasAdded() || cellMap == null) {
            return;
        }

        TableRow<S> tableRow = getSkinnable();
        TableView<S> tableView = tableRow.getTableView();

        int index = tableRow.getIndex();
        ObservableList<S> items = tableView.getItems();
        if (items == null) {
            return;
        }
        // Empty or unused rows will be ignored.
        if (index < 0 || index >= items.size()) {
            return;
        }

        if (ExtendedTableSkin.REFRESH_COLUMN.equals(change.getKey())) {
            TableColumn<S, ?> column = (TableColumn<S, ?>) change.getValueAdded();

            WeakReference<TableCell<S, ?>> cellRef = cellMap.get(column);
            if (cellRef == null) {
                return;
            }

            TableCell<S, ?> tableCell = cellRef.get();
            if (tableCell == null) {
                return;
            }

            if (tableCell instanceof ExtendedTableCell<S, ?> extendedTableCell) {
                extendedTableCell.refresh();
            }
        }
        if (ExtendedTableSkin.REFRESH.equals(change.getKey())) {
            // Update row first, then the cells.
            if (tableRow instanceof ExtendedTableRow<?> extendedTableRow) {
                extendedTableRow.refresh();
            }

            for (WeakReference<TableCell<S, ?>> cellRef : cellMap.values()) {
                if (cellRef == null) {
                    continue;
                }

                TableCell<S, ?> tableCell = cellRef.get();
                if (tableCell == null) {
                    continue;
                }

                if (tableCell instanceof ExtendedTableCell<S, ?> extendedTableCell) {
                    extendedTableCell.refresh();
                }
            }
        }
    }

}
