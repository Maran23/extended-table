package tools.maran.extendedtable.treetableview.row;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.ToDoubleFunction;

import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TableRowSkin;
import javafx.scene.control.skin.TreeTableRowSkin;

import tools.maran.extendedtable.table.common.ExtendedTableSkin;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableView;
import tools.maran.extendedtable.treetableview.ExtendedTreeTableViewSkin;
import tools.maran.extendedtable.treetableview.cell.ExtendedTreeTableCell;
import tools.maran.extendedtable.treetableview.line.TreeLineDrawable;
import tools.maran.extendedtable.treetableview.line.TreeLineDrawer;

/// Extended [TableRowSkin] for the [ExtendedTreeTableRow] to support fixed cells.
/// Also supports drawing tree lines via the [TreeLineDrawer].
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class ExtendedTreeTableRowSkin<S> extends TreeTableRowSkin<S> implements TreeLineDrawable {

    private static final String FIXED_TREE_TABLE_CELL = "fixed-tree-table-cell";

    private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

    private WeakHashMap<TreeTableColumn<S, ?>, WeakReference<TreeTableCell<S, ?>>> cellMap;
    private TreeLineDrawer treeLineDrawer;

    /// Creates a new [ExtendedTreeTableRowSkin] instance.
    ///
    /// @param extendedTableRow
    ///         the [ExtendedTreeTableRow]
    public ExtendedTreeTableRowSkin(ExtendedTreeTableRow<S> extendedTableRow) {
        super(extendedTableRow);

        TreeTableView<S> treeTableView = getSkinnable().getTreeTableView();
        if (treeTableView == null) {
            return;
        }

        ObservableMap<Object, Object> properties = treeTableView.getProperties();
        properties.addListener(propertiesMapListener);

        if (treeTableView instanceof ExtendedTreeTableView) {
            // Request layout so the tree lines are removed/added.
            registerChangeListener(((ExtendedTreeTableView<S>) treeTableView).showTreeLinesProperty(),
                    _ -> onShowTreeLinesChanged());
        }

        Skin<?> skin = treeTableView.getSkin();
        if (skin instanceof ExtendedTreeTableViewSkin<?> extendedTableViewSkin) {
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

        TreeTableView<S> treeTableView = getSkinnable().getTreeTableView();
        if (treeTableView != null) {
            treeTableView.getProperties().removeListener(propertiesMapListener);
        }

        super.dispose();
    }

    @Override
    public double getCellHeight() {
        // Returns the height of the first cell since it is only relevant to us.
        for (Node child : getChildren()) {
            if (child instanceof TreeTableCell) {
                return ((TreeTableCell<?, ?>) child).getHeight();
            }
        }

        return 0;
    }

    @Override
    public double getCellWidth() {
        // Returns the width of the first cell since it is only relevant to us.
        for (Node child : getChildren()) {
            if (child instanceof TreeTableCell) {
                return ((TreeTableCell<?, ?>) child).getWidth();
            }
        }

        return 0;
    }

    @Override
    public Node getDisclosureNode() {
        TreeTableRow<S> treeTableRow = getSkinnable();
        return treeTableRow.getDisclosureNode();
    }

    @Override
    public TreeItem<?> getTreeItem() {
        return getSkinnable().getTreeItem();
    }

    @Override
    public int getTreeItemLevel(TreeItem<?> treeItem) {
        return getSkinnable().getTreeTableView().getTreeItemLevel(treeItem);
    }

    @Override
    public boolean isShowRoot() {
        return getSkinnable().getTreeTableView().isShowRoot();
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
    protected TreeTableCell<S, ?> createCell(TableColumnBase tcb) {
        TreeTableCell<S, ?> cell = super.createCell(tcb);
        if (cellMap == null) {
            cellMap = new WeakHashMap<>();
        }
        cellMap.put(cell.getTableColumn(), new WeakReference<>(cell));
        return cell;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        super.layoutChildren(x, y, w, h);

        TreeTableView<S> treeTableView = getSkinnable().getTreeTableView();
        if (treeTableView == null) {
            return;
        }

        if (treeTableView instanceof ExtendedTreeTableView<S> extendedTreeTableView) {
            boolean showTreeLines = extendedTreeTableView.isShowTreeLines();

            if (showTreeLines && treeLineDrawer == null) {
                treeLineDrawer = new TreeLineDrawer(this);
            }
        }

        Skin<?> skin = treeTableView.getSkin();
        if (skin instanceof ExtendedTreeTableViewSkin<?> extendedTableViewSkin) {
            int fixedColumnCount = extendedTableViewSkin.getFixedColumnCount();
            if (fixedColumnCount > 0) {
                double hScrollValue = extendedTableViewSkin.getHBarValue();
                layoutFixedCells(x, y, fixedColumnCount, hScrollValue);
            }
        }

        Node disclosureNode = getDisclosureNode();
        if (disclosureNode != null && disclosureNode.isVisible()) {
            double disclosureWidth = snapSizeX(disclosureNode.prefWidth(h));
            double disclosureHeight = snapSizeY(disclosureNode.prefHeight(disclosureWidth));

            double minY = disclosureNode.getLayoutBounds().getMinY();
            disclosureNode.setLayoutY(snapPositionY(y + (h / 2.0 - disclosureHeight / 2.0)) - minY);
            disclosureNode.resize(disclosureWidth, disclosureHeight);
        }

        if (treeTableView instanceof ExtendedTreeTableView<S> extendedTreeTableView) {
            boolean showTreeLines = extendedTreeTableView.isShowTreeLines();

            if (showTreeLines) {
                // This node may get lost due to column reordering, therefore we add it again here.
                if (!getChildren().contains(treeLineDrawer)) {
                    getChildren().add(treeLineDrawer);
                } else {
                    // When horizontal scrolling this node might not be the top most node, therefore we reorder it here.
                    treeLineDrawer.toFront();
                }
                treeLineDrawer.clear();
                treeLineDrawer.drawTreeLines();
            }
        }
    }

    private Optional<Double> calculateCellSize() {
        TreeTableView<S> treeTableView = getSkinnable().getTreeTableView();
        if (!(treeTableView instanceof ExtendedTreeTableView)) {
            return Optional.empty();
        }
        // Let the super method calculate.
        if (treeTableView.getFixedCellSize() > 0) {
            return Optional.empty();
        }
        // We can't provide an item, therefore we can't call the cell size function.
        S item = getSkinnable().getItem();
        if (item == null) {
            return Optional.empty();
        }

        ToDoubleFunction<S> cellSizeProvider = ((ExtendedTreeTableView<S>) treeTableView).getCellSizeProvider();
        if (cellSizeProvider == null) {
            return Optional.empty();
        }

        double cellSize = cellSizeProvider.applyAsDouble(item);
        if (cellSize > 0) {
            return Optional.of(cellSize);
        }
        return Optional.empty();
    }

    private TableColumnBase<TreeItem<S>, ?> getTopMostColumn(TreeTableColumn<S, ?> column) {
        TableColumnBase<TreeItem<S>, ?> topMostColumn = column;
        while (topMostColumn.getParentColumn() != null) {
            topMostColumn = topMostColumn.getParentColumn();
        }

        return topMostColumn;
    }

    private void layoutFixedCells(double x, double y, int fixedColumnCount, double hScrollValue) {
        TreeTableView<S> treeTableView = getSkinnable().getTreeTableView();
        double fixedCellSize = treeTableView.getFixedCellSize();

        double fixedColumnWidth = 0;
        int counter = fixedColumnCount;
        double newX = snapPositionX(x + Math.abs(hScrollValue - x));
        double startX = newX;

        TableColumnBase<TreeItem<S>, ?> previousTopMostColumn = null;
        List<? extends TreeTableColumn<S, ?>> columns = getVisibleLeafColumns();
        for (TreeTableColumn<S, ?> column : columns) {
            if (!column.isVisible()) {
                continue;
            }

            Reference<TreeTableCell<S, ?>> tableCellReference = cellMap.get(column);
            if (tableCellReference == null) {
                cellMap.remove(column);
                continue;
            }

            TreeTableCell<S, ?> tableCell = tableCellReference.get();
            if (tableCell == null) {
                cellMap.remove(column);
                continue;
            }

            TableColumnBase<TreeItem<S>, ?> currentTopMostColumn = getTopMostColumn(column);
            boolean differentTopMostColumn = currentTopMostColumn != previousTopMostColumn;
            if (counter <= 0 && differentTopMostColumn) {
                tableCell.getStyleClass().remove(FIXED_TREE_TABLE_CELL);
                continue;
            }

            // When this is the cell of the same top most column we don't decrement the counter as they belong
            // together.
            if (differentTopMostColumn) {
                counter--;
            }

            if (!tableCell.getStyleClass().contains(FIXED_TREE_TABLE_CELL)) {
                tableCell.getStyleClass().add(FIXED_TREE_TABLE_CELL);
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

        // The disclosure node and the tree line drawer need to be resized and relocated too,
        // as otherwise they will scroll like the other unfixed cells.
        // It's also important that the relayout is performed in the end,
        // as otherwise the disclosure node and tree lines get hidden behind the other fixed cells.
        Node disclosureNode = getSkinnable().getDisclosureNode();
        if (disclosureNode != null && disclosureNode.isVisible()) {
            int indentationLevel = getSkinnable().getTreeTableView().getTreeItemLevel(getSkinnable().getTreeItem());
            if (!isShowRoot()) {
                indentationLevel--;
            }
            double indentationPerLevel = getIndent();
            double leftIndent = indentationLevel * indentationPerLevel;

            disclosureNode.setLayoutX(snapPositionX(startX + leftIndent) - disclosureNode.getLayoutBounds().getMinX());
            disclosureNode.toFront();
        }

        if (treeLineDrawer != null) {
            treeLineDrawer.relocate(startX, snappedTopInset());
        }
    }

    private void onPropertiesAdded(Change<?, ?> change) {
        if (!change.wasAdded() || cellMap == null) {
            return;
        }

        TreeTableRow<S> treeTableRow = getSkinnable();
        TreeTableView<S> treeTableView = treeTableRow.getTreeTableView();

        int index = treeTableRow.getIndex();
        // Empty or unused rows will be ignored.
        if (index < 0 || index >= treeTableView.getExpandedItemCount()) {
            return;
        }

        if (ExtendedTableSkin.REFRESH_COLUMN.equals(change.getKey())) {
            TreeTableColumn<S, ?> column = (TreeTableColumn<S, ?>) change.getValueAdded();

            WeakReference<TreeTableCell<S, ?>> cellRef = cellMap.get(column);
            if (cellRef == null) {
                return;
            }

            TreeTableCell<S, ?> treeTableCell = cellRef.get();
            if (treeTableCell == null) {
                return;
            }

            if (treeTableCell instanceof ExtendedTreeTableCell<S, ?> extendedTreeTableCell) {
                extendedTreeTableCell.refresh();
            }
        }
        if (ExtendedTableSkin.REFRESH.equals(change.getKey())) {
            // Update row first, then the cells.
            if (treeTableRow instanceof ExtendedTreeTableRow<?> extendedTreeTableRow) {
                extendedTreeTableRow.refresh();
            }

            for (WeakReference<TreeTableCell<S, ?>> cellRef : cellMap.values()) {
                if (cellRef == null) {
                    continue;
                }

                TreeTableCell<S, ?> treeTableCell = cellRef.get();
                if (treeTableCell == null) {
                    continue;
                }

                if (treeTableCell instanceof ExtendedTreeTableCell<S, ?> extendedTreeTableCell) {
                    extendedTreeTableCell.refresh();
                }
            }
        }
    }

    private void onShowTreeLinesChanged() {
        if (treeLineDrawer != null) {
            getChildren().remove(treeLineDrawer);

            treeLineDrawer = null;
        }

        getSkinnable().requestLayout();
    }

}
