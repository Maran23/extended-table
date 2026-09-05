package tools.maran.extendedtable.filter.popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import tools.maran.extendedtable.filter.popup.strategy.FilterStrategy;

/// Wrapper class for an amount of items with an additional selected property.
///
/// @param <T>
///         the item type
/// @author Marius Hanl
public class SelectableItem<T> {

    private final String itemText;
    private final List<T> backingItems;
    private final List<T> items;
    private final UnaryOperator<String> normalizer;
    private final IntConsumer selectionListener;

    private BooleanProperty selected;
    private String normalizedItemText;

    /// Creates a new [SelectableItem] instance.
    ///
    /// @param itemText
    ///         the item text
    /// @param normalizer
    ///         normalizes the item text for matching, see [FilterStrategy#normalize(String)]
    /// @param selectionListener
    ///         called with the amount of items that were selected (positive) or unselected (negative)
    SelectableItem(String itemText, UnaryOperator<String> normalizer, IntConsumer selectionListener) {
        this.itemText = itemText;
        this.normalizer = normalizer;
        this.selectionListener = selectionListener;

        backingItems = new ArrayList<>();
        items = new ArrayList<>();
    }

    /// Adds a backing item which this [SelectableItem] represents.
    ///
    /// @param item
    ///         the item
    public void addBackingItem(T item) {
        backingItems.add(item);
    }

    /// Adds an item which this [SelectableItem] currently represents.
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

    /// Returns true when this item matches against the matcher, false otherwise.
    ///
    /// @param matcher
    ///         the matcher, to be used to evaluate against.
    /// @return true when his item matches, false otherwise
    public boolean isMatch(Predicate<String> matcher) {
        return matcher.test(getNormalizedItemText());
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
            selected = new SimpleBooleanProperty(this, "selected") {
                @Override
                protected void invalidated() {
                    notifySelectionListener(get());
                }
            };
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

    void clearItems() {
        items.clear();
    }

    List<T> getBackingItems() {
        return backingItems;
    }

    void resetNormalizedItemText() {
        normalizedItemText = null;
    }

    private String getNormalizedItemText() {
        if (normalizedItemText == null) {
            normalizedItemText = Objects.toString(normalizer.apply(itemText), "");
        }
        return normalizedItemText;
    }

    private void notifySelectionListener(boolean isSelected) {
        int size = items.size();
        if (size == 0) {
            return;
        }

        selectionListener.accept(isSelected ? size : -size);
    }
}
