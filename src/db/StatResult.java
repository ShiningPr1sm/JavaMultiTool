package db;

public class StatResult {
    public int[] values;
    public String[] labels;

    public StatResult(int size) {
        values = new int[size];
        labels = new String[size];
    }
}
