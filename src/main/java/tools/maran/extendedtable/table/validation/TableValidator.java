package tools.maran.extendedtable.table.validation;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumnBase;

import tools.maran.extendedtable.table.common.ExtendedTable;

/// Validator for an [ExtendedTable].
/// The [TableValidator] is built on heavy caching so that a validation may only be triggered once for an
/// item and column unless otherwise specified.
/// [#evaluateValidity()] is therefore lightweight but should still be only triggered when needed.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class TableValidator<S> {

    private final Map<S, ItemValidationResult<S>> itemValidationResultMap;
    private final Collection<? extends TableColumnBase<S, ?>> columns;

    private ReadOnlyBooleanWrapper valid;

    /// Creates a new [TableValidator] instance and validates the given items right away.
    ///
    /// @param columns
    ///         the columns which hold the validators
    /// @param items
    ///         the items to validate
    public TableValidator(ObservableList<? extends TableColumnBase<S, ?>> columns, Collection<S> items) {
        this.columns = columns;

        itemValidationResultMap = new IdentityHashMap<>(items.size());

        setAll(items);
        evaluateValidity();
    }

    /// Evaluates the validity based of the results and updates the [#validProperty()] accordingly.
    public void evaluateValidity() {
        if (itemValidationResultMap.isEmpty()) {
            setValid(true);
            return;
        }

        setValid(itemValidationResultMap.values().stream().allMatch(ItemValidationResult::isValid));
    }

    /// Returns whether the item is valid.
    ///
    /// @param item
    ///         the item
    /// @return true when valid, false otherwise
    public boolean isValid(S item) {
        ItemValidationResult<S> result = itemValidationResultMap.get(item);
        if (result == null) {
            // This can happen when the cell is e.g. updated or when the autosizing happens.
            // We can ignore this, as the validation will catch up and the cell is updated accordingly.
            return true;
        }

        return result.isValid();
    }

    /// Returns whether the item in the given column is valid.
    ///
    /// @param item
    ///         the item
    /// @param tableColumn
    ///         the [TableColumnBase]
    /// @return true when valid, false otherwise
    public boolean isValid(S item, TableColumnBase<S, ?> tableColumn) {
        ItemValidationResult<S> result = itemValidationResultMap.get(item);
        if (result == null) {
            // This can happen when the cell is e.g. updated or when the autosizing happens.
            // We can ignore this, as the validation will catch up and the cell is updated accordingly.
            return true;
        }

        return result.isValid(tableColumn);
    }

    public final boolean isValid() {
        return validProperty().get();
    }

    /// Revalidates the given items for every column.
    ///
    /// @param items
    ///         the items to revalidate
    public void revalidate(Collection<? extends S> items) {
        for (S item : items) {
            revalidate(item);
        }
    }

    /// Revalidates the given item for every column.
    ///
    /// @param item
    ///         the item to revalidate
    public void revalidate(S item) {
        for (TableColumnBase<S, ?> column : columns) {
            evaluateValidation(item, column);
        }
    }

    /// Sets all items.
    ///
    /// @param items
    ///         the items
    public void setAll(Collection<? extends S> items) {
        itemValidationResultMap.clear();

        revalidate(items);
    }

    /// Returns the valid property.
    ///
    /// @return the valid property
    public final ReadOnlyBooleanProperty validProperty() {
        return validPropertyImpl().getReadOnlyProperty();
    }

    private void evaluateValidation(S item, TableColumnBase<S, ?> column) {
        if (column instanceof ValidatableColumn) {
            ValidatableColumn<S> validatableColumn = (ValidatableColumn<S>) column;
            boolean isValid = validatableColumn.validate(item);

            itemValidationResultMap.computeIfAbsent(item, _ -> new ItemValidationResult<>()).putResult(column, isValid);
        }
    }

    private void setValid(boolean value) {
        validPropertyImpl().set(value);
    }

    private ReadOnlyBooleanWrapper validPropertyImpl() {
        if (valid == null) {
            valid = new ReadOnlyBooleanWrapper(this, "valid", true);
        }
        return valid;
    }

    /// Result for a validation (of all [TableColumnBase]) of an item.
    ///
    /// @param columnResultMap the map from column to the result
    /// @param <S>
    ///         the item type
    private record ItemValidationResult<S>(WeakHashMap<TableColumnBase<S, ?>, Boolean> columnResultMap) {

        ItemValidationResult() {
            this(new WeakHashMap<>());
        }

        public boolean isValid() {
            return columnResultMap.values().stream().allMatch(b -> b);
        }

        public boolean isValid(TableColumnBase<S, ?> tableColumn) {
            return columnResultMap.getOrDefault(tableColumn, true);
        }

        public void putResult(TableColumnBase<S, ?> column, boolean isValid) {
            columnResultMap.put(column, isValid);
        }

    }
}
