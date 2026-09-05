package tools.maran.extendedtable.treetableview.column;

/// Implementation of the [GenericTreeTableColumn] with [Long] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class LongTreeTableColumn<S> extends GenericTreeTableColumn<S, Long> {

    /// Creates a new [LongTreeTableColumn] instance.
    public LongTreeTableColumn() {
        super();
    }

    /// Creates a new [LongTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public LongTreeTableColumn(String text) {
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
