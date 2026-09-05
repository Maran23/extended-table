package tools.maran.extendedtable.data;

import java.util.Objects;

/// [Row] which is equal to every row with the same values, so its hash code changes whenever a value is edited.
public final class ValueRow extends Row {

    public ValueRow(String first, String second, String third) {
        super(first, second, third);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ValueRow row && Objects.equals(first(), row.first()) && Objects.equals(second(),
                row.second()) && Objects.equals(third(), row.third());
    }

    @Override
    public int hashCode() {
        return Objects.hash(first(), second(), third());
    }
}
