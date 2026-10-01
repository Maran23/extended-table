package tools.maran.extendedtable.table.header;

import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.skin.NestedTableColumnHeader;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.layout.Region;

/// Extended [NestedTableColumnHeader] with a better autosizing algorithm.
/// It usually contains children table headers.
///
/// @author Marius Hanl
public class ExtendedNestedTableColumnHeader extends NestedTableColumnHeader {

    private boolean isTitleFitted;

    /// Creates a new [ExtendedNestedTableColumnHeader] instance.
    ///
    /// @param tableColumnBase
    ///         the [TableColumnBase]
    public ExtendedNestedTableColumnHeader(TableColumnBase<?, ?> tableColumnBase) {
        super(tableColumnBase);
    }

    /// Resizes this `NestedTableColumnHeader`'s column to fit the width of its title.
    /// Since a nested column header has no cells (the underlying columns have), we only can estimate the header width here.
    public void resizeColumnToFitContent() {
        // When there is no scene we should not do resizing as it will result in an exception and a wrong calculation
        // anyway.
        if (getScene() == null) {
            return;
        }

        if (!getTableColumn().isResizable()) {
            return;
        }

        resizeChildColumnsToFitContent();
        resizeColumnToFitTitle();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected TableColumnHeader createTableColumnHeader(TableColumnBase col) {
        return col == null || col.getColumns().isEmpty() || col == getTableColumn() ? new ExtendedTableColumnHeader(col)
                : new ExtendedNestedTableColumnHeader(col);
    }

    @Override
    protected void layoutChildren() {
        // JavaFX only autosizes columns which still have the default width, which a nested column with multiple
        // children never has. So we fit the title once, as soon as the header is laid out.
        if (!isTitleFitted && getTableColumn() != null && getTableColumn().isResizable()) {
            isTitleFitted = true;
            resizeColumnToFitTitle();
        }

        super.layoutChildren();
    }

    /// Resizes all children columns, including nested ones, to fit their content.
    protected final void resizeChildColumnsToFitContent() {
        for (TableColumnHeader columnHeader : getColumnHeaders()) {
            if (columnHeader instanceof ExtendedTableColumnHeader extendedHeader) {
                extendedHeader.resizeColumnToFitContent();
            } else if (columnHeader instanceof ExtendedNestedTableColumnHeader extendedNestedHeader) {
                extendedNestedHeader.resizeColumnToFitContent();
            }
        }
    }

    @Override
    protected void resizeColumnToFitContent(int maxRows) {
        resizeColumnToFitContent();
    }

    private void resizeColumnToFitTitle() {
        // Apply css so that everything is ready in the header.
        applyCss();

        double nestedPrefWidth = snapSpaceX(prefWidth(getHeight()));

        // The first entry is the TableColumnHeader, and from there the first entry is the Label.
        // There is no other way to access that.
        Region columnHeader = (Region) getChildren().getFirst();
        Region label = (Region) columnHeader.getChildrenUnmodifiable().getFirst();
        double colInsets = columnHeader.snappedLeftInset() + columnHeader.snappedRightInset();
        double colWidth = columnHeader.snapSizeX(label.prefWidth(-1));
        double headerWidth = columnHeader.snapSpaceX(colInsets + colWidth + columnHeader.snapSpaceX(4));

        if (nestedPrefWidth >= headerWidth) {
            // The nested columns are already wide enough for our title.
            return;
        }

        // Shift the column (and nested columns) by the amount our header is bigger than the nested columns.
        double delta = headerWidth - nestedPrefWidth;

        Control control = getTableSkin().getSkinnable();
        if (control instanceof TableView<?> tableView) {
            tableView.resizeColumn((TableColumn) getTableColumn(), delta);
        } else if (control instanceof TreeTableView<?> treeTableView) {
            treeTableView.resizeColumn((TreeTableColumn) getTableColumn(), delta);
        }

        // Lay out the nested headers right away, otherwise they keep their unsnapped widths and leave pixel gaps.
        for (TableColumnHeader header : getColumnHeaders()) {
            header.requestLayout();
            header.layout();
        }
    }

}
