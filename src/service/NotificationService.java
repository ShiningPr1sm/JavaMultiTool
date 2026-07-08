package service;

import db.BDaysRepository;
import db.DatabaseProvider;
import db.NotificationRecord;
import db.NotificationRepository;
import util.AppLogger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationService {
    private static final int[] REMINDER_DAYS = {7, 3, 1};
    private static final DateTimeFormatter STORAGE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public int checkBirthdayReminders() {
        NotificationRepository notifRepo = DatabaseProvider.getNotificationRepository();
        BDaysRepository bdaysRepo = DatabaseProvider.getBDaysRepository();
        int newCount = 0;

        try {
            var birthdays = bdaysRepo.getAllBirthdays();
            LocalDate today = LocalDate.now();

            for (var bday : birthdays) {
                String dbDateStr = bday.getBdayDate();
                int month = Integer.parseInt(dbDateStr.substring(5, 7));
                int day = Integer.parseInt(dbDateStr.substring(8, 10));

                for (int daysBefore : REMINDER_DAYS) {
                    LocalDate reminderDate = today.plusDays(daysBefore);
                    if (reminderDate.getMonthValue() == month && reminderDate.getDayOfMonth() == day) {
                        if (!notifRepo.hasBeenNotifiedToday(bday.getId(), "bday_reminder", daysBefore)) {
                            notifRepo.insertNotification("bday_reminder", bday.getId(), daysBefore);
                            newCount++;
                            AppLogger.info("Notification: " + bday.getName() + " in " + daysBefore + " days");
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.error("NotificationService: failed to check reminders: " + e.getMessage());
        }

        return newCount;
    }

    public List<NotificationRecord> getActiveNotifications() {
        return DatabaseProvider.getNotificationRepository().getActiveNotifications();
    }

    public void dismissNotification(int id) {
        DatabaseProvider.getNotificationRepository().dismissNotification(id);
    }

    public int countActive() {
        return DatabaseProvider.getNotificationRepository().countActive();
    }
}
