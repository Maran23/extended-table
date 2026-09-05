package tools.maran.extendedtable.table.common;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/// Utility class which resolves the localized messages of the extended table framework.
///
/// @author Marius Hanl
public final class TableI18N {

    private static final String BUNDLE_NAME = "tools.maran.extendedtable.table.common.messages";

    private TableI18N() {
        // noop
    }

    /// Returns the message for the given key.
    ///
    /// @param key
    ///         the key of the message
    /// @return the localized message
    public static String message(String key) {
        return getBundle().getString(key);
    }

    /// Returns the message for the given key, formatted with the given parameters.
    ///
    /// @param key
    ///         the key of the message
    /// @param params
    ///         the parameters used to format the message
    /// @return the localized and formatted message
    public static String message(String key, Object... params) {
        return MessageFormat.format(getBundle().getString(key), params);
    }

    private static ResourceBundle getBundle() {
        return ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
    }
}
