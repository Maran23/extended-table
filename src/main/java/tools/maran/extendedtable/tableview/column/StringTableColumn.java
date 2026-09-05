package tools.maran.extendedtable.tableview.column;

/// Implementation of the [GenericTableColumn] with [String] as the output type.
///
/// @param <S>
///         the source type
/// @author Marius Hanl
public class StringTableColumn<S> extends GenericTableColumn<S, String> {

    /// Creates a new [StringTableColumn] instance.
    public StringTableColumn() {
        super();
    }

    /// Creates a new [StringTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public StringTableColumn(String text) {
        super(text);
    }

    @Override
    protected void init() {
        super.init();

        setToStringConverter(string -> string);
        setFromStringConverter(string -> string);
    }
}
