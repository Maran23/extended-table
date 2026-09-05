package tools.maran.extendedtable.filter.popup;

import javafx.beans.property.BooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListCell;

/// [ListCell] which can be selected via click/drag or a [CheckBox].
///
/// @param <S>
///         the item type
/// @author Marius Hanl
public class SelectableCell<S> extends ListCell<SelectableItem<S>> {

    private final CheckBox checkBox;
    private BooleanProperty currentSelectedProperty;
    private boolean startedDrag;

    SelectableCell() {
        checkBox = new CheckBox();
        checkBox.setAlignment(Pos.TOP_LEFT);

        setOnMousePressed(_ -> selectCheckBox());
        setOnDragDetected(_ -> startDragging());
        setOnMouseDragEntered(_ -> select());
    }

    @Override
    protected void updateItem(SelectableItem<S> item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            String text = item.getItemText();
            setText(text);

            if (currentSelectedProperty != null) {
                checkBox.selectedProperty().unbindBidirectional(currentSelectedProperty);
            }

            currentSelectedProperty = item.selectedProperty();
            checkBox.selectedProperty().bindBidirectional(currentSelectedProperty);

            setGraphic(checkBox);
        }
    }

    private void select() {
        if (isEmpty()) {
            return;
        }

        // We don't want to unselect the item where we just clicked on and started the drag.
        if (startedDrag) {
            startedDrag = false;
            return;
        }

        getListView().getSelectionModel().select(getIndex());
        checkBox.setSelected(!checkBox.isSelected());
    }

    private void selectCheckBox() {
        if (isEmpty()) {
            return;
        }

        checkBox.setSelected(!checkBox.isSelected());
    }

    private void startDragging() {
        if (isEmpty()) {
            return;
        }

        startedDrag = true;
        startFullDrag();
    }
}
