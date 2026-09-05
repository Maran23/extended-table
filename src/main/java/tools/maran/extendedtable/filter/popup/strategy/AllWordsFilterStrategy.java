package tools.maran.extendedtable.filter.popup.strategy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// The filter text is split into lowercase tokens:
/// - Whitespace separates tokens, except within double quotes: `"foo bar"` is the single token `foo bar`
/// - Quotes also separate tokens, e.g. `foo"bar"` results in `foo` and `bar`
/// - An unclosed quote is ignored, e.g. `"foo bar` results in `foo` and `bar`
/// - `""` results in an empty token, which matches everything
/// - Duplicate tokens are collapsed
///
/// An item matches when its lowercase text contains every token as a substring (AND), in any order.
/// A text without any tokens matches every item.
///
/// @author Marius Hanl
final class AllWordsFilterStrategy implements FilterStrategy {

    private static final Pattern QUOTE_OR_WORD_PATTERN = Pattern.compile("\"([^\"]*)\"|([^\"\\s]+)");

    @Override
    public Predicate<String> createMatcher(String filterText) {
        Set<String> tokens = split(filterText);
        return itemText -> tokens.stream().allMatch(itemText::contains);
    }

    /// Splits the text by whitespace except within double quotes into normalized tokens.
    ///
    /// @param text
    ///         the text to split
    /// @return the normalized tokens
    Set<String> split(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = QUOTE_OR_WORD_PATTERN.matcher(normalize(text));

        while (matcher.find()) {
            String quoted = matcher.group(1);
            tokens.add(quoted != null ? quoted : matcher.group(2));
        }
        return tokens;
    }
}
