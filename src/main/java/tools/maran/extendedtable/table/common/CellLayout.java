package tools.maran.extendedtable.table.common;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/// Layout of the editor and clip of a table cell, shared by the cell skins of every table.
///
/// @author Marius Hanl
public final class CellLayout {

    private CellLayout() {
    }

    /// While editing, the editor fills the cell from the given x with the side insets of the cell as its padding, so its
    /// text stays in place. It draws the editing ring, as a theme may style the background of the cell, e.g. when
    /// selected. Its text is centered vertically, so the height of the row stays.
    ///
    /// @param cell
    ///         the cell
    /// @param x
    ///         the x where the editor starts
    public static void layoutEditor(IndexedCell<?> cell, double x) {
        Node graphic = cell.getGraphic();
        if (!(graphic instanceof TextInputControl || graphic instanceof CellComboBox<?>)) {
            return;
        }

        Region editor = (Region) graphic;
        boolean isEditing = cell.isEditing();
        Insets padding = isEditing ? new Insets(0, cell.snappedRightInset(), 0, cell.snappedLeftInset())
                : Insets.EMPTY;
        if (!padding.equals(editor.getPadding())) {
            editor.setPadding(padding);
        }
        if (isEditing) {
            editor.resizeRelocate(x, 0, cell.getWidth() - x, cell.getHeight());
        }
    }

    /// Clips the part of the cell which is overlapped by the fixed cells. The [Rectangle] clip of the cell skin is
    /// reused: its width is unbound while overlapped and bound to the cell again afterwards.
    ///
    /// @param cell
    ///         the cell
    /// @param overlap
    ///         the width of the cell, from its left edge, which is overlapped by the fixed cells
    public static void updateClip(IndexedCell<?> cell, double overlap) {
        Rectangle clip = cell.getClip() instanceof Rectangle rectangle ? rectangle : null;
        if (clip == null) {
            if (overlap == 0) {
                return;
            }
            clip = new Rectangle();
            clip.heightProperty().bind(cell.heightProperty());
            cell.setClip(clip);
        }

        if (overlap == 0) {
            if (!clip.widthProperty().isBound()) {
                clip.setX(0);
                clip.widthProperty().bind(cell.widthProperty());
            }
            return;
        }

        clip.widthProperty().unbind();
        clip.setX(overlap);
        clip.setWidth(Math.max(0, cell.getWidth() - overlap));
    }
}
