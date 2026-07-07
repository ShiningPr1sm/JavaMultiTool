package db;

public class BirthdayRecord {
    private final int id;
    private final String name;
    private final String bdayDate;

    public BirthdayRecord(int id, String name, String bdayDate) {
        this.id = id;
        this.name = name;
        this.bdayDate = bdayDate;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getBdayDate() { return bdayDate; }
}
