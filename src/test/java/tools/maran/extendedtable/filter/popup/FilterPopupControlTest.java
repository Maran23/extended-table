package tools.maran.extendedtable.filter.popup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.maran.extendedtable.JavaFxTest;
import tools.maran.extendedtable.filter.popup.FilterPopupControl.Status;

/// Tests the [FilterPopupControl].
///
/// @author Marius Hanl
class FilterPopupControlTest extends JavaFxTest {

    @DisplayName("CTRL + ENTER applies the filter, also in the item list")
    @Test
    void testCtrlEnterApplies() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = createPopupWithItems();
            show(popup);

            ListView<?> listView = getListView(popup);
            SelectableItem<?> selectableItem = (SelectableItem<?>) listView.getItems().getFirst();
            selectableItem.setSelected(true);
            listView.getSelectionModel().select(0);

            press(listView, KeyCode.ENTER, true);

            assertEquals(Status.APPLY, popup.getStatus());
        });
    }

    @DisplayName("CTRL + ENTER does nothing while the apply button is disabled")
    @Test
    void testCtrlEnterDoesNothingWhenApplyIsDisabled() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = createPopupWithItems();
            show(popup);

            press(getListView(popup), KeyCode.ENTER, true);

            assertTrue(popup.isShowing());
        });
    }

    @DisplayName("Without a pref width, the popup has the default width from the CSS")
    @Test
    void testDefaultWidth() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            show(popup);

            // The list view pref width plus the padding of the filter box.
            double expectedWidth = Font.getDefault().getSize() * (33.333333 + 2 * 0.333333);
            assertEquals(expectedWidth, getFilterBox(popup).getWidth(), 1);
        });
    }

    @DisplayName("ENTER in the filter text field selects all items, another ENTER deselects them")
    @Test
    void testEnterTogglesTheSelection() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = createPopupWithItems();
            show(popup);

            ListView<?> listView = getListView(popup);
            SelectableItem<?> selectableItem = (SelectableItem<?>) listView.getItems().getFirst();
            selectableItem.setSelected(true);
            TextField filterTextField = (TextField) getFilterBox(popup).lookup(".text-field");

            press(filterTextField, KeyCode.ENTER, false);
            assertTrue(listView.getItems().stream().allMatch(item -> ((SelectableItem<?>) item).isSelected()));

            press(filterTextField, KeyCode.ENTER, false);
            assertTrue(listView.getItems().stream().noneMatch(item -> ((SelectableItem<?>) item).isSelected()));
        });
    }

    @DisplayName("ENTER, TAB and ESCAPE in the popup are not redirected to the owner")
    @ParameterizedTest
    @EnumSource(value = KeyCode.class, names = {"ENTER", "TAB", "ESCAPE"})
    void testKeyIsNotRedirectedToTheOwner(KeyCode code) {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = createPopupWithItems();
            Label owner = show(popup);

            AtomicBoolean ownerPressed = new AtomicBoolean();
            owner.setOnKeyPressed(_ -> ownerPressed.set(true));

            press(owner, code, false);

            assertFalse(ownerPressed.get());
        });
    }

    @DisplayName("The pref width and pref height of the popup are used by the filter box")
    @Test
    void testPrefSize() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            popup.setPrefWidth(1600);
            popup.setPrefHeight(500);
            show(popup);

            Region filterBox = getFilterBox(popup);
            assertEquals(1600, filterBox.getWidth());
            assertEquals(500, filterBox.getHeight());
        });
    }

    @DisplayName("TAB in the filter text field moves the focus inside the popup")
    @Test
    void testTabMovesTheFocusInsideThePopup() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            show(popup);

            TextField filterTextField = (TextField) getFilterBox(popup).lookup(".text-field");
            filterTextField.requestFocus();

            press(filterTextField, KeyCode.TAB, false);

            Node focusOwner = filterTextField.getScene().getFocusOwner();
            assertNotSame(filterTextField, focusOwner);
        });
    }

    private static FilterPopupControl<String> createPopupWithItems() {
        FilterPopupControl<String> popup = new FilterPopupControl<>();
        List<String> items = List.of("A", "B");
        popup.setBackingItems(items);
        popup.setItems(items);
        return popup;
    }

    private static Region getFilterBox(FilterPopupControl<String> popup) {
        return (Region) popup.getSkin().getNode();
    }

    private static ListView<?> getListView(FilterPopupControl<String> popup) {
        return (ListView<?>) getFilterBox(popup).lookup(".list-view");
    }

    private static void press(Node node, KeyCode code, boolean control) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false));
    }

    private static Label show(FilterPopupControl<String> popup) {
        popup.setReadFunction(String::valueOf);
        Label owner = new Label();
        showInStage(owner, 200, 200);
        popup.show(owner, 0, 0);
        return owner;
    }
}
