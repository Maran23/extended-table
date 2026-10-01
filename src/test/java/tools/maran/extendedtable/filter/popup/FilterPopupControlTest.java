package tools.maran.extendedtable.filter.popup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.maran.extendedtable.JavaFxTest;

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
