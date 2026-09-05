package tools.maran.extendedtable.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import tools.maran.extendedtable.filter.FilterPopupControl.Status;
import tools.maran.extendedtable.table.common.TableI18N;

/// [Skin] for the [FilterPopupControl] which creates the layout and logic for the filter.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class FilterPopupControlSkin<S> implements Skin<FilterPopupControl<S>> {

    private final InvalidationListener itemsBackingListener = _ -> initBacking = true;
    private final WeakInvalidationListener weakBackingItemsListener = new WeakInvalidationListener(
            itemsBackingListener);

    private final InvalidationListener itemsListener = _ -> initItems = true;
    private final WeakInvalidationListener weakItemsListener = new WeakInvalidationListener(itemsListener);

    private final ChangeListener<String> filterChangeListener = (_, _, newValue) -> filterItems(newValue);
    private final WeakChangeListener<String> weakFilterChangeListener = new WeakChangeListener<>(filterChangeListener);

    private final EventHandler<WindowEvent> windowHiddenListener = _ -> onHide();
    private final WeakEventHandler<WindowEvent> weakWindowHiddenListener = new WeakEventHandler<>(windowHiddenListener);

    private final EventHandler<WindowEvent> windowShownListener = _ -> onShow();
    private final WeakEventHandler<WindowEvent> weakWindowShownListener = new WeakEventHandler<>(windowShownListener);

    private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

    private FilterPopupControl<S> filterPopupControl;
    private VBox filterBox;
    private CheckBox selectStateCbx;
    private TextField filterTxt;
    private ListView<SelectableItem<S>> itemListView;
    private Label selectionIndicatorLbl;

    private final Callback<S, String> readFunction;

    private ObservableList<SelectableItem<S>> selectableItems;
    private Map<S, SelectableItem<S>> itemToSelectableItemMap;

    private boolean suppressSelectionComputing;
    private boolean initBacking = true;
    private boolean initItems = true;

    private int selectedCounter;
    private int itemSize;
    private Status newStatus;

    /// Creates a new [FilterPopupControlSkin] instance.
    ///
    /// @param filterPopupControl
    ///         the [FilterPopupControl]
    FilterPopupControlSkin(FilterPopupControl<S> filterPopupControl) {
        this.filterPopupControl = filterPopupControl;
        readFunction = filterPopupControl.getReadFunction();

        filterPopupControl.addEventHandler(WindowEvent.WINDOW_SHOWN, weakWindowShownListener);
        filterPopupControl.addEventHandler(WindowEvent.WINDOW_HIDDEN, weakWindowHiddenListener);

        filterPopupControl.getBackingItems().addListener(weakBackingItemsListener);
        filterPopupControl.getItems().addListener(weakItemsListener);

        filterPopupControl.getProperties().remove(FilterPopupControl.REFRESH_ITEM);
        filterPopupControl.getProperties().addListener(propertiesMapListener);

        initFilterBox();
    }

    @Override
    public void dispose() {
        if (filterPopupControl == null) {
            return;
        }

        filterPopupControl.getProperties().removeListener(propertiesMapListener);
        filterPopupControl.removeEventHandler(WindowEvent.WINDOW_SHOWN, weakWindowShownListener);
        filterPopupControl.removeEventHandler(WindowEvent.WINDOW_HIDDEN, weakWindowHiddenListener);
        filterPopupControl.getBackingItems().removeListener(weakBackingItemsListener);
        filterPopupControl.getItems().removeListener(weakItemsListener);
        filterPopupControl = null;

        filterBox.prefWidthProperty().unbind();
        filterBox = null;
    }

    @Override
    public Node getNode() {
        return filterBox;
    }

    @Override
    public FilterPopupControl<S> getSkinnable() {
        return filterPopupControl;
    }

    private void computeSelectionState() {
        updateSelectionIndicatorLbl();

        SelectionState selectionState = getSelectionState();

        selectStateCbx.setIndeterminate(selectionState == SelectionState.INTERMEDIATE);
        if (selectionState != SelectionState.INTERMEDIATE) {
            selectStateCbx.setSelected(selectionState == SelectionState.ALL);
        }
    }

    private void filterItems(String text) {
        filterItemsImpl(text);

        computeSelectionState();
    }

    private void filterItemsImpl(String text) {
        if (text == null || text.isEmpty()) {
            itemListView.setItems(selectableItems);
            return;
        }

        Set<String> splitTokens = FilterUtils.splitByWhiteSpaceExceptQuotes(text);

        ObservableList<SelectableItem<S>> filteredItems = selectableItems.stream()
                .filter(selectableItem -> FilterUtils.contains(splitTokens, selectableItem.getItemText().toLowerCase()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        itemListView.setItems(filteredItems);
    }

    private int getMaxCount() {
        return getSkinnable().getBackingItems().size();
    }

    /// Returns the current [SelectionState].
    ///
    /// The [SelectionState] is calculated using the selected counter and item size.
    /// This is much faster than iterating over the selected items.
    ///
    /// @return the [SelectionState]
    private SelectionState getSelectionState() {
        if (selectedCounter == itemSize) {
            return SelectionState.ALL;
        }
        if (selectedCounter == 0) {
            return SelectionState.NONE;
        }
        return SelectionState.INTERMEDIATE;
    }

    /// We try to estimate how many unique items we will have.
    /// That is, the amount of unique String occurrences of a particular column.
    ///
    /// @param size
    ///         the size
    /// @return the estimated item size
    private int guessUniqueItemsSize(int size) {
        return Math.max(16, size / 8);
    }

    private void hideWithStatus(Status status) {
        newStatus = status;
        getSkinnable().hide();
    }

    private void initBackingItems() {
        ObservableList<S> items = getSkinnable().getBackingItems();
        itemToSelectableItemMap = new IdentityHashMap<>(items.size());

        Map<String, SelectableItem<S>> itemTextMap = HashMap.newHashMap(items.size() / 8);
        for (S item : items) {
            // Null is treated as empty String.
            String itemText = Objects.toString(readFunction.call(item), "");

            SelectableItem<S> selectableItem = itemTextMap.computeIfAbsent(itemText, SelectableItem::new);
            selectableItem.addBackingItem(item);
            itemToSelectableItemMap.put(item, selectableItem);
        }
    }

    private void initCenter() {
        itemListView = new ListView<>();
        itemListView.fixedCellSizeProperty().bind(getSkinnable().fixedCellSizeProperty());
        itemListView.setCellFactory(_ -> new SelectableCell());
        itemListView.setOnKeyPressed(this::onListViewKeyPressed);

        // Let the list view show 10 items with a size of 24 and a bit.
        // If the items are bigger, we will obviously show fewer items, which is a tradeoff we are okay to accept.
        itemListView.setPrefHeight(24 * 10.5);
        filterBox.setPrefWidth(400);
        filterBox.getChildren().add(itemListView);
    }

    private void initFilterBox() {
        filterBox = new VBox();
        filterBox.getStylesheets().add(getClass().getResource("filter-popup.css").toExternalForm());
        filterBox.getStyleClass().add("filter-box");
        filterBox.setOnKeyPressed(this::onFilterKeyPress);

        initHeader();
        initCenter();
        initFooter();
    }

    private void initFooter() {
        selectionIndicatorLbl = new Label();
        filterBox.getChildren().add(selectionIndicatorLbl);

        Button colResetBtn = new Button(TableI18N.message("reset.column"));
        colResetBtn.setMaxWidth(Double.MAX_VALUE);
        colResetBtn.setCursor(Cursor.HAND);
        colResetBtn.setOnAction(_ -> hideWithStatus(Status.RESET_COLUMN));

        Button resetBtn = new Button(TableI18N.message("reset.column.all"));
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setCursor(Cursor.HAND);
        resetBtn.setOnAction(_ -> hideWithStatus(Status.RESET_ALL));

        Button applyBtn = new Button(TableI18N.message("apply"));
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setCursor(Cursor.HAND);
        applyBtn.disableProperty().bind(selectStateCbx.indeterminateProperty().not());
        applyBtn.setOnAction(_ -> hideWithStatus(Status.APPLY));

        HBox filter = new HBox(4, colResetBtn, resetBtn);
        HBox.setHgrow(colResetBtn, Priority.ALWAYS);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);

        VBox footer = new VBox(4, applyBtn, filter);
        filterBox.getChildren().add(footer);
    }

    private void initHeader() {
        selectStateCbx = new CheckBox();
        selectStateCbx.setOnAction(_ -> setSelectionState());

        filterTxt = new TextField();
        filterTxt.textProperty().addListener(weakFilterChangeListener);
        filterTxt.setOnKeyPressed(this::onTextFieldEnterPress);
        HBox.setHgrow(filterTxt, Priority.ALWAYS);

        HBox header = new HBox(4, selectStateCbx, filterTxt);
        header.setAlignment(Pos.CENTER_LEFT);

        filterBox.getChildren().add(header);
    }

    private void initItems() {
        itemSize = 0;

        int size = guessUniqueItemsSize(itemToSelectableItemMap.size());
        selectableItems = FXCollections.observableList(new ArrayList<>(size));
        ObservableList<S> items = getSkinnable().getItems();

        Comparator<S> comparator = filterPopupControl.getComparator();
        if (comparator != null) {
            items = items.stream().sorted(comparator)
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        }

        Set<SelectableItem<S>> duplicates = HashSet.newHashSet(size);
        for (S item : items) {
            SelectableItem<S> selectableItem = itemToSelectableItemMap.get(item);

            if (selectableItem == null) {
                // All items should also be inside the backing items.
                throw new IllegalStateException("Item is not inside the backing items: " + item);
            }

            boolean isNew = duplicates.add(selectableItem);
            if (isNew) {
                selectableItem.items = new ArrayList<>();
                selectableItems.add(selectableItem);
            }
            selectableItem.addItem(item);
            itemSize++;
        }

        filterItems(filterTxt.getText());
    }

    private void onFilterKeyPress(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code.isLetterKey() || code.isDigitKey()) {
            filterTxt.requestFocus();
            filterTxt.positionCaret(filterTxt.getLength());
            return;
        }

        if (code == KeyCode.ESCAPE) {
            getSkinnable().hide();
        }
    }

    private void onHide() {
        if (Status.RESET_ALL == newStatus || Status.RESET_COLUMN == newStatus) {
            getSkinnable().setStatus(newStatus);
            return;
        }

        if (newStatus == null || selectedCounter == itemSize) {
            getSkinnable().setStatus(Status.UNCHANGED);
            return;
        }

        ObservableList<S> newItems = FXCollections.observableList(new ArrayList<>(selectedCounter));
        for (SelectableItem<S> selectableItem : selectableItems) {
            if (selectableItem.isSelected()) {
                newItems.addAll(selectableItem.backingItems);
            }
        }

        getSkinnable().setSelectedItems(newItems);
        getSkinnable().setStatus(newStatus);
    }

    private void onListViewKeyPressed(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();

        switch (code) {
            case ENTER, SPACE -> {
                SelectableItem<S> selectedItem = itemListView.getSelectionModel().getSelectedItem();
                selectedItem.setSelected(!selectedItem.isSelected());
            }
            case ESCAPE -> getSkinnable().hide();
            default -> {
                // noop
            }
        }
    }

    private void onPropertiesAdded(Change<?, ?> change) {
        if (change.wasAdded() && FilterPopupControl.REFRESH_ITEM.equals(change.getKey())) {
            getSkinnable().getProperties().remove(FilterPopupControl.REFRESH_ITEM);
            // Rebuilt lazily when shown the next time, so any amount of refreshed items is only rebuilt once.
            initBacking = true;
            initItems = true;
        }
    }

    private void onShow() {
        // Unselect everything
        if (itemToSelectableItemMap != null) {
            itemToSelectableItemMap.values().forEach(item -> item.setSelected(false));
        }

        newStatus = null;

        if (initBacking) {
            initBackingItems();

            selectedCounter = 0;
            initBacking = false;
            // The selectable items were recreated, so the items need to reference the new ones.
            initItems = true;
        }

        if (initItems) {
            initItems();
            initItems = false;
        }

        filterTxt.requestFocus();
    }

    private void onTextFieldEnterPress(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();

        if (code == KeyCode.ENTER) {
            if (keyEvent.isControlDown()) {
                hideWithStatus(Status.APPLY);
                return;
            }
            itemListView.getItems().forEach(item -> item.setSelected(true));
            itemListView.refresh();
            updateSelectionIndicatorLbl();
        }
    }

    private void setSelectionState() {
        suppressSelectionComputing = true;
        itemListView.getItems().forEach(item -> item.setSelected(selectStateCbx.isSelected()));
        suppressSelectionComputing = false;

        computeSelectionState();
    }

    private void updateSelectionIndicatorLbl() {
        selectionIndicatorLbl.setText(TableI18N.message("count.selected", selectedCounter, getMaxCount()));
    }

    /// [ListCell] which can be selected via click/drag or a [CheckBox].
    ///
    /// @author Marius Hanl
    public class SelectableCell extends ListCell<SelectableItem<S>> {

        private final CheckBox checkBox;
        private BooleanProperty currentSelectedProperty;
        private boolean startedDrag;

        SelectableCell() {
            checkBox = new CheckBox();
            checkBox.setAlignment(Pos.TOP_LEFT);

            setOnMousePressed(_ -> selectCheckBox());
            setOnDragDetected(_ -> startDragging());
            setOnMouseDragEntered(_ -> select());
        }

        @Override
        protected void updateItem(SelectableItem<S> item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                String text = item.getItemText();
                setText(text);

                if (currentSelectedProperty != null) {
                    checkBox.selectedProperty().unbindBidirectional(currentSelectedProperty);
                }

                currentSelectedProperty = item.selectedProperty();
                checkBox.selectedProperty().bindBidirectional(currentSelectedProperty);

                setGraphic(checkBox);
            }
        }

        private void select() {
            if (isEmpty()) {
                return;
            }

            // We don't want to unselect the item where we just clicked on and started the drag.
            if (startedDrag) {
                startedDrag = false;
                return;
            }

            getListView().getSelectionModel().select(getIndex());
            checkBox.setSelected(!checkBox.isSelected());
        }

        private void selectCheckBox() {
            if (isEmpty()) {
                return;
            }

            checkBox.setSelected(!checkBox.isSelected());
        }

        private void startDragging() {
            if (isEmpty()) {
                return;
            }

            startedDrag = true;
            startFullDrag();
        }
    }

    /// Wrapper class for an amount of items with an additional selected property.
    ///
    /// @param <T>
    ///         the item type
    /// @author Marius Hanl
    public class SelectableItem<T> {

        private BooleanProperty selected;
        private final String itemText;
        private final List<T> backingItems;
        private List<T> items;

        SelectableItem(String itemText) {
            this.itemText = itemText;

            backingItems = new ArrayList<>();
        }

        /// Adds a backing item which this [SelectableItem] represents.
        ///
        /// @param item
        ///         the item
        public void addBackingItem(T item) {
            backingItems.add(item);
        }

        /// Adds an item which this [SelectableItem] **CURRENTLY** represents.
        /// Can be the same items as the backing items or less, when filtered down.
        ///
        /// @param item
        ///         the item
        public void addItem(T item) {
            items.add(item);
        }

        /// Returns the item text.
        ///
        /// @return the item text
        public final String getItemText() {
            return itemText;
        }

        /// Returns true when selected, false otherwise.
        ///
        /// @return true when selected, false otherwise
        public final boolean isSelected() {
            return selected != null && selectedProperty().get();
        }

        /// Returns the selected property.
        ///
        /// @return the selected property
        public final BooleanProperty selectedProperty() {
            if (selected == null) {
                selected = new SimpleBooleanProperty(this, "selected", false);
                selected.addListener((_, _, isSelected) -> updateCounterAndSelectionState(isSelected));
            }
            return selected;
        }

        /// Sets the selected property.
        ///
        /// @param value
        ///         the selected flag
        public final void setSelected(boolean value) {
            if (selected == null && !value) {
                return;
            }

            selectedProperty().set(value);
        }

        private void updateCounterAndSelectionState(boolean isSelected) {
            int size = items.size();
            if (size == 0) {
                return;
            }

            if (isSelected) {
                selectedCounter += size;
            } else {
                selectedCounter -= size;
            }
            if (suppressSelectionComputing) {
                return;
            }

            computeSelectionState();
        }
    }

    /// Enum representing the current selection state.
    ///
    /// @author Marius Hanl
    private enum SelectionState {
        ALL, NONE, INTERMEDIATE
    }
}
