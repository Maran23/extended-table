package tools.maran.extendedtable.table.virtualflow;

import javafx.scene.control.IndexedCell;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.skin.VirtualFlow;

/// [VirtualFlow] with extended functionality. Exposes functions we will need for fixed column support.
///
/// @param <S>
///         the cell type
/// @author Marius Hanl
public class ExtendedVirtualFlow<S extends IndexedCell<?>> extends VirtualFlow<S> {

    /// Creates a new [ExtendedVirtualFlow] instance.
    public ExtendedVirtualFlow() {
    }

    /// Returns the scroll bar used for scrolling horizontally. A developer who needs to be notified when a scroll is
    /// happening could attach a listener to the [ScrollBar#valueProperty()].
    ///
    /// @return the scroll bar used for scrolling horizontally
    public ScrollBar getHorizontalBar() {
        return getHbar();
    }

    /// Returns the horizontal scrollbar value.
    ///
    /// @return the horizontal scrollbar value
    public double getHorizontalBarValue() {
        return getHbar().getValue();
    }

    @Override
    public void requestCellLayout() {
        super.requestCellLayout();
    }

    /// Scrolls horizontally as little as possible, so that the given range of the scrollable content is fully visible.
    /// The scrollable content starts after the fixed columns, which always stay visible in front of it.
    ///
    /// @param start
    ///         the start of the range, relative to the scrollable content
    /// @param width
    ///         the width of the range
    /// @param fixedWidth
    ///         the width of the fixed columns
    public void scrollHorizontallyIntoView(double start, double width, double fixedWidth) {
        ScrollBar vbar = getVbar();
        double viewportWidth = getWidth() - fixedWidth - (vbar.isVisible() ? vbar.getWidth() : 0);
        ScrollBar hbar = getHbar();
        double pos = hbar.getValue();

        double newPos;
        if (start < pos || width > viewportWidth) {
            newPos = start;
        } else if (start + width > pos + viewportWidth) {
            newPos = start + width - viewportWidth;
        } else {
            return;
        }
        hbar.setValue(Math.clamp(newPos, 0, hbar.getMax()));
    }
}
