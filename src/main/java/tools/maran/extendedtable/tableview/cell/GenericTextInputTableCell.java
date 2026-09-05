package tools.maran.extendedtable.tableview.cell;

import javafx.scene.control.TextInputControl;
import javafx.util.Callback;

/// [ExtendedTableCell] which shows a [TextInputControl] on edit.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public abstract class GenericTextInputTableCell<S, T> extends ExtendedTableCell<S, T> {

    private TextInputControl textInputControl;

    private Callback<T, String> readFunction;
    private Callback<String, T> writeFunction;

    private boolean isCancelEdit;

    /// Creates a new [GenericTextInputTableCell] instance.
    protected GenericTextInputTableCell() {
        super();
    }

    @Override
    public void cancelEdit() {
        if (!isEditing()) {
            return;
        }

        // Instead of cancelling, we commit the text instead.
        if (!isCancelEdit) {
            commitText(textInputControl.getText());

            // If the commit did not work, we pass down and cancel instead.
            if (!isEditing()) {
                return;
            }
        }

        isCancelEdit = false;

        // Reset graphic before cancelling, so if updateItem(..) is called, the state is already reset.
        resetGraphic();

        super.cancelEdit();
    }

    @Override
    public void commitEdit(T newValue) {
        if (!isEditing()) {
            return;
        }

        // Reset graphic before committing, so if updateItem(..) is called, the state is already reset.
        resetGraphic();

        super.commitEdit(newValue);
    }

    /// Sets the read function which transforms the cell value into the text shown in the [TextInputControl].
    ///
    /// @param readFunction
    ///         the read function
    public final void setReadFunction(Callback<T, String> readFunction) {
        this.readFunction = readFunction;
    }

    /// Sets the write function which transforms the committed text back into the cell value.
    ///
    /// @param writeFunction
    ///         the write function
    public final void setWriteFunction(Callback<String, T> writeFunction) {
        this.writeFunction = writeFunction;
    }

    @Override
    public void startEdit() {
        if (isEditing()) {
            return;
        }

        super.startEdit();

        if (!isEditing()) {
            return;
        }

        if (textInputControl == null) {
            textInputControl = createTextInput();
        }

        textInputControl.setText(getText());
        textInputControl.selectAll();

        setText(null);
        setGraphic(textInputControl);

        textInputControl.requestFocus();
    }

    /// Cancels the current edit and discards the text of the [TextInputControl].
    protected void cancelText() {
        isCancelEdit = true;

        cancelEdit();
    }

    /// Commits the given text by converting it with the write function and committing the result.
    /// Does nothing when no write function is set.
    ///
    /// @param text
    ///         the text to commit
    protected void commitText(String text) {
        if (writeFunction == null) {
            return;
        }

        commitEdit(writeValue(text));
    }

    /// Creates the [TextInputControl] which is shown when this cell is edited.
    ///
    /// @return the newly created [TextInputControl]
    protected abstract TextInputControl createTextInput();

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
            setText(null);
        } else {
            if (readFunction != null) {
                setText(readValue(item));
            }
        }
    }

    /// Calls the write function of the cell to set the value.
    ///
    /// @param text
    ///         the content of the cell
    /// @return the cell value
    protected T writeValue(String text) {
        if (writeFunction == null) {
            return null;
        }
        return writeFunction.call(text);
    }

    private void resetGraphic() {
        if (getGraphic() == textInputControl) {
            setGraphic(null);
        }
    }

}
