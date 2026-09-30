package tools.maran.extendedtable.tableview.cell;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

/// [ExtendedTableCell] which shows a [ComboBox] on edit.
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
        setAlignment(Pos.CENTER);
    }

    /// Creates the [ComboBox] which is shown when this cell is edited.
    ///
    /// @return the newly created [ComboBox]
    protected ComboBox<T> createComboBox() {
        ComboBox<T> cbx = new ComboBox<>(items);
        cbx.setSkin(createComboBoxSkin(cbx));
        cbx.getProperties().put("comboBoxRowsToMeasureWidth", 30);

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

        cbx.setOnHidden(_ -> {
            if (isCancelEdit) {
                cancelEdit();
                isCancelEdit = false;
            } else {
                commitCurrentComboBoxValue();
            }
        });

        cbx.setOnMousePressed(_ -> getTableView().getSelectionModel().select(getIndex(), getTableColumn()));

        return cbx;
    }

    /// Sets the read function which transforms the cell value into the text shown in the [ComboBox].
    ///
    /// @param readFunction
    ///         the read function
    public final void setReadFunction(Callback<T, String> readFunction) {
        this.readFunction = readFunction;
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

    private ComboBoxListViewSkin<T> createComboBoxSkin(ComboBox<T> comboBox) {
        ComboBoxListViewSkin<T> skin = new ComboBoxListViewSkin<>(comboBox);

        Node popupContent = skin.getPopupContent();
        popupContent.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                isCancelEdit = true;
            }
        });
        return skin;
    }
}
