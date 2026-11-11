package tools.maran.extendedtable.tableview.cell;

import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// [GenericTextInputTableCell] which shows a [TextArea] on edit.
/// The [TextArea] has the `cell-text-input` and `cell-text-area` style class instead of the default ones, which themes style, so it fills the cell
/// without a border.
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class GenericTextAreaTableCell<S, T> extends GenericTextInputTableCell<S, T> {

    /// Creates a new [GenericTextAreaTableCell] instance.
    public GenericTextAreaTableCell() {
    }

    @Override
    protected TextArea createTextInput() {
        TextArea textAreaNode = new TextArea();
        textAreaNode.getStyleClass().setAll("cell-text-input", "cell-text-area");
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
