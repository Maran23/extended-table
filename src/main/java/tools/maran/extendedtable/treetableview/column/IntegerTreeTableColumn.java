package tools.maran.extendedtable.treetableview.column;

/// Implementation of the [GenericTreeTableColumn] with [Integer] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class IntegerTreeTableColumn<S> extends GenericTreeTableColumn<S, Integer> {

    /// Creates a new [IntegerTreeTableColumn] instance.
    public IntegerTreeTableColumn() {
        super();
    }

    /// Creates a new [IntegerTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public IntegerTreeTableColumn(String text) {
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
