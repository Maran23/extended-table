package tools.maran.extendedtable.table.common;

import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/// [ComboBox] which is shown inside a cell. The cell acts as the box, so it has the `cell-combo-box` style class
/// instead of the default ones, which themes do not style, and is not focus traversable.
///
/// The popup gets all key events while showing, so TAB and ESCAPE hide the popup and are passed on to the [ComboBox],
/// where the cell and table can handle them, e.g. to move the edit to the next cell. Hiding the popup for them fires
/// no [#ON_HIDDEN] event, as the receiver of the key decides what happens.
///
/// @param <T>
///         the item type
/// @author Marius Hanl
public class CellComboBox<T> extends ComboBox<T> {

    /// Creates a new [CellComboBox] instance.
    ///
    /// @param items
    ///         the items to show in the popup
    public CellComboBox(ObservableList<T> items) {
        super(items);
        getStyleClass().setAll("cell-combo-box");
        setFocusTraversable(false);
        getProperties().put("comboBoxRowsToMeasureWidth", 30);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new CellComboBoxSkin<>(this);
    }

    /// [ComboBoxListViewSkin] for the [CellComboBox], which passes TAB and ESCAPE from the popup on to the [ComboBox].
    private static final class CellComboBoxSkin<T> extends ComboBoxListViewSkin<T> {

        private final EventHandler<Event> hiddenFilter = this::onHidden;

        private boolean isPassingKey;

        CellComboBoxSkin(CellComboBox<T> comboBox) {
            super(comboBox);

            comboBox.addEventFilter(ON_HIDDEN, hiddenFilter);
            getPopupContent().addEventFilter(KeyEvent.KEY_PRESSED, this::onPopupKeyPressed);
        }

        @Override
        public void dispose() {
            if (getSkinnable() == null) {
                return;
            }

            getSkinnable().removeEventFilter(ON_HIDDEN, hiddenFilter);
            super.dispose();
        }

        private void onHidden(Event event) {
            if (isPassingKey) {
                event.consume();
            }
        }

        private void onPopupKeyPressed(KeyEvent event) {
            if (event.getCode() != KeyCode.TAB && event.getCode() != KeyCode.ESCAPE) {
                return;
            }

            // Hidden first, as key events are redirected to the popup while it is showing.
            ComboBoxBase<T> comboBox = getSkinnable();
            isPassingKey = true;
            comboBox.hide();
            isPassingKey = false;
            Event.fireEvent(comboBox, event.copyFor(comboBox, comboBox));
            event.consume();
        }
    }
}
