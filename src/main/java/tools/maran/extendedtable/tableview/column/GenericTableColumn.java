package tools.maran.extendedtable.tableview.column;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import tools.maran.extendedtable.tableview.cell.ExtendedTableCell;
import tools.maran.extendedtable.tableview.cell.GenericTextFieldTableCell;

/// Generic implementation of the [AbstractFilterTableColumn].
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
/// @author Marius Hanl
public class GenericTableColumn<S, T> extends AbstractFilterTableColumn<S, T> {

    /// Shared property across all cells of this column which holds the current cell value.
    ///
    /// @implNote In order to avoid creating many property objects, this object is created once and shared
    /// across all cells.
    /// This property does not allow any listener to improve performance as otherwise JavaFX will add/remove listeners
    /// which are not really needed.
    /// Note: All this is possible as we do not fully use the table concept of JavaFX with regard to properties.
    private final ObjectProperty<T> cellValue = new SimpleObjectProperty<>() {

        @Override
        public void addListener(ChangeListener<? super T> listener) {
            // We do not want JavaFX to install their listener.
        }

        @Override
        public void addListener(InvalidationListener listener) {
            // We do not want JavaFX to install their listener.
        }

        @Override
        public void removeListener(ChangeListener<? super T> listener) {
            // No listeners are allowed, so we can just save JavaFX from making this call.
        }

        @Override
        public void removeListener(InvalidationListener listener) {
            // No listeners are allowed, so we can just save JavaFX from making this call.
        }

        @Override
        protected void fireValueChangedEvent() {
            // No listeners are allowed, so we can just save JavaFX from making this call.
        }
    };

    /// Creates a new [GenericTableColumn] instance.
    public GenericTableColumn() {
        super();
    }

    /// Creates a new [GenericTableColumn] instance.
    ///
    /// @param text
    ///         the text
    public GenericTableColumn(String text) {
        super(text);
    }

    /// Creates the [ExtendedTableCell] which is used for the cells of this column.
    ///
    /// @return the newly created [ExtendedTableCell]
    protected ExtendedTableCell<S, T> createTableCell() {
        GenericTextFieldTableCell<S, T> cell = new GenericTextFieldTableCell<>();
        cell.setReadFunction(this::convertToString);
        cell.setWriteFunction(this::convertFromString);
        return cell;
    }

    @Override
    protected void init() {
        setCellValueFactory(this::createProperty);
        setCellFactory(_ -> createTableCell());
    }

    private ObservableValue<T> createProperty(CellDataFeatures<S, T> cellData) {
        T value = readValue(cellData.getValue());
        cellValue.set(value);

        return cellValue;
    }

}
