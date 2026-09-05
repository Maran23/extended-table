package tools.maran.extendedtable.table.header;

import java.lang.ref.WeakReference;
import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import tools.maran.extendedtable.table.common.ExtendedTable;

/// Component which can be used to show a header above a table.
/// It will synchronize itself with the header text, items and header buttons set in the [ExtendedTable].
///
/// @author Marius Hanl
public class ExtendedTableHeader extends HBox {

    private static final String HEADER_STYLE_CLASS = "header";
    private static final String BUTTON_BOX_STYLE_CLASS = "button-box";

    private Label headerLabel;
    private HBox buttonBox;
    private ExtendedTable<?> extendedTable;

    private final ListChangeListener<Node> headerButtonsChangedListener = _ -> onHeaderButtonsChanged();
    private final InvalidationListener headerTextUpdateListener = _ -> updateCounter();
    private final InvalidationListener itemsInvalidatedListener = _ -> itemsInvalidated();
    private final ListChangeListener<Object> itemsChangedListener = _ -> updateCounter();

    private WeakReference<ObservableList<?>> itemsRef;

    /// Creates a new [ExtendedTableHeader] instance.
    ///
    /// @param extendedTable
    ///         the [ExtendedTable] this header belongs to
    public ExtendedTableHeader(ExtendedTable<?> extendedTable) {
        this.extendedTable = extendedTable;

        getStyleClass().add(HEADER_STYLE_CLASS);

        setUpHeaderLabel();
        setUpButtonBox();

        getChildren().addAll(headerLabel, buttonBox);

        HBox.setHgrow(headerLabel, Priority.ALWAYS);
        HBox.setHgrow(buttonBox, Priority.ALWAYS);

        ObservableList<Node> tableHeaderButtons = extendedTable.getHeaderButtons();
        tableHeaderButtons.addListener(headerButtonsChangedListener);

        extendedTable.headerTextProperty().addListener(headerTextUpdateListener);
        extendedTable.itemsProperty().addListener(itemsInvalidatedListener);

        itemsRef = new WeakReference<>(null);
        itemsInvalidated();

        onHeaderButtonsChanged();
        updateCounter();
    }

    /// This method allows this component to implement any logic necessary to clean up itself.
    /// Calling dispose twice has no effect.
    public final void dispose() {
        if (extendedTable == null) {
            return;
        }

        extendedTable.getHeaderButtons().removeListener(headerButtonsChangedListener);
        extendedTable.headerTextProperty().removeListener(headerTextUpdateListener);
        extendedTable.itemsProperty().removeListener(itemsInvalidatedListener);

        ObservableList<?> oldItems = itemsRef.get();
        if (oldItems != null) {
            oldItems.removeListener(itemsChangedListener);
        }

        buttonBox = null;
        headerLabel = null;
        extendedTable = null;
    }

    @Override
    protected double computeMinHeight(double width) {
        return Math.max(extendedTable.getFixedCellSize(), super.computeMinHeight(width));
    }

    @Override
    protected double computePrefHeight(double width) {
        return Math.max(extendedTable.getFixedCellSize(), super.computePrefHeight(width));
    }

    private void itemsInvalidated() {
        ObservableList<?> oldItems = itemsRef.get();

        if (oldItems != null) {
            oldItems.removeListener(itemsChangedListener);
        }

        ObservableList<?> newItems = extendedTable.getItems();
        if (newItems == null) {
            itemsRef = new WeakReference<>(null);
        } else {
            newItems.addListener(itemsChangedListener);
            itemsRef = new WeakReference<>(newItems);
        }

        updateCounter();
    }

    private void onHeaderButtonsChanged() {
        buttonBox.getChildren().setAll(extendedTable.getHeaderButtons());
    }

    private void setUpButtonBox() {
        buttonBox = new HBox();
        buttonBox.getStyleClass().add(BUTTON_BOX_STYLE_CLASS);
        buttonBox.setMaxHeight(Double.MAX_VALUE);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
    }

    private void setUpHeaderLabel() {
        headerLabel = new Label();
        headerLabel.setMinWidth(Region.USE_PREF_SIZE);
        headerLabel.setMaxWidth(Double.MAX_VALUE);
        headerLabel.setMaxHeight(Double.MAX_VALUE);
        headerLabel.setAlignment(Pos.CENTER_LEFT);
    }

    private void updateCounter() {
        int backingItemsSize = extendedTable.getBackingItems().size();
        int size = 0;

        if (extendedTable.getItems() != null) {
            size = extendedTable.getItems().size();
        }

        String headerText = Objects.toString(extendedTable.getHeaderText(), "");
        headerLabel.setText(String.format("%s (%d/%d)", headerText, size, backingItemsSize));
    }

}
