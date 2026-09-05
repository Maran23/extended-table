package tools.maran.extendedtable.treetableview.line;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/// [Canvas] responsible for drawing tree lines for a given [TreeLineDrawable].
///
/// @author Marius Hanl
public class TreeLineDrawer extends Canvas {

    private StyleableObjectProperty<Paint> stroke;
    private StyleableDoubleProperty size;

    private final TreeLineDrawable treeLineDrawable;

    /// Creates a new [TreeLineDrawer] instance.
    ///
    /// @param treeLineDrawable
    ///         the object where the tree lines will be drawn to
    public TreeLineDrawer(TreeLineDrawable treeLineDrawable) {
        this.treeLineDrawable = treeLineDrawable;
        setMouseTransparent(true);
        getStyleClass().add("tree-line-drawer");

        getGraphicsContext2D().setStroke(getStroke());
        getGraphicsContext2D().setLineWidth(getSize());
    }

    /// Clears the tree lines.
    public void clear() {
        getGraphicsContext2D().clearRect(0, 0, getWidth(), getHeight());
    }

    /// Draws the tree lines.
    public void drawTreeLines() {
        TreeItem<?> treeItem = treeLineDrawable.getTreeItem();
        if (treeItem == null) {
            return;
        }

        GraphicsContext graphicsContext = getGraphicsContext2D();

        int treeItemLevel = treeLineDrawable.getTreeItemLevel(treeItem);
        double indentation = treeLineDrawable.getIndent();

        setWidth(treeLineDrawable.getCellWidth());
        setHeight(treeLineDrawable.getCellHeight());

        // Base indentation
        final double baseIndent = indentation;

        // Indent offset
        double indentOffset = 0;

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
            double startX = snap(indentation * treeItemLevel - indentOffset) - 1;

            // Every other level than 0 will just need a plain vertical line: |
            if (level != 0) {
                // Vertical line over the whole cell.
                graphicsContext.strokeLine(startX, 0, startX, snap(getHeight()));
            } else {
                // The line for the innermost tree item (level 0) should either look like this: '- or |-
                ObservableList<? extends TreeItem<?>> children = treeItem.getParent().getChildren();
                boolean isLastChild = children.getLast() == treeItem;

                // Y coordinate of the mid of this cell.
                double yCellMid = snap(getHeight() / 2);

                boolean isDisclosureNodeVisible = disclosureNode != null && disclosureNode.isVisible();

                double lineWidthMultiplier = isDisclosureNodeVisible ? 0.5 : 1;
                double endX = startX + lineWidthMultiplier * baseIndent;

                if (isLastChild) {
                    // When this tree item is the last child, the line should look like this: '-

                    // Vertical line to the mid y of the cell since this is the last child.
                    graphicsContext.strokeLine(startX, 0, startX, yCellMid);
                } else {
                    // Otherwise, it should look like this: |-

                    // Vertical line over the whole cell.
                    graphicsContext.strokeLine(startX, 0, startX, snap(getHeight()));
                }
                // Horizontal line to the before calculated end x coordinate.
                graphicsContext.strokeLine(startX, yCellMid, endX, yCellMid);
            }
            treeItem = treeItem.getParent();
        }
    }

    /// Gets the `CssMetaData` associated with this class, which may include the `CssMetaData` of its
    /// superclasses.
    ///
    /// @return the `CssMetaData`
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /// This method returns a list of [CssMetaData] for this component.
    ///
    /// @return The [CssMetaData] associated with this node, which may include the CssMetaData of its superclasses
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    /// Gets the size.
    ///
    /// @return the size
    public final double getSize() {
        return size == null ? 1 : sizeProperty().get();
    }

    /// Gets the stroke.
    ///
    /// @return the stroke
    public final Paint getStroke() {
        return stroke == null ? Color.BLACK : strokeProperty().get();
    }

    /// Sets the size.
    ///
    /// @param value
    ///         the size
    public final void setSize(double value) {
        sizeProperty().set(value);
    }

    /// Sets the stroke.
    ///
    /// @param value
    ///         the stroke
    public final void setStroke(Paint value) {
        strokeProperty().set(value);
    }

    /// Property with the size used for the lines.
    ///
    /// Can be set in CSS with `-fx-size`.
    ///
    /// @return the [StyleableDoubleProperty] with the size
    /// @defaultValue 1
    public final StyleableDoubleProperty sizeProperty() {
        if (size == null) {
            size = new StyleableDoubleProperty(1) {

                @Override
                public Object getBean() {
                    return TreeLineDrawer.this;
                }

                @Override
                public CssMetaData<TreeLineDrawer, Number> getCssMetaData() {
                    return StyleableProperties.SIZE;
                }

                @Override
                public String getName() {
                    return "size";
                }

                @Override
                protected void invalidated() {
                    getGraphicsContext2D().setLineWidth(getSize());
                }
            };
        }
        return size;
    }

    /// Property with the [Paint] used for the lines.
    ///
    /// Can be set in CSS with `-fx-stroke`.
    ///
    /// @return the [StyleableObjectProperty] with the [Paint]
    /// @defaultValue black
    public final StyleableObjectProperty<Paint> strokeProperty() {
        if (stroke == null) {
            stroke = new StyleableObjectProperty<>(Color.BLACK) {

                @Override
                public Object getBean() {
                    return TreeLineDrawer.this;
                }

                @Override
                public CssMetaData<TreeLineDrawer, Paint> getCssMetaData() {
                    return StyleableProperties.STROKE;
                }

                @Override
                public String getName() {
                    return "stroke";
                }

                @Override
                protected void invalidated() {
                    getGraphicsContext2D().setStroke(getStroke());
                }
            };
        }
        return stroke;
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

    /// Snaps the given input to the center if it is an odd number. This enables the canvas to draw sharp lines when
    /// an integer size is used.
    ///
    /// @param input
    ///         the input
    /// @return the snapped input
    private double snap(double input) {
        if (getGraphicsContext2D().getLineWidth() % 2 == 0) {
            return input;
        }
        // Odd numbers needs to be snapped to the center coordinate (+0.5).
        return ((int) input) + 0.5;
    }

    /// Styleable properties for the [TreeLineDrawer].
    ///
    /// @author Marius Hanl
    private static class StyleableProperties {

        private static final CssMetaData<TreeLineDrawer, Paint> STROKE = new CssMetaData<>("-fx-stroke",
                PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public StyleableProperty<Paint> getStyleableProperty(TreeLineDrawer treeLineDrawer) {
                return treeLineDrawer.strokeProperty();
            }

            @Override
            public boolean isSettable(TreeLineDrawer treeLineDrawer) {
                return treeLineDrawer.stroke == null || !treeLineDrawer.stroke.isBound();
            }
        };
        private static final CssMetaData<TreeLineDrawer, Number> SIZE = new CssMetaData<>("-fx-size",
                SizeConverter.getInstance(), 1) {

            @Override
            public StyleableProperty<Number> getStyleableProperty(TreeLineDrawer treeLineDrawer) {
                return treeLineDrawer.sizeProperty();
            }

            @Override
            public boolean isSettable(TreeLineDrawer treeLineDrawer) {
                return treeLineDrawer.size == null || !treeLineDrawer.size.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Canvas.getClassCssMetaData());
            styleables.add(STROKE);
            styleables.add(SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

}
