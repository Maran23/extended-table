package tools.maran.extendedtable.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Utility class with methods for splitting and determining matches of word (groups) for filtering.
///
/// @author Marius Hanl
public final class FilterUtils {

    private static final Pattern QUOTE_OR_WORD_PATTERN = Pattern.compile("\"([^\"]*)\"|([^\"\\s]+)");

    private FilterUtils() {
        // noop
    }

    /// Checks if the provided itemText contains all tokens from the specified set.
    ///
    /// @param tokens
    ///         the set of tokens to check within the itemText
    /// @param itemText
    ///         the string to be checked against the tokens
    /// @return true if the set of tokens is empty or if all tokens are substrings of itemText,
    /// false otherwise
    static boolean contains(Set<String> tokens, String itemText) {
        if (tokens.isEmpty()) {
            return true;
        }
        return tokens.stream().allMatch(itemText::contains);
    }

    /// Splits the input text by whitespace characters except those within double quotes.
    /// Double-quoted sections are treated as single units.
    ///
    /// @param text
    ///         The input text to be processed.
    /// @return An array of strings where each element is a segment of the input text,
    /// ignoring whitespace outside double quotes. All segments are converted
    /// to lowercase.
    static Set<String> splitByWhiteSpaceExceptQuotes(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = QUOTE_OR_WORD_PATTERN.matcher(text);

        while (matcher.find()) {
            String group = matcher.group(1);

            if (group != null) {
                tokens.add(group.toLowerCase());
            } else {
                group = matcher.group(2);

                if (group != null) {
                    tokens.add(group.toLowerCase());
                }
            }
        }
        return tokens;
    }
}
