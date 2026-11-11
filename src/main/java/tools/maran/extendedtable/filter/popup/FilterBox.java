package tools.maran.extendedtable.filter.popup;

import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ListViewSkin;
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
    private final CustomListView<T> itemListView;
    private final Label selectionIndicatorLbl;
    private final Button applyBtn;
    private final Button colResetBtn;
    private final Button resetBtn;

    FilterBox() {
        getStyleClass().addAll("context-menu", "filter-box");

        selectStateCbx = new CheckBox();
        filterTxt = new TextField();
        HBox.setHgrow(filterTxt, Priority.ALWAYS);

        HBox header = new HBox(selectStateCbx, filterTxt);
        header.getStyleClass().add("header");

        itemListView = new CustomListView<>();
        selectionIndicatorLbl = new Label();

        applyBtn = new Button(TableI18N.message("apply"));
        applyBtn.setTooltip(new Tooltip(TableI18N.message("apply.desc")));
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setDefaultButton(true);

        colResetBtn = new Button(TableI18N.message("reset.column"));
        colResetBtn.setTooltip(new Tooltip(TableI18N.message("reset.column.desc")));
        colResetBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(colResetBtn, Priority.ALWAYS);

        resetBtn = new Button(TableI18N.message("reset.column.all"));
        resetBtn.setTooltip(new Tooltip(TableI18N.message("reset.column.all.desc")));
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

    CustomListView<T> getItemListView() {
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

    /// Custom [ListView] which can scroll an item into view without shifting it to the top.
    static class CustomListView<T> extends ListView<T> {

        private static final String SCROLL_INTO_VIEW = "scrollIntoView";

        /// Scrolls as little as possible, so that the item at the given index is fully visible.
        ///
        /// @param index
        ///         the item index
        void scrollIntoView(int index) {
            getProperties().put(SCROLL_INTO_VIEW, index);
        }

        @Override
        protected Skin<?> createDefaultSkin() {
            return new CustomListViewSkin<>(this);
        }
    }

    /// [ListViewSkin] for [CustomListView].
    ///
    /// @author Marius Hanl
    private static class CustomListViewSkin<T> extends ListViewSkin<T> {

        private final MapChangeListener<Object, Object> propertiesMapListener = this::onPropertiesAdded;

        CustomListViewSkin(CustomListView<T> listView) {
            super(listView);

            listView.getProperties().remove(CustomListView.SCROLL_INTO_VIEW);
            listView.getProperties().addListener(propertiesMapListener);
        }

        @Override
        public void dispose() {
            if (getSkinnable() == null) {
                return;
            }

            getSkinnable().getProperties().removeListener(propertiesMapListener);
            super.dispose();
        }

        private void onPropertiesAdded(Change<?, ?> change) {
            if (change.wasAdded() && CustomListView.SCROLL_INTO_VIEW.equals(change.getKey())) {
                getSkinnable().getProperties().remove(CustomListView.SCROLL_INTO_VIEW);

                int index = (int) change.getValueAdded();
                getVirtualFlow().scrollTo(index);
            }
        }
    }
}
