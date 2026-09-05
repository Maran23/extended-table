package tools.maran.extendedtable.treetableview.cell;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// [GenericTextInputTreeTableCell] which shows a [TextField] on edit.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class GenericTextFieldTreeTableCell<S, T> extends GenericTextInputTreeTableCell<S, T> {

    /// Creates a new [GenericTextFieldTreeTableCell] instance.
    public GenericTextFieldTreeTableCell() {
    }

    @Override
    protected TextField createTextInput() {
        TextField textFieldNode = new TextField();

        textFieldNode.setOnAction(event -> {
            commitText(textFieldNode.getText());
            event.consume();
        });

        textFieldNode.focusedProperty().addListener(_ -> {
            if (!textFieldNode.isFocused()) {
                commitText(textFieldNode.getText());
            }
        });

        textFieldNode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelText();
                event.consume();
            }
        });

        return textFieldNode;
    }

}
