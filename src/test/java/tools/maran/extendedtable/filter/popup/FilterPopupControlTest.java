package tools.maran.extendedtable.filter.popup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.List;

import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.maran.extendedtable.JavaFxTest;
import tools.maran.shadfx.Palette;
import tools.maran.shadfx.Theme;

/// Tests the [FilterPopupControl].
///
/// @author Marius Hanl
class FilterPopupControlTest extends JavaFxTest {

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

    @DisplayName("The popup looks like a context menu of the theme")
    @Test
    void testLooksLikeAContextMenu() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            show(popup);
            ContextMenu contextMenu = new ContextMenu(new MenuItem("Item"));
            contextMenu.show(getFilterBox(popup), 0, 0);

            assertEquals(((Region) contextMenu.getSkin().getNode()).getBackground(), getFilterBox(popup).getBackground());
            contextMenu.hide();
        });
    }

    @DisplayName("With a theme, which overrides the padding of a context menu, the content keeps its padding")
    @Test
    void testContentKeepsItsPaddingWithATheme() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            show(popup);
            Region filterBox = getFilterBox(popup);
            filterBox.getScene().getStylesheets().addAll(Theme.of(Palette.of()).stylesheets());
            filterBox.applyCss();

            Region content = (Region) filterBox.lookup(".content");
            double padding = Font.getDefault().getSize() * 0.333333;
            assertEquals(padding, content.getPadding().getTop(), 0.1);
            assertEquals(padding, content.getPadding().getLeft(), 0.1);
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

    @DisplayName("TAB in the item list selects the next item and SHIFT + TAB the previous one, wrapping around")
    @Test
    void testTabSelectsTheNextItem() {
        runOnFxThread(() -> {
            FilterPopupControl<String> popup = new FilterPopupControl<>();
            List<String> items = List.of("A", "B");
            popup.setBackingItems(items);
            popup.setItems(items);
            show(popup);
            ListView<?> listView = (ListView<?>) getFilterBox(popup).lookup(".list-view");

            pressTab(listView, false);
            assertEquals(0, listView.getSelectionModel().getSelectedIndex());

            pressTab(listView, false);
            assertEquals(1, listView.getSelectionModel().getSelectedIndex());

            pressTab(listView, true);
            assertEquals(0, listView.getSelectionModel().getSelectedIndex());

            pressTab(listView, true);
            assertEquals(1, listView.getSelectionModel().getSelectedIndex());
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

            pressTab(filterTextField, false);

            Node focusOwner = filterTextField.getScene().getFocusOwner();
            assertNotSame(filterTextField, focusOwner);
        });
    }

    private static void pressTab(Node node, boolean shift) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, shift, false, false, false));
    }

    private static Region getFilterBox(FilterPopupControl<String> popup) {
        return (Region) popup.getSkin().getNode();
    }

    private static void show(FilterPopupControl<String> popup) {
        popup.setReadFunction(String::valueOf);
        Label owner = new Label();
        showInStage(owner, 200, 200);
        popup.show(owner, 0, 0);
    }
}
