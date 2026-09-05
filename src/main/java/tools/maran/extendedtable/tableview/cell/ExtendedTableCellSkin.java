package tools.maran.extendedtable.tableview.cell;

import javafx.css.PseudoClass;
import javafx.scene.control.skin.TableCellSkin;

/// [TableCellSkin] for the [ExtendedTableCell].
///
/// @param <S>
///         the source type
/// @param <T>
///         the target type
public class ExtendedTableCellSkin<S, T> extends TableCellSkin<S, T> {

    private static final PseudoClass PSEUDO_CLASS_EDITABLE = PseudoClass.getPseudoClass("editable");

    private ExtendedTableCell<S, T> tableCell;

    /// Creates a new [ExtendedTableCellSkin] instance.
    ///
    /// @param tableCell
    ///         the [ExtendedTableCell] where this skin belongs to
    public ExtendedTableCellSkin(ExtendedTableCell<S, T> tableCell) {
        super(tableCell);
        this.tableCell = tableCell;

        registerChangeListener(tableCell.editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableRow().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableColumn().editableProperty(), _ -> updateEditablePseudoClass());
        registerChangeListener(tableCell.getTableView().editableProperty(), _ -> updateEditablePseudoClass());

        updateEditablePseudoClass();
    }

    @Override
    public void dispose() {
        super.dispose();

        tableCell = null;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        // Overwritten as JavaFX is calculating in some weird magic offset.
        layoutLabelInArea(x, y, w, h);
    }

    private void updateEditablePseudoClass() {
        boolean isEditable =
                tableCell.isEditable() && tableCell.getTableRow().isEditable() && tableCell.getTableColumn()
                        .isEditable() && tableCell.getTableView().isEditable();
        tableCell.pseudoClassStateChanged(PSEUDO_CLASS_EDITABLE, isEditable);
        tableCell.updateValidState();
    }
}
