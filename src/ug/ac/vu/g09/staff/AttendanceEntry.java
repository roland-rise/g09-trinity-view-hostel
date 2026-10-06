package ug.ac.vu.g09.staff;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import ug.ac.vu.g09.core.Record;

/** One attendance record: either a sign-in (with time) or an absence (with reason).
 * @author Douth Nhial
 */
public class AttendanceEntry extends Record {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final Staff staff;          // module link: AttendanceEntry -> Staff
    private final String staffName;
    private final LocalDate date;
    private final boolean present;
    private final LocalTime signInTime; // null when absent
    private final String absenceReason; // null when present

    private AttendanceEntry(String id, Staff staff, LocalDate date, boolean present,
                            LocalTime signInTime, String absenceReason) {
        super(id, staff == null ? "Unknown" : staff.getFullName());
        if (staff == null) throw new IllegalArgumentException("Attendance must refer to a staff member.");
        if (date == null) throw new IllegalArgumentException("Date is required.");
        if (present && signInTime == null)
            throw new IllegalArgumentException("A sign-in time is required for attendance.");
        if (!present && (absenceReason == null || absenceReason.trim().isEmpty()))
            throw new IllegalArgumentException("An absence reason is required for an absent staff member.");
        this.staff = staff;
        this.staffName = staff.getFullName();
        this.date = date;
        this.present = present;
        this.signInTime = present ? signInTime : null;
        this.absenceReason = present ? null : absenceReason.trim().replace('|', '/');
    }

    public static AttendanceEntry signedIn(String id, Staff s, LocalDate d, LocalTime t) {
        return new AttendanceEntry(id, s, d, true, t, null);
    }

    public static AttendanceEntry absent(String id, Staff s, LocalDate d, String reason) {
        return new AttendanceEntry(id, s, d, false, null, reason);
    }

    public Staff getStaff() { return staff; }
    public String getStaffName() { return staffName; }
    public LocalDate getDate() { return date; }
    public boolean isPresent() { return present; }
    public LocalTime getSignInTime() { return signInTime; }
    public String getAbsenceReason() { return absenceReason; }

    @Override
    public String describe() {
        return ("ATTENDANCE  " + getId() + " | " + staffName + " (" + staff.getId() + ") | "
            + date.format(DATE_FMT) + " | "
            + (present ? "Signed in " + signInTime.format(TIME_FMT) : "ABSENT - " + absenceReason));
    }

    public String toFileLine() {
        return getId() + "|" + staff.getId() + "|" + date + "|" + present + "|"
            + (present ? signInTime : "") + "|" + (present ? "" : absenceReason);
    }
}
