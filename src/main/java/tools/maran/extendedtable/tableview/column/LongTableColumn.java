package tools.maran.extendedtable.tableview.column;

/// Implementation of the [GenericTableColumn] with [Long] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class LongTableColumn<S> extends GenericTableColumn<S, Long> {

    /// Creates a new [LongTableColumn] instance.
    public LongTableColumn() {
        super();
    }

    /// Creates a new [LongTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public LongTableColumn(String text) {
        super(text);
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(value -> value == null ? null : String.valueOf(value));
        setFromStringConverter(this::toLong);
    }

    private Long toLong(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        try {
            return Long.valueOf(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
