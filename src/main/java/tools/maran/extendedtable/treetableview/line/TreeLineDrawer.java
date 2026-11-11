package tools.maran.extendedtable.treetableview.line;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.transform.Scale;
import javafx.stage.Window;

/// [Path] responsible for drawing tree lines for a given [TreeLineDrawable].
/// The lines are snapped to the physical pixels of the render scale, so they stay sharp on every screen.
/// The line width is set with `-fx-stroke-width` and rounded to whole physical pixels.
///
/// @author Marius Hanl
public class TreeLineDrawer extends Path {

    private final TreeLineDrawable treeLineDrawable;

    /// Creates a new [TreeLineDrawer] instance.
    ///
    /// @param treeLineDrawable
    ///         the object where the tree lines will be drawn to
    public TreeLineDrawer(TreeLineDrawable treeLineDrawable) {
        this.treeLineDrawable = treeLineDrawable;
        setManaged(false);
        setMouseTransparent(true);
        setStrokeLineCap(StrokeLineCap.BUTT);
        getStyleClass().add("tree-line-drawer");
    }

    /// Clears the tree lines.
    public void clear() {
        getElements().clear();
    }

    /// Draws the tree lines.
    public void drawTreeLines() {
        TreeItem<?> treeItem = treeLineDrawable.getTreeItem();
        if (treeItem == null || getStrokeWidth() <= 0) {
            return;
        }

        Window window = getScene() == null ? null : getScene().getWindow();
        double scaleX = window == null ? 1 : window.getRenderScaleX();
        double scaleY = window == null ? 1 : window.getRenderScaleY();

        // Line widths in physical pixels, of the vertical (x) and horizontal (y) lines.
        double lineWidthX = Math.max(1, Math.round(getStrokeWidth() * scaleX));
        double lineWidthY = Math.max(1, Math.round(getStrokeWidth() * scaleY));

        // Scales the stroke to the physical line widths, while the coordinates are scaled back when added.
        Scale strokeScale = new Scale(strokeScale(getStrokeWidth(), scaleX), strokeScale(getStrokeWidth(), scaleY));
        getTransforms().setAll(strokeScale);

        double width = treeLineDrawable.getCellWidth();
        double height = treeLineDrawable.getCellHeight();

        int treeItemLevel = treeLineDrawable.getTreeItemLevel(treeItem);
        double indentation = treeLineDrawable.getIndent();

        // Base indentation
        final double baseIndent = indentation;

        // Indent offset
        double indentOffset = 0;

        List<PathElement> elements = new ArrayList<>();

        // Loop through all indented level (from inside to outside)
        boolean[] levelToDraw = getLevelToDraw(treeItemLevel);
        Node disclosureNode = treeLineDrawable.getDisclosureNode();
        for (int level = 0; level < levelToDraw.length; level++) {
            if (!levelToDraw[level]) {
                treeItem = treeItem.getParent();
                continue;
            }

            if (!treeLineDrawable.isShowRoot()) {
                // The last level (root node) does not need a line.
                if (level == (levelToDraw.length - 1)) {
                    break;
                }

                // If there is no root, the second level is less indented.
                if (level == (levelToDraw.length - 2)) {
                    indentation /= 2;
                    indentOffset = 0;
                } else {
                    indentation = baseIndent;
                    indentOffset = indentation;
                }
            }

            treeItemLevel = treeLineDrawable.getTreeItemLevel(treeItem);

            // X coordinate of the starting point.
            double startX = snap(indentation * treeItemLevel - indentOffset - 1, scaleX, lineWidthX);

            // Lines outside the cell are not visible.
            if (startX > width) {
                treeItem = treeItem.getParent();
                continue;
            }

            // Every other level than 0 will just need a plain vertical line: |
            if (level != 0) {
                // Vertical line over the whole cell.
                addLine(elements, strokeScale, startX, 0, startX, height);
            } else {
                // The line for the innermost tree item (level 0) should either look like this: '- or |-
                ObservableList<? extends TreeItem<?>> children = treeItem.getParent().getChildren();
                boolean isLastChild = children.getLast() == treeItem;

                // Y coordinate of the mid of this cell.
                double yCellMid = snap(height / 2, scaleY, lineWidthY);

                boolean isDisclosureNodeVisible = disclosureNode != null && disclosureNode.isVisible();

                double lineWidthMultiplier = isDisclosureNodeVisible ? 0.5 : 1;
                double endX = Math.min(width, Math.round((startX + lineWidthMultiplier * baseIndent) * scaleX) / scaleX);

                if (isLastChild) {
                    // When this tree item is the last child, the line should look like this: '-
                    // The vertical line goes until the bottom edge of the horizontal line to close the corner.
                    addLine(elements, strokeScale, startX, 0, startX, yCellMid + lineWidthY / scaleY / 2);
                } else {
                    // Otherwise, it should look like this: |-
                    addLine(elements, strokeScale, startX, 0, startX, height);
                }
                // Horizontal line to the before calculated end x coordinate.
                addLine(elements, strokeScale, startX, yCellMid, endX, yCellMid);
            }
            treeItem = treeItem.getParent();
        }

        getElements().setAll(elements);
    }

    /// Snaps the given coordinate to the physical pixel grid, so that a line with the given physical width is drawn
    /// sharp. Odd widths are centered on a physical pixel.
    ///
    /// @param value
    ///         the coordinate
    /// @param scale
    ///         the render scale
    /// @param lineWidth
    ///         the line width in physical pixels
    /// @return the snapped coordinate
    static double snap(double value, double scale, double lineWidth) {
        double physical = Math.floor(value * scale);
        return (lineWidth % 2 == 0 ? physical : physical + 0.5) / scale;
    }

    /// Returns the factor which scales the given stroke width to whole physical pixels.
    ///
    /// @param strokeWidth
    ///         the stroke width
    /// @param scale
    ///         the render scale
    /// @return the factor
    static double strokeScale(double strokeWidth, double scale) {
        return Math.max(1, Math.round(strokeWidth * scale)) / scale / strokeWidth;
    }

    private static void addLine(List<PathElement> elements, Scale strokeScale, double startX, double startY,
            double endX, double endY) {
        double x = strokeScale.getX();
        double y = strokeScale.getY();
        elements.add(new MoveTo(startX / x, startY / y));
        elements.add(new LineTo(endX / x, endY / y));
    }

    /// Gets all levels where a line should be drawn. If the corresponding value is true, a line should be drawn to it.
    ///
    /// @param treeItemLevel
    ///         the tree item level
    /// @return array containing all level where lines should be drawn to. True means that a line of some kind should be
    /// drawn for a given level
    private boolean[] getLevelToDraw(int treeItemLevel) {
        TreeItem<?> currentTreeItem = treeLineDrawable.getTreeItem();

        boolean[] indentedLines = new boolean[treeItemLevel];

        int indention = 0;
        while (currentTreeItem != null && currentTreeItem.getParent() != null) {
            ObservableList<? extends TreeItem<?>> children = currentTreeItem.getParent().getChildren();
            boolean isLastChild = children.getLast() == currentTreeItem;

            if (!isLastChild || indention == 0) {
                indentedLines[indention] = true;
            }
            currentTreeItem = currentTreeItem.getParent();

            indention++;
        }
        return indentedLines;
    }

}
