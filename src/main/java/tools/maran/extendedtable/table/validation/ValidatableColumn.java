package tools.maran.extendedtable.table.validation;

/// Defines a contract which columns that wish to be validated need to fulfill.
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public interface ValidatableColumn<S> {

    /// Validates the given item.
    ///
    /// @param item
    ///         the item
    /// @return true, if valid, false otherwise
    boolean validate(S item);

}
