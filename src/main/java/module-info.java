/// Defines all components that are required and available in the extended table framework.
///
/// @author Marius Hanl
module tools.maran.extendedtable {
    requires transitive javafx.controls;

    exports tools.maran.extendedtable.filter;
    exports tools.maran.extendedtable.filter.popup.strategy;
    exports tools.maran.extendedtable.filter.popup;

    exports tools.maran.extendedtable.table.common;
    exports tools.maran.extendedtable.table.header;
    exports tools.maran.extendedtable.table.validation;
    exports tools.maran.extendedtable.table.virtualflow;

    exports tools.maran.extendedtable.tableview;
    exports tools.maran.extendedtable.tableview.cell;
    exports tools.maran.extendedtable.tableview.column;
    exports tools.maran.extendedtable.tableview.row;

    exports tools.maran.extendedtable.treetableview;
    exports tools.maran.extendedtable.treetableview.cell;
    exports tools.maran.extendedtable.treetableview.column;
    exports tools.maran.extendedtable.treetableview.line;
    exports tools.maran.extendedtable.treetableview.row;
}
