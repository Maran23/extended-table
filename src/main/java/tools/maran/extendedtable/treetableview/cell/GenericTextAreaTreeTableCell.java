package tools.maran.extendedtable.treetableview.cell;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// [GenericTextInputTreeTableCell] which shows a [TextArea] on edit.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class GenericTextAreaTreeTableCell<S, T> extends GenericTextInputTreeTableCell<S, T> {

    /// Creates a new [GenericTextAreaTreeTableCell] instance.
    public GenericTextAreaTreeTableCell() {
    }

    @Override
    protected TextArea createTextInput() {
        TextArea textAreaNode = new TextArea();
        textAreaNode.setWrapText(true);

        textAreaNode.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                cancelText();
                event.consume();
            }
        });

        textAreaNode.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                commitText(textAreaNode.getText());
                event.consume();
            }
        });

        textAreaNode.focusedProperty().addListener(_ -> {
            if (!textAreaNode.isFocused()) {
                commitText(textAreaNode.getText());
            }
        });

        return textAreaNode;
    }

}
