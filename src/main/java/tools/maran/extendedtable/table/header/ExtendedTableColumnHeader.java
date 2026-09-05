package tools.maran.extendedtable.table.header;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import tools.maran.extendedtable.filter.FilterableColumn;

/// Extended [TableColumnHeader] which supports filtering and a better autosizing algorithm.
///
/// @author Marius Hanl
public class ExtendedTableColumnHeader extends TableColumnHeader {

    private static final String FILTERED_STYLE_CLASS = "filtered";

    private final EventHandler<MouseEvent> mouseEventHandler = this::onMouseClicked;
    private final WeakEventHandler<MouseEvent> weakMouseEventHandler = new WeakEventHandler<>(mouseEventHandler);

    private final ChangeListener<Boolean> filteredListener = (_, _, newValue) -> updateFiltered(newValue);
    private final WeakChangeListener<Boolean> weakFilteredListener = new WeakChangeListener<>(filteredListener);

    private StackPane filterPane;
    private boolean isFiltered;

    /// Creates a new [ExtendedTableColumnHeader] instance.
    ///
    /// @param tableColumnBase
    ///         the [TableColumnBase] where this header will be created from
    public ExtendedTableColumnHeader(TableColumnBase<?, ?> tableColumnBase) {
        super(tableColumnBase);

        TableColumnBase<?, ?> tableColumn = getTableColumn();
        if (tableColumn instanceof FilterableColumn filterableColumn) {
            addEventHandler(MouseEvent.MOUSE_PRESSED, weakMouseEventHandler);

            filterableColumn.filteredProperty().addListener(weakFilteredListener);
            updateFiltered(filterableColumn.filteredProperty().get());
        }
    }

    /// Resizes this `TableColumnHeader`'s column to fit the width of its content.
    public void resizeColumnToFitContent() {
        // When there is no scene we should not do resizing as it will result in an exception and a wrong calculation
        // anyway.
        if (getScene() == null) {
            return;
        }

        if (!getTableColumn().isResizable()) {
            return;
        }

        // Since maxRows can be arbitrary large we only just take 15 rows into account, which is almost always enough.
        super.resizeColumnToFitContent(15);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        positionFilterIcon();
    }

    @Override
    protected void resizeColumnToFitContent(int maxRows) {
        resizeColumnToFitContent();
    }

    private void onMouseClicked(MouseEvent mouseEvent) {
        FilterableColumn filterTableColumn = (FilterableColumn) getTableColumn();

        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            if (filterTableColumn.isFilteringDisabled()) {
                return;
            }

            PopupWindow filterPopup = filterTableColumn.getFilterPopup();
            if (filterPopup == null) {
                return;
            }

            showFilterPopup(filterPopup);
        }
    }

    private void positionFilterIcon() {
        if (!isFiltered) {
            return;
        }

        if (!getChildren().contains(filterPane)) {
            getChildren().add(filterPane);
        }

        double height = snapSpaceY(getHeight() - (snappedTopInset() + snappedBottomInset()));
        double imageWidth = snapSizeX(filterPane.prefWidth(height));
        positionInArea(filterPane, snappedLeftInset(), snappedTopInset(), imageWidth, height, 0, HPos.CENTER,
                VPos.CENTER);

        // First entry is the Label.
        // There is no other way to access it.
        Node label = getChildren().getFirst();
        label.resizeRelocate(snapPositionX(snappedLeftInset() + imageWidth), 0,
                snapSpaceX(label.getLayoutBounds().getWidth() - imageWidth), getHeight());
    }

    private void showFilterPopup(PopupWindow filterPopup) {
        Window window = getScene().getWindow();
        Point2D localToScene = localToScene(0, 0);
        double x = window.getX() + localToScene.getX() + getScene().getX();
        double y = window.getY() + localToScene.getY() + getScene().getY() + getHeight();

        filterPopup.show(getTableSkin().getSkinnable(), x, y);
    }

    private void updateFiltered(boolean isFiltered) {
        if (isFiltered) {
            if (filterPane == null) {
                filterPane = new StackPane();

                StackPane funnel = new StackPane();
                funnel.getStyleClass().add("funnel");
                filterPane.getChildren().add(funnel);
            }
            if (!getStyleClass().contains(FILTERED_STYLE_CLASS)) {
                getStyleClass().add(FILTERED_STYLE_CLASS);
            }
        } else {
            if (filterPane != null) {
                getChildren().remove(filterPane);
            }
            getStyleClass().remove(FILTERED_STYLE_CLASS);
        }
        this.isFiltered = isFiltered;
    }

}
