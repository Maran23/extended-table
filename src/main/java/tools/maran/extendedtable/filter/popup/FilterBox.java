package tools.maran.extendedtable.filter.popup;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import tools.maran.extendedtable.table.common.TableI18N;

/// Root container of the [FilterPopupControl], which creates the layout.
///
/// @param <T>
///         the list item type
/// @author Marius Hanl
class FilterBox<T> extends VBox {

    private final CheckBox selectStateCbx;
    private final TextField filterTxt;
    private final ListView<T> itemListView;
    private final Label selectionIndicatorLbl;
    private final Button applyBtn;
    private final Button colResetBtn;
    private final Button resetBtn;

    FilterBox() {
        getStyleClass().add("filter-box");

        selectStateCbx = new CheckBox();
        filterTxt = new TextField();
        HBox.setHgrow(filterTxt, Priority.ALWAYS);

        HBox header = new HBox(selectStateCbx, filterTxt);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);

        itemListView = new ListView<>();
        selectionIndicatorLbl = new Label();

        applyBtn = new Button(TableI18N.message("apply"));
        applyBtn.setMaxWidth(Double.MAX_VALUE);

        colResetBtn = new Button(TableI18N.message("reset.column"));
        colResetBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(colResetBtn, Priority.ALWAYS);

        resetBtn = new Button(TableI18N.message("reset.column.all"));
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resetBtn, Priority.ALWAYS);

        HBox resetButtons = new HBox(colResetBtn, resetBtn);
        resetButtons.getStyleClass().add("reset-buttons");

        VBox footer = new VBox(applyBtn, resetButtons);
        footer.getStyleClass().add("footer");

        getChildren().addAll(header, itemListView, selectionIndicatorLbl, footer);
    }

    @Override
    public String getUserAgentStylesheet() {
        return FilterBox.class.getResource("filter-popup.css").toExternalForm();
    }

    Button getApplyButton() {
        return applyBtn;
    }

    TextField getFilterTextField() {
        return filterTxt;
    }

    ListView<T> getItemListView() {
        return itemListView;
    }

    Button getResetAllButton() {
        return resetBtn;
    }

    Button getResetColumnButton() {
        return colResetBtn;
    }

    CheckBox getSelectStateCheckBox() {
        return selectStateCbx;
    }

    Label getSelectionIndicatorLabel() {
        return selectionIndicatorLbl;
    }
}
