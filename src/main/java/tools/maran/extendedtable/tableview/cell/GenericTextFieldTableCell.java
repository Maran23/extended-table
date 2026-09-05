package tools.maran.extendedtable.tableview.cell;

import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// [GenericTextInputTableCell] which shows a [TextField] on edit.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class GenericTextFieldTableCell<S, T> extends GenericTextInputTableCell<S, T> {

    /// Creates a new [GenericTextFieldTableCell] instance.
    public GenericTextFieldTableCell() {
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
