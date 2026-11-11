package tools.maran.extendedtable.tableview.cell;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

import tools.maran.extendedtable.table.common.CellComboBox;

/// [ExtendedTableCell] which always shows a [ComboBox].
/// The [CellComboBox] only shows its text and arrow. The cell has the `combo-box-cell` style class.
/// Every edit, e.g. by click, ENTER, F2 or TAB, opens the popup.
/// Like a text input, a cancel from outside (e.g. TAB to the next cell) commits the current value.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class ComboBoxTableCell<S, T> extends ExtendedTableCell<S, T> {

    private final ObservableList<T> items;

    private Callback<T, String> readFunction;

    private ComboBox<T> comboBox;

    private boolean isCancelEdit;

    /// Creates a new [ComboBoxTableCell] instance.
    ///
    /// @param items
    ///         all items which should be displayed in the [ComboBox]
    public ComboBoxTableCell(ObservableList<T> items) {
        this.items = items;
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        getStyleClass().add("combo-box-cell");
    }

    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }

        if (!isCancelEdit) {
            commitCurrentComboBoxValue();
            return;
        }

        isCancelEdit = false;
        super.cancelEdit();
        comboBox.hide();
    }

    @Override
    public void commitEdit(T newValue) {
        super.commitEdit(newValue);
        comboBox.hide();
    }

    /// Sets the read function which transforms the cell value into the text shown in the [ComboBox].
    ///
    /// @param readFunction
    ///         the read function
    public final void setReadFunction(Callback<T, String> readFunction) {
        this.readFunction = readFunction;
    }

    @Override
    public void startEdit() {
        if (isEditing()) {
            return;
        }

        super.startEdit();

        if (isEditing()) {
            comboBox.requestFocus();
            comboBox.show();
        }
    }

    /// Creates the [ComboBox] which is shown in this cell.
    ///
    /// @return the newly created [ComboBox]
    protected ComboBox<T> createComboBox() {
        ComboBox<T> cbx = new CellComboBox<>(items);

        ListCell<T> buttonCell = new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : readValue(item));
            }
        };
        buttonCell.getStyleClass().setAll("cell-combo-box-text");
        cbx.setButtonCell(buttonCell);

        cbx.setConverter(new StringConverter<>() {

            @Override
            public T fromString(String string) {
                return null;
            }

            @Override
            public String toString(T object) {
                return readValue(object);
            }
        });

        cbx.setOnShowing(_ -> startEdit());

        cbx.setOnHidden(_ -> commitCurrentComboBoxValue());

        cbx.focusedProperty().addListener(_ -> {
            if (!cbx.isFocused() && !cbx.isShowing()) {
                commitCurrentComboBoxValue();
            }
        });

        cbx.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                isCancelEdit = true;
                cancelEdit();
                e.consume();
            } else if (e.getCode() == KeyCode.ENTER) {
                commitCurrentComboBoxValue();
                e.consume();
            }
        });

        cbx.setOnMousePressed(_ -> getTableView().getSelectionModel().select(getIndex(), getTableColumn()));

        return cbx;
    }

    /// Calls the read function of the cell to get the value.
    ///
    /// @param item
    ///         the content of the cell
    /// @return the cell value
    protected String readValue(T item) {
        if (readFunction == null) {
            return null;
        }
        return readFunction.call(item);
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setGraphic(null);
        } else {
            if (comboBox == null) {
                comboBox = createComboBox();
                comboBox.disableProperty().bind(Bindings.not(
                        getTableView().editableProperty().and(getTableColumn().editableProperty())
                                .and(getTableRow().editableProperty()).and(editableProperty())));
            }
            comboBox.getSelectionModel().select(item);

            setGraphic(comboBox);
        }
    }

    private void commitCurrentComboBoxValue() {
        commitEdit(comboBox.getSelectionModel().getSelectedItem());
    }

}
