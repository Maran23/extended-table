package tools.maran.extendedtable.filter.data;

/// Mutable row which is only equal to itself, so rows with the same values can be distinguished.
public class Row {

    private String first;
    private String second;
    private String third;

    public Row(String first, String second, String third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public String first() {
        return first;
    }

    public String second() {
        return second;
    }

    public String third() {
        return third;
    }

    @Override
    public String toString() {
        return "Row[" + first + ", " + second + ", " + third + "]";
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    public void setThird(String third) {
        this.third = third;
    }
}
