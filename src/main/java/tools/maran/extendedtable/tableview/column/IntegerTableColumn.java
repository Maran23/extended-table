package tools.maran.extendedtable.tableview.column;

/// Implementation of the [GenericTableColumn] with [Integer] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class IntegerTableColumn<S> extends GenericTableColumn<S, Integer> {

    /// Creates a new [IntegerTableColumn] instance.
    public IntegerTableColumn() {
        super();
    }

    /// Creates a new [IntegerTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public IntegerTableColumn(String text) {
        super(text);
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(value -> value == null ? null : String.valueOf(value));
        setFromStringConverter(this::toInteger);
    }

    private Integer toInteger(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
