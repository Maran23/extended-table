package tools.maran.extendedtable.treetableview.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Tests the [TreeLineDrawer].
///
/// @author Marius Hanl
class TreeLineDrawerTest {

    @DisplayName("Coordinates are snapped to the physical pixel grid, odd line widths to the pixel center")
    @ParameterizedTest
    @CsvSource({
            "10.3, 1, 1, 10.5",
            "10.3, 1, 2, 10",
            "10.3, 1.25, 1, 10",
            "10.3, 1.25, 2, 9.6",
            "10.3, 1.5, 1, 10.333333333333334",
            "10.3, 2, 2, 10",
    })
    void testSnap(double value, double scale, double lineWidth, double expected) {
        assertEquals(expected, TreeLineDrawer.snap(value, scale, lineWidth), 1e-9);
    }

}
