package tools.maran.extendedtable.filter.popup.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.util.DefaultLocale;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/// Tests [AllWordsFilterStrategy].
///
/// @author Marius Hanl
class AllWordsFilterStrategyTest {

    private final AllWordsFilterStrategy strategy = new AllWordsFilterStrategy();

    @DisplayName("Matching is case-insensitive but accent-sensitive")
    @ParameterizedTest
    @CsvSource({ "CAFÉ, café, true", "zürich, ZÜRICH, true", "cafe, Café, false", "café, Cafe, false" })
    void testMatchIsCaseInsensitiveButAccentSensitive(String filterText, String itemText, boolean expected) {
        assertEquals(expected, matches(filterText, itemText));
    }

    @DisplayName("All tokens must be contained in any order")
    @ParameterizedTest
    @CsvSource({ "york new, New York City, true", "'   ', anything, true", "york boston, New York City, false",
            "\"york new\", New York City, false" })
    void testMatchRequiresAllTokens(String filterText, String itemText, boolean expected) {
        assertEquals(expected, matches(filterText, itemText));
    }

    @DisplayName("Lowercasing does not depend on the default locale")
    @Test
    @DefaultLocale(language = "tr")
    void testNormalizeIsLocaleIndependent() {
        assertTrue(matches("item", "ITEM"));
    }

    @DisplayName("Text is split by whitespace except within quotes")
    @ParameterizedTest
    @MethodSource("splitArguments")
    void testSplit(String text, Set<String> expectedTokens) {
        assertEquals(expectedTokens, strategy.split(text));
    }

    private boolean matches(String filterText, String itemText) {
        return strategy.createMatcher(filterText).test(strategy.normalize(itemText));
    }

    private static Stream<Arguments> splitArguments() {
        return Stream.of(Arguments.of("\"New York\"  2024", Set.of("new york", "2024")),
                Arguments.of("foo\"bar\"", Set.of("foo", "bar")), Arguments.of("\"foo bar", Set.of("foo", "bar")),
                Arguments.of("foo FOO", Set.of("foo")), Arguments.of("   ", Set.of()));
    }
}
