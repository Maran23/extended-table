package tools.maran.extendedtable.treetableview.column;

/// Implementation of the [GenericTreeTableColumn] with [String] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class StringTreeTableColumn<S> extends GenericTreeTableColumn<S, String> {

    /// Creates a new [StringTreeTableColumn] instance.
    public StringTreeTableColumn() {
        super();
    }

    /// Creates a new [StringTreeTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public StringTreeTableColumn(String text) {
        super(text);
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(string -> string);
        setFromStringConverter(string -> string);
    }

}
