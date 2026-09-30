package tools.maran.extendedtable.treetableview.cell;

import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;

import tools.maran.extendedtable.tableview.cell.CheckBoxTableCell;

/// [ExtendedTreeTableCell] which shows a [CheckBox].
///
/// @param <S>
///         the source type
public class CheckBoxTreeTableCell<S> extends ExtendedTreeTableCell<S, Boolean> {

    private CheckBox checkBox;

    private boolean isInUpdate;

    /// Creates a new [CheckBoxTableCell] instance.
    public CheckBoxTreeTableCell() {
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
                        getTreeTableView().editableProperty().and(getTableColumn().editableProperty())
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
