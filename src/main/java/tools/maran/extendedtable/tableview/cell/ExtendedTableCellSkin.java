package tools.maran.extendedtable.tableview.cell;

import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.skin.TableCellSkin;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

import tools.maran.extendedtable.table.common.CellComboBox;

/// [TableCellSkin] for the [ExtendedTableCell].
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class ExtendedTableCellSkin<S, T> extends TableCellSkin<S, T> {

    private static final PseudoClass PSEUDO_CLASS_EDITABLE = PseudoClass.getPseudoClass("editable");

    private ExtendedTableCell<S, T> tableCell;

    /// Creates a new [ExtendedTableCellSkin] instance.
    ///
    /// @param tableCell
    ///         the [ExtendedTableCell] where this skin belongs to
    public ExtendedTableCellSkin(ExtendedTableCell<S, T> tableCell) {
        super(tableCell);
        this.tableCell = tableCell;

        registerChangeListener(tableCell.fixedOverlapProperty(), _ -> updateClip());
        registerChangeListener(tableCell.editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableRow().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableColumn().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableView().editableProperty(), _ -> updateEditablePseudoClass());

        updateEditablePseudoClass();
    }

    @Override
    public void dispose() {
        super.dispose();

        tableCell = null;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        // Overwritten as JavaFX is calculating in some weird magic offset.
        layoutLabelInArea(x, y, w, h);
        layoutEditor(0);
        updateClip();
    }

    /// While editing, the editor fills the cell from the given x with the side insets of the cell as its padding, so its
    /// text stays in place. It draws the editing ring, as a theme may style the background of the cell, e.g. when
    /// selected. Its text is centered vertically, so the height of the row stays.
    private void layoutEditor(double x) {
        Node graphic = tableCell.getGraphic();
        if (!(graphic instanceof TextInputControl || graphic instanceof CellComboBox<?>)) {
            return;
        }

        Region editor = (Region) graphic;
        boolean isEditing = tableCell.isEditing();
        Insets padding = isEditing ? new Insets(0, tableCell.snappedRightInset(), 0, tableCell.snappedLeftInset()) : Insets.EMPTY;
        if (!padding.equals(editor.getPadding())) {
            editor.setPadding(padding);
        }
        if (isEditing) {
            editor.resizeRelocate(x, 0, tableCell.getWidth() - x, tableCell.getHeight());
        }
    }

    /// Clips the part of the cell which is overlapped by the fixed cells. The [Rectangle] clip of the super skin is
    /// reused: its width is unbound while overlapped and bound to the cell again afterwards.
    private void updateClip() {
        double overlap = tableCell.getFixedOverlap();
        Rectangle clip = tableCell.getClip() instanceof Rectangle rectangle ? rectangle : null;
        if (clip == null) {
            if (overlap == 0) {
                return;
            }
            clip = new Rectangle();
            clip.heightProperty().bind(tableCell.heightProperty());
            tableCell.setClip(clip);
        }

        if (overlap == 0) {
            if (!clip.widthProperty().isBound()) {
                clip.setX(0);
                clip.widthProperty().bind(tableCell.widthProperty());
            }
            return;
        }

        clip.widthProperty().unbind();
        clip.setX(overlap);
        clip.setWidth(Math.max(0, tableCell.getWidth() - overlap));
    }

    private void updateEditablePseudoClass() {
        boolean isEditable =
                tableCell.isEditable() && tableCell.getTableRow().isEditable() && tableCell.getTableColumn()
                        .isEditable() && tableCell.getTableView().isEditable();
        tableCell.pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE, isEditable);
        tableCell.updateValidState();
    }
}
