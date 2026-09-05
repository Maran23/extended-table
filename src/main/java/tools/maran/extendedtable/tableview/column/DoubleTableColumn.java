package tools.maran.extendedtable.tableview.column;

/// Implementation of the [GenericTableColumn] with [Double] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class DoubleTableColumn<S> extends GenericTableColumn<S, Double> {

    /// Creates a new [DoubleTableColumn] instance.
    public DoubleTableColumn() {
        super();
    }

    /// Creates a new [DoubleTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public DoubleTableColumn(String text) {
        super(text);
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(value -> value == null ? null : String.valueOf(value));
        setFromStringConverter(this::toDouble);
    }

    private Double toDouble(String string) {
        if (string == null || string.isEmpty()) {
            return null;
        }

        try {
            return Double.valueOf(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
