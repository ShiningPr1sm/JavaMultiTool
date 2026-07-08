package db;

public class NotificationRecord {
    private final int id;
    private final String type;
    private final int referenceId;
    private final int daysBefore;
    private final String notifiedAt;
    private final boolean dismissed;

    public NotificationRecord(int id, String type, int referenceId, int daysBefore, String notifiedAt, boolean dismissed) {
        this.id = id;
        this.type = type;
        this.referenceId = referenceId;
        this.daysBefore = daysBefore;
        this.notifiedAt = notifiedAt;
        this.dismissed = dismissed;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public int getReferenceId() { return referenceId; }
    public int getDaysBefore() { return daysBefore; }
    public String getNotifiedAt() { return notifiedAt; }
    public boolean isDismissed() { return dismissed; }
}
