package tools.maran.extendedtable.filter.popup.strategy;

import java.util.Locale;
import java.util.function.Predicate;

import tools.maran.extendedtable.filter.popup.FilterPopupControl;

/// Strategy which matches the item texts of a [FilterPopupControl] against the entered filter text.
///
/// @author Marius Hanl
@FunctionalInterface
public interface FilterStrategy {

    /// The default strategy: case-insensitive, every whitespace separated word or double-quoted phrase of the filter
    /// text must be contained in the item text, in any order.
    FilterStrategy DEFAULT = new AllWordsFilterStrategy();

    /// Normalizes the given item text, which is then matched by [#createMatcher(String)].
    /// Called once per item text, the result is cached. Lowercases the text by default.
    ///
    /// @param itemText
    ///         the item text
    /// @return the normalized item text
    default String normalize(String itemText) {
        return itemText.toLowerCase(Locale.ROOT);
    }

    /// Creates a matcher for the given filter text, which tests the normalized item texts.
    ///
    /// @param filterText
    ///         the filter text, never empty
    /// @return the matcher
    Predicate<String> createMatcher(String filterText);
}
