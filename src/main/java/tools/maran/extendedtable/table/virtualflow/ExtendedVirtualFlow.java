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

}
