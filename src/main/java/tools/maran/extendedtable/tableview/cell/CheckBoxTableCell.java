package tools.maran.extendedtable.tableview.cell;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;

/// [ExtendedTableCell] which shows a [CheckBox].
///
/// @param <S>
///         the source type
public class CheckBoxTableCell<S> extends ExtendedTableCell<S, Boolean> {

    private CheckBox checkBox;

    private boolean isInUpdate;

    /// Creates a new [CheckBoxTableCell] instance.
    public CheckBoxTableCell() {
        setAlignment(Pos.CENTER);
    }

    /// Creates the [CheckBox] which is shown in this cell.
    ///
    /// @return the newly created [CheckBox]
    protected CheckBox createCheckBox() {
        CheckBox checkBoxNode = new CheckBox();
        checkBoxNode.selectedProperty().addListener(_ -> startAndCommitEdit(checkBoxNode));

        return checkBoxNode;
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
            }

            isInUpdate = true;
            checkBox.setSelected(item);
            isInUpdate = false;

            checkBox.disableProperty().bind(Bindings.not(
                    getTableView().editableProperty().and(getTableColumn().editableProperty())
                            .and(getTableRow().editableProperty()).and(editableProperty())));

            setGraphic(checkBox);
        }
    }

}
