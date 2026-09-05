package tools.maran.extendedtable.treetableview.column;

/// Implementation of the [GenericTreeTableColumn] with [Double] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class DoubleTreeTableColumn<S> extends GenericTreeTableColumn<S, Double> {

    /// Creates a new [DoubleTreeTableColumn] instance.
    public DoubleTreeTableColumn() {
        super();
    }

    /// Creates a new [DoubleTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public DoubleTreeTableColumn(String text) {
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
