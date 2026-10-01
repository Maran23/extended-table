package tools.maran.extendedtable.table.header;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.control.skin.TableViewSkinBase;
import javafx.scene.shape.Rectangle;

import tools.maran.extendedtable.table.common.ExtendedTableSkin;

/// Root header which lays out the fixed column headers if set.
///
/// @author Marius Hanl
public class ExtendedRootHeader extends ExtendedNestedTableColumnHeader {

    private static final String KEY = "TableColumn";

    /// Creates a new [ExtendedRootHeader] instance.
    public ExtendedRootHeader() {
        super(null);
    }

    /// Layout the fixed column headers, if set.
    public void layoutFixedColumns() {
        TableViewSkinBase<?, ?, ?, ?, ?> tableSkin = getTableSkin();

        if (tableSkin instanceof ExtendedTableSkin<?> extendedTableViewSkin) {
            int fixedColumnCount = extendedTableViewSkin.getFixedColumnCount();
            double hScrollValue = extendedTableViewSkin.getHBarValue();

            if (fixedColumnCount > 0) {
                layoutFixedColumnsImpl(fixedColumnCount, hScrollValue);
            }
        }
    }

    @Override
    public void resizeColumnToFitContent() {
        resizeChildColumnsToFitContent();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        layoutFixedColumns();
    }

    @Override
    protected void resizeColumnToFitContent(int maxRows) {
        // Note: This method is never called by JavaFX.
        resizeColumnToFitContent();
    }

    private void layoutFixedColumnsImpl(int fixedColumnCount, double hScrollValue) {
        double x = snappedLeftInset();
        double labelHeight = getChildren().getFirst().prefHeight(-1);
        double newY = snapPositionY(labelHeight + snappedTopInset());

        double fixedColumnWidth = 0;
        int counter = fixedColumnCount;

        // we need to collect the drag rectangles manually here, as they are private in NestedTableColumnHeader
        Map<TableColumnBase<?, ?>, Rectangle> dragRects = new HashMap<>();
        for (Node node : getChildren()) {
            if (node instanceof Rectangle rect && rect.getProperties()
                    .get(KEY) instanceof TableColumnBase<?, ?> tableColumn) {
                dragRects.put(tableColumn, rect);
            }
        }

        for (int index = 0; index < getColumnHeaders().size(); index++) {
            TableColumnHeader header = getColumnHeaders().get(index);
            if (!header.isVisible()) {
                continue;
            }

            if (counter-- == 0) {
                break;
            }

            double prefWidth = header.prefWidth(-1);

            double newX = x;
            if (hScrollValue + fixedColumnWidth > x) {
                newX = snapPositionX(Math.abs(hScrollValue) + fixedColumnWidth);

                header.toFront();
                fixedColumnWidth = snapSpaceX(fixedColumnWidth + prefWidth);
            }

            header.relocate(newX, newY);

            Rectangle dragRect = dragRects.get(header.getTableColumn());
            if (dragRect != null) {
                double rightHeaderEdge = newX + header.getWidth();
                double rectCenter = dragRect.getWidth() / 2;
                dragRect.relocate(snapPositionX(rightHeaderEdge - rectCenter), newY);
            }

            x = snapPositionX(x + prefWidth);
        }

        dragRects.values().forEach(Node::toFront);
    }
}
