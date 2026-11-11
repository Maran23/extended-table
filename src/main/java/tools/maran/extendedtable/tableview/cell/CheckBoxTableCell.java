package tools.maran.extendedtable.tableview.cell;

import javafx.beans.binding.Bindings;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/// [ExtendedTableCell] which shows a [CheckBox].
///
/// @param <S>
///         the source type
public class CheckBoxTableCell<S> extends ExtendedTableCell<S, Boolean> {

    private CheckBox checkBox;

    private boolean isInUpdate;

    /// Creates a new [CheckBoxTableCell] instance.
    public CheckBoxTableCell() {
        getStyleClass().add("check-box-table-cell");
    }

    /// Creates the [CheckBox] which is shown in this cell.
    ///
    /// @return the newly created [CheckBox]
    protected CheckBox createCheckBox() {
        CheckBox checkBoxNode = new CheckBox();
        checkBoxNode.setFocusTraversable(false);
        checkBoxNode.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> {
            getTableView().requestFocus();
            getTableView().getSelectionModel().select(getIndex(), getTableColumn());
        });
        checkBoxNode.selectedProperty().addListener(_ -> startAndCommitEdit(checkBoxNode));
        checkBoxNode.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                checkBoxNode.fire();
                e.consume();
            }
        });

        return checkBoxNode;
    }

    /// Focuses the [CheckBox], so it can be toggled with ENTER or SPACE.
    @Override
    public void startEdit() {
        super.startEdit();

        if (isEditing()) {
            checkBox.requestFocus();
        }
    }

    @Override
    protected void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            setEditable(false);
        } else {
            setEditable(true);

            if (checkBox == null) {
                checkBox = createCheckBox();
                checkBox.disableProperty().bind(Bindings.not(
                        getTableView().editableProperty().and(getTableColumn().editableProperty())
                                .and(getTableRow().editableProperty()).and(editableProperty())));
            }

            isInUpdate = true;
            checkBox.setSelected(item);
            isInUpdate = false;

            setGraphic(checkBox);
        }
    }

    private void startAndCommitEdit(CheckBox checkBoxNode) {
        if (isInUpdate) {
            return;
        }

        boolean isSelected = checkBoxNode.isSelected();

        // startEdit() will call updateItem(), which will update the checkbox again,
        // so we save the previous selected flag.
        startEdit();
        commitEdit(isSelected);
    }
}
