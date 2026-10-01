package tools.maran.extendedtable.filter.popup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import tools.maran.extendedtable.filter.popup.FilterPopupControl.Status;
import tools.maran.extendedtable.filter.popup.strategy.FilterStrategy;
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

    private final InvalidationListener filterStrategyListener = _ -> resetNormalizedItemTexts();
    private final WeakInvalidationListener weakFilterStrategyListener = new WeakInvalidationListener(
            filterStrategyListener);

    private final ChangeListener<String> filterChangeListener = (_, _, newValue) -> filterItems(newValue);
    private final WeakChangeListener<String> weakFilterChangeListener = new WeakChangeListener<>(filterChangeListener);

    private final EventHandler<WindowEvent> windowHiddenListener = _ -> onHide();
    private final WeakEventHandler<WindowEvent> weakWindowHiddenListener = new WeakEventHandler<>(windowHiddenListener);

    private final EventHandler<WindowEvent> windowShownListener = _ -> onShow();
    private final WeakEventHandler<WindowEvent> weakWindowShownListener = new WeakEventHandler<>(windowShownListener);

    private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

    private FilterPopupControl<S> filterPopupControl;
    private FilterBox<SelectableItem<S>> filterBox;

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

        filterPopupControl.comparatorProperty().addListener(weakItemsListener);
        filterPopupControl.filterStrategyProperty().addListener(weakFilterStrategyListener);
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
        filterPopupControl.comparatorProperty().removeListener(weakItemsListener);
        filterPopupControl.filterStrategyProperty().removeListener(weakFilterStrategyListener);
        filterPopupControl = null;

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

        filterBox.getSelectStateCheckBox().setIndeterminate(selectionState == SelectionState.INTERMEDIATE);
        if (selectionState != SelectionState.INTERMEDIATE) {
            filterBox.getSelectStateCheckBox().setSelected(selectionState == SelectionState.ALL);
        }
    }

    private void filterItems(String text) {
        filterItemsImpl(text);

        computeSelectionState();
    }

    private void filterItemsImpl(String text) {
        if (text == null || text.isEmpty()) {
            filterBox.getItemListView().setItems(selectableItems);
            return;
        }

        Predicate<String> matcher = getFilterStrategy().createMatcher(text);

        ObservableList<SelectableItem<S>> filteredItems = selectableItems.stream()
                .filter(selectableItem -> selectableItem.isMatch(matcher))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        filterBox.getItemListView().setItems(filteredItems);
    }

    private FilterStrategy getFilterStrategy() {
        FilterStrategy filterStrategy = getSkinnable().getFilterStrategy();
        if (filterStrategy == null) {
            return FilterStrategy.DEFAULT;
        }

        return filterStrategy;
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

            SelectableItem<S> selectableItem = itemTextMap.computeIfAbsent(itemText,
                    text -> new SelectableItem<>(text, this::normalize, this::onItemSelectionChanged));
            selectableItem.addBackingItem(item);
            itemToSelectableItemMap.put(item, selectableItem);
        }
    }

    private void initFilterBox() {
        filterBox = new FilterBox<>();
        filterBox.minWidthProperty().bind(filterPopupControl.minWidthProperty());
        filterBox.prefWidthProperty().bind(filterPopupControl.prefWidthProperty());
        filterBox.maxWidthProperty().bind(filterPopupControl.maxWidthProperty());
        filterBox.minHeightProperty().bind(filterPopupControl.minHeightProperty());
        filterBox.prefHeightProperty().bind(filterPopupControl.prefHeightProperty());
        filterBox.maxHeightProperty().bind(filterPopupControl.maxHeightProperty());
        filterBox.setOnKeyPressed(this::onFilterKeyPress);

        CheckBox selectStateCbx = filterBox.getSelectStateCheckBox();
        selectStateCbx.setOnAction(_ -> setSelectionState());

        TextField filterTxt = filterBox.getFilterTextField();
        filterTxt.textProperty().addListener(weakFilterChangeListener);
        filterTxt.setOnKeyPressed(this::onTextFieldEnterPress);

        ListView<SelectableItem<S>> itemListView = filterBox.getItemListView();
        itemListView.fixedCellSizeProperty().bind(getSkinnable().fixedCellSizeProperty());
        itemListView.setCellFactory(_ -> new SelectableCell<>());
        itemListView.setOnKeyPressed(this::onListViewKeyPressed);

        filterBox.getResetColumnButton().setOnAction(_ -> hideWithStatus(Status.RESET_COLUMN));
        filterBox.getResetAllButton().setOnAction(_ -> hideWithStatus(Status.RESET_ALL));

        Button applyBtn = filterBox.getApplyButton();
        applyBtn.disableProperty().bind(selectStateCbx.indeterminateProperty().not());
        applyBtn.setOnAction(_ -> hideWithStatus(Status.APPLY));
    }

    private void initItems() {
        itemSize = 0;

        int size = guessUniqueItemsSize(itemToSelectableItemMap.size());
        selectableItems = FXCollections.observableList(new ArrayList<>(size));
        Collection<S> items = getSkinnable().getItems();

        Comparator<S> comparator = filterPopupControl.getComparator();
        if (comparator != null) {
            items = items.stream().sorted(comparator).toList();
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
                selectableItem.clearItems();
                selectableItems.add(selectableItem);
            }
            selectableItem.addItem(item);
            itemSize++;
        }

        filterItems(filterBox.getFilterTextField().getText());
    }

    private String normalize(String text) {
        return getFilterStrategy().normalize(text);
    }

    private void onFilterKeyPress(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();
        if (code.isLetterKey() || code.isDigitKey()) {
            TextField filterTxt = filterBox.getFilterTextField();
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
                newItems.addAll(selectableItem.getBackingItems());
            }
        }

        getSkinnable().setSelectedItems(newItems);
        getSkinnable().setStatus(newStatus);
    }

    private void onItemSelectionChanged(int selectedDelta) {
        selectedCounter += selectedDelta;
        if (!suppressSelectionComputing) {
            computeSelectionState();
        }
    }

    private void onListViewKeyPressed(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();

        switch (code) {
            case ENTER, SPACE -> {
                SelectableItem<S> selectedItem = filterBox.getItemListView().getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    selectedItem.setSelected(!selectedItem.isSelected());
                }
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

        filterBox.getFilterTextField().requestFocus();
    }

    private void onTextFieldEnterPress(KeyEvent keyEvent) {
        KeyCode code = keyEvent.getCode();

        if (code == KeyCode.ENTER) {
            if (keyEvent.isControlDown()) {
                hideWithStatus(Status.APPLY);
                return;
            }
            ListView<SelectableItem<S>> itemListView = filterBox.getItemListView();
            itemListView.getItems().forEach(item -> item.setSelected(true));
            itemListView.refresh();
            updateSelectionIndicatorLbl();
        }
    }

    private void resetNormalizedItemTexts() {
        if (itemToSelectableItemMap != null) {
            itemToSelectableItemMap.values().forEach(SelectableItem::resetNormalizedItemText);
        }
    }

    private void setSelectionState() {
        suppressSelectionComputing = true;
        boolean selected = filterBox.getSelectStateCheckBox().isSelected();
        filterBox.getItemListView().getItems().forEach(item -> item.setSelected(selected));
        suppressSelectionComputing = false;

        computeSelectionState();
    }

    private void updateSelectionIndicatorLbl() {
        filterBox.getSelectionIndicatorLabel()
                .setText(TableI18N.message("count.selected", selectedCounter, getMaxCount()));
    }

    /// Enum representing the current selection state.
    ///
    /// @author Marius Hanl
    private enum SelectionState {
        ALL, NONE, INTERMEDIATE
    }
}
