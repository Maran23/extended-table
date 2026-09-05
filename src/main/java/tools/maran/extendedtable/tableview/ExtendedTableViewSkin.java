package tools.maran.extendedtable.tableview;

import java.util.function.ToDoubleFunction;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.skin.TableViewSkin;

import tools.maran.extendedtable.table.common.ExtendedTable;
import tools.maran.extendedtable.table.common.ExtendedTableSkin;
import tools.maran.extendedtable.table.header.ExtendedRootHeader;
import tools.maran.extendedtable.table.header.ExtendedTableHeader;
import tools.maran.extendedtable.table.header.ExtendedTableHeaderRow;
import tools.maran.extendedtable.table.virtualflow.ExtendedVirtualFlow;

/// [TableViewSkin] which supports the [ExtendedTableView] functionality.
/// That is fixed cells support, an optional table header
/// as well as multiple operations, like refresh or autosizing.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class ExtendedTableViewSkin<S> extends TableViewSkin<S> implements ExtendedTableSkin<S> {

    private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

    private ExtendedTableHeader tableHeader;

    /// Creates a new [ExtendedTableViewSkin] instance.
    ///
    /// @param tableView
    ///         the [ExtendedTableView] this skin belongs to
    public ExtendedTableViewSkin(ExtendedTableView<S> tableView) {
        super(tableView);

        registerChangeListener(tableView.showHeaderProperty(), _ -> headerVisibilityChanged(tableView.isShowHeader()));
        headerVisibilityChanged(tableView.isShowHeader());

        ObservableMap<Object, Object> properties = tableView.getProperties();
        properties.remove(AUTOSIZE_COLUMNS);
        properties.remove(REFRESH);
        properties.remove(REFRESH_COLUMN);
        properties.addListener(propertiesMapListener);
    }

    @Override
    public ObjectProperty<ToDoubleFunction<S>> cellSizeProviderProperty() {
        return ((ExtendedTableView<S>) getSkinnable()).cellSizeProviderProperty();
    }

    @Override
    public void dispose() {
        if (getSkinnable() == null) {
            return;
        }

        getSkinnable().getProperties().removeListener(propertiesMapListener);

        if (tableHeader != null) {
            tableHeader.dispose();
            tableHeader = null;
        }

        super.dispose();
    }

    @Override
    public final IntegerProperty fixedColumnCountProperty() {
        return ((ExtendedTableView<S>) getSkinnable()).fixedColumnCountProperty();
    }

    @Override
    public final int getFixedColumnCount() {
        return ((ExtendedTableView<S>) getSkinnable()).getFixedColumnCount();
    }

    @Override
    public double getHBarValue() {
        return ((ExtendedVirtualFlow<TableRow<S>>) getVirtualFlow()).getHorizontalBarValue();
    }

    @Override
    public void requestCellLayout() {
        // JavaFX will request a layout when the fixed cell size is > 0.
        // We also need this without a fixed cell size set when the fixed column count is > 0.
        if (getSkinnable().getFixedCellSize() <= 0 && getFixedColumnCount() > 0) {
            ((ExtendedVirtualFlow<TableRow<S>>) getVirtualFlow()).requestCellLayout();
        }
    }

    @Override
    protected ExtendedTableHeaderRow createTableHeaderRow() {
        return new ExtendedTableHeaderRow(this);
    }

    @Override
    protected ExtendedVirtualFlow<TableRow<S>> createVirtualFlow() {
        return new ExtendedVirtualFlow<>();
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        boolean isShowHeader = showHeaderProperty().get();
        if (!isShowHeader) {
            super.layoutChildren(x, y, w, h);
        } else {
            double headerHeight = tableHeader.prefHeight(w);
            layoutInArea(tableHeader, x, y, w, headerHeight, 0, HPos.CENTER, VPos.CENTER);
            super.layoutChildren(x, y + headerHeight, w, h - headerHeight);
        }
    }

    @Override
    protected void scrollHorizontally(TableColumn<S, ?> col) {
        if (col == null || !col.isVisible()) {
            return;
        }

        // work out where this column header is, and it's width (start -> end)
        double start = 0;
        double end = 0;

        int fixedColumnCount = getFixedColumnCount();
        for (TableColumn<S, ?> c : getSkinnable().getVisibleLeafColumns()) {
            if (c == col) {
                break;
            }

            if (fixedColumnCount > 0) {
                // we do not count fixed columns in as they will always be in front, therefore can be ignored.
                fixedColumnCount--;
                end += c.getWidth();
            } else {
                start += c.getWidth();
            }
        }
        end = end + start + col.getWidth();

        // determine the visible width of the table
        double headerWidth = getSkinnable().getWidth();

        // determine by how much we need to translate the table to ensure that the start position of this
        // column lines up with the left edge of the tableview, and also that the columns don't become detached from the
        // right edge of the table
        ScrollBar hBar = getHBar();
        double pos = hBar.getValue();
        double max = hBar.getMax();

        double newPos;
        if (start < pos && start >= 0) {
            newPos = start;
        } else {
            double delta = start < 0 || end > headerWidth ? start - pos : 0;
            newPos = Math.min(pos + delta, max);
        }

        hBar.setValue(newPos);
    }

    private ScrollBar getHBar() {
        return ((ExtendedVirtualFlow<TableRow<S>>) getVirtualFlow()).getHorizontalBar();
    }

    private void headerVisibilityChanged(boolean isShowHeader) {
        if (tableHeader == null) {
            if (!isShowHeader) {
                return;
            }
            tableHeader = new ExtendedTableHeader((ExtendedTable<?>) getSkinnable());
        }

        if (isShowHeader) {
            getChildren().add(tableHeader);
        } else {
            getChildren().remove(tableHeader);
        }

        updateShowHeaderPseudoClass(isShowHeader);

        getSkinnable().requestLayout();
    }

    private void onPropertiesAdded(Change<?, ?> change) {
        if (!change.wasAdded()) {
            return;
        }
        // Autosize propagation.
        if (AUTOSIZE_COLUMNS.equals(change.getKey())) {
            getSkinnable().getProperties().remove(AUTOSIZE_COLUMNS);

            if (getTableHeaderRow().getRootHeader() instanceof ExtendedRootHeader rootHeader) {
                rootHeader.resizeColumnToFitContent();
            }
        }
        // Refresh is handled by every row.
        // So they will receive this event, and we remove it here once and for all.
        if (REFRESH.equals(change.getKey())) {
            getSkinnable().getProperties().remove(REFRESH);
        }
        // Refresh column is handled by every row.
        // So they will receive this event, and we remove it here once and for all.
        if (REFRESH_COLUMN.equals(change.getKey())) {
            getSkinnable().getProperties().remove(REFRESH_COLUMN);
        }
    }

    private BooleanProperty showHeaderProperty() {
        return ((ExtendedTableView<S>) getSkinnable()).showHeaderProperty();
    }

    private void updateShowHeaderPseudoClass(boolean showHeader) {
        getSkinnable().pseudoClassStateChanged(PSEUDO_CLASS_SHOW_HEADER, showHeader);
    }
}
