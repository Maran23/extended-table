package tools.maran.extendedtable.treetableview.cell;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.TreeTableCellSkin;
import javafx.scene.control.skin.TreeTableRowSkin;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import tools.maran.extendedtable.table.common.CellComboBox;

/// [TreeTableCellSkin] for the [ExtendedTreeTableCell].
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class ExtendedTreeTableCellSkin<S, T> extends TreeTableCellSkin<S, T> {

    /// Key to save the indentation in the column properties.
    private static final String INDENTATION = "indentation";

    private static final PseudoClass PSEUDO_CLASS_EDITABLE = PseudoClass.getPseudoClass("editable");

    private ExtendedTreeTableCell<S, T> treeTableCell;

    /// Creates a new [ExtendedTreeTableCellSkin] instance.
    ///
    /// @param treeTableCell
    ///         the [ExtendedTreeTableCell] where this skin belongs to
    public ExtendedTreeTableCellSkin(ExtendedTreeTableCell<S, T> treeTableCell) {
        super(treeTableCell);
        this.treeTableCell = treeTableCell;

        registerChangeListener(treeTableCell.fixedOverlapProperty(), _ -> updateClip());
        registerChangeListener(treeTableCell.editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(treeTableCell.getTableRow().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(treeTableCell.getTableColumn().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(treeTableCell.getTreeTableView().editableProperty(), _ -> updateEditablePseudoClass());

        updateEditablePseudoClass();
    }

    @Override
    public void dispose() {
        super.dispose();

        treeTableCell = null;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        // Overwritten as JavaFX is calculating in some weird magic offset.
        double indentation = calculateIndent();
        double newX = x + indentation;
        double newW = w - indentation;

        layoutLabelInArea(newX, y, newW, h);
        layoutEditor(indentation);
        updateClip();
    }

    /// Copied from JavaFX, minimally adjusted to save the indentation in the column.
    ///
    /// @return the indentation
    private double calculateIndent() {
        double indentation = 0;

        TreeTableCell<S, T> cell = getSkinnable();

        TreeTableColumn<S, T> tableColumn = cell.getTableColumn();
        if (tableColumn == null) {
            return indentation;
        }

        // check if this column is the TreeTableView treeColumn (i.e. the
        // column showing the disclosure node and graphic).
        TreeTableView<S> treeTable = cell.getTreeTableView();
        if (treeTable == null) {
            return indentation;
        }

        int columnIndex = treeTable.getVisibleLeafIndex(tableColumn);

        TreeTableColumn<S, ?> treeColumn = treeTable.getTreeColumn();
        if ((treeColumn == null && columnIndex != 0) || (treeColumn != null && !tableColumn.equals(treeColumn))) {
            return indentation;
        }

        TreeTableRow<S> treeTableRow = cell.getTableRow();
        if (treeTableRow == null) {
            return indentation;
        }

        TreeItem<S> treeItem = treeTableRow.getTreeItem();
        if (treeItem == null) {
            return indentation;
        }

        int nodeLevel = treeTable.getTreeItemLevel(treeItem);
        if (!treeTable.isShowRoot()) {
            nodeLevel--;
        }

        double indentPerLevel = 10;
        if (treeTableRow.getSkin() instanceof TreeTableRowSkin) {
            indentPerLevel = ((TreeTableRowSkin<?>) treeTableRow.getSkin()).getIndent();
        }
        indentation += nodeLevel * indentPerLevel;

        double tableColIndent = getTableColumnIndent(tableColumn);
        indentation += tableColIndent;

        // adding in the width of the graphic on the tree item
        Node graphic = treeItem.getGraphic();
        indentation += graphic == null ? 0 : graphic.prefWidth(getSkinnable().getHeight());

        return indentation;
    }

    private double getTableColumnIndent(TreeTableColumn<S, T> tableColumn) {
        // add in the width of the disclosure node, if one exists
        TreeTableRow<S> tableRow = getSkinnable().getTableRow();
        Node disclosureNode = tableRow.getDisclosureNode();

        double tableColIndent = 0;

        Object identObj = tableColumn.getProperties().get(INDENTATION);
        if (identObj != null) {
            tableColIndent = (double) identObj;
        }

        double newTableColIndent = disclosureNode.prefWidth(getSkinnable().getHeight());
        if (newTableColIndent > tableColIndent) {
            tableColIndent = newTableColIndent;

            // Only the maximum width for this column so that other rows can grab it
            // (that may have not a disclosure node).
            tableColumn.getProperties().put(INDENTATION, tableColIndent);
        }
        return tableColIndent;
    }

    /// While editing, the editor fills the cell from the given x with the side insets of the cell as its padding, so its
    /// text stays in place. It draws the editing ring, as a theme may style the background of the cell, e.g. when
    /// selected. Its text is centered vertically, so the height of the row stays.
    private void layoutEditor(double x) {
        Node graphic = treeTableCell.getGraphic();
        if (!(graphic instanceof TextInputControl || graphic instanceof CellComboBox<?>)) {
            return;
        }

        Region editor = (Region) graphic;
        boolean isEditing = treeTableCell.isEditing();
        Insets padding = isEditing ? new Insets(0, treeTableCell.snappedRightInset(), 0, treeTableCell.snappedLeftInset()) : Insets.EMPTY;
        if (!padding.equals(editor.getPadding())) {
            editor.setPadding(padding);
        }
        if (isEditing) {
            editor.resizeRelocate(x, 0, treeTableCell.getWidth() - x, treeTableCell.getHeight());
        }
    }

    /// Clips the part of the cell which is overlapped by the fixed cells. The [Rectangle] clip of the super skin is
    /// reused: its width is unbound while overlapped and bound to the cell again afterwards.
    private void updateClip() {
        double overlap = treeTableCell.getFixedOverlap();
        Rectangle clip = treeTableCell.getClip() instanceof Rectangle rectangle ? rectangle : null;
        if (clip == null) {
            if (overlap == 0) {
                return;
            }
            clip = new Rectangle();
            clip.heightProperty().bind(treeTableCell.heightProperty());
            treeTableCell.setClip(clip);
        }

        if (overlap == 0) {
            if (!clip.widthProperty().isBound()) {
                clip.setX(0);
                clip.widthProperty().bind(treeTableCell.widthProperty());
            }
            return;
        }

        clip.widthProperty().unbind();
        clip.setX(overlap);
        clip.setWidth(Math.max(0, treeTableCell.getWidth() - overlap));
    }

    private void updateEditablePseudoClass() {
        boolean isEditable =
                treeTableCell.isEditable() && treeTableCell.getTableRow().isEditable() && treeTableCell.getTableColumn()
                        .isEditable() && treeTableCell.getTreeTableView().isEditable();
        treeTableCell.pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE, isEditable);
        treeTableCell.updateValidState();
    }
}
