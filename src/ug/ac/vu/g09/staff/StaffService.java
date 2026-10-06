package ug.ac.vu.g09.staff;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.staff.Staff.Role;
import ug.ac.vu.g09.staff.Staff.Shift;

/** CRUD, attendance, shift rules, reports and file storage for the Staff module.
 * @author Douth Nhial
 */
public class StaffService {

    private final ArrayList<Staff> staffList = new ArrayList<>();
    private final ArrayList<AttendanceEntry> attendanceList = new ArrayList<>();
    private final Path staffFile;
    private final Path attendanceFile;
    private int nextStaffNo = 1;
    private int nextAttNo = 1;

    public StaffService(String staffFile, String attendanceFile) {
        this.staffFile = Paths.get(staffFile);
        this.attendanceFile = Paths.get(attendanceFile);
    }

    // ---------- Staff CRUD ----------

    public Staff addStaff(String fullName, String roleText) {
        Role role = Role.parse(roleText);
        Staff s = new Staff(String.format("G09-S%03d", nextStaffNo), fullName, role); // validates name first
        checkQuota(role, null);
        nextStaffNo++;
        staffList.add(s);
        return s;
    }

    public Staff findStaffById(String id) {
        if (id != null) {
            for (Staff s : staffList) {
                if (s.getId().equalsIgnoreCase(id.trim())) return s;
            }
        }
        throw new IllegalArgumentException("No staff member with ID '" + id + "'.");
    }

    public ArrayList<Staff> findStaffByName(String part) {
        ArrayList<Staff> found = new ArrayList<>();
        if (part == null || part.trim().isEmpty()) return found;
        for (Staff s : staffList) {
            if (s.getFullName().toLowerCase().contains(part.trim().toLowerCase())) found.add(s);
        }
        return found;
    }

    public ArrayList<Staff> getAllStaff() { return new ArrayList<>(staffList); }

    public void updateStaff(String id, String newName, String newRoleText) {
        Staff s = findStaffById(id);
        if (newName != null && !newName.trim().isEmpty()) s.setFullName(newName);
        if (newRoleText != null && !newRoleText.trim().isEmpty()) {
            Role r = Role.parse(newRoleText);
            if (r != s.getRole()) {
                checkQuota(r, s);
                s.setRole(r);
            }
        }
    }

    public void removeStaff(String id) {
        Staff s = findStaffById(id);
        staffList.remove(s);
        attendanceList.removeIf(a -> a.getStaff() == s); // keep references consistent
    }

    private void checkQuota(Role role, Staff ignore) {
        int count = 0;
        for (Staff s : staffList) {
            if (s != ignore && s.getRole() == role) count++;
        }
        if (count >= role.getQuota()) {
            throw new IllegalArgumentException("Cannot have more than " + role.getQuota()
                + " " + role + " staff (client rule).");
        }
    }

    // ---------- Attendance ----------

    public AttendanceEntry recordAttendance(String staffId, LocalDate date, LocalTime time) {
        Staff s = findStaffById(staffId);
        rejectDuplicate(s, date);
        AttendanceEntry e = AttendanceEntry.signedIn(nextAttId(), s, date, time);
        nextAttNo++;
        attendanceList.add(e);
        return e;
    }

    public AttendanceEntry recordAbsence(String staffId, LocalDate date, String reason) {
        Staff s = findStaffById(staffId);
        rejectDuplicate(s, date);
        AttendanceEntry e = AttendanceEntry.absent(nextAttId(), s, date, reason);
        nextAttNo++;
        attendanceList.add(e);
        return e;
    }

    private void rejectDuplicate(Staff s, LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date is required.");
        for (AttendanceEntry a : attendanceList) {
            if (a.getStaff() == s && a.getDate().equals(date))
                throw new IllegalArgumentException(s.getFullName() + " already has attendance for that date.");
        }
    }

    private String nextAttId() { return String.format("G09-A%03d", nextAttNo); }

    // ---------- Shift rules (security only) ----------

    private void requireManager(String managerId) throws ShiftClashException {
        Staff m;
        try { m = findStaffById(managerId); }
        catch (IllegalArgumentException e) { throw new ShiftClashException("Shift change refused: manager ID not found."); }
        if (m.getRole() != Role.MANAGER)
            throw new ShiftClashException("Shift change refused: only the manager can authorise shifts.");
    }

    private int countOnShift(Shift shift) {
        int n = 0;
        for (Staff s : staffList) {
            if (s.getRole() == Role.SECURITY && s.getShift() == shift) n++;
        }
        return n;
    }

    /** Put a security guard on Day (max 1) or Night (max 2). Manager authorisation required. */
    public void assignShift(String staffId, String shiftText, String managerId) throws ShiftClashException {
        requireManager(managerId);
        Staff s = findStaffById(staffId);
        if (s.getRole() != Role.SECURITY)
            throw new IllegalArgumentException("Only security staff work shifts.");
        Shift target = Shift.parse(shiftText);
        if (target == Shift.UNASSIGNED) throw new IllegalArgumentException("Choose Day or Night.");
        if (s.getShift() == target) return;
        if (countOnShift(target) >= target.getRequired())
            throw new ShiftClashException("Shift clash: " + target + " already has "
                + target.getRequired() + " security. Swap two guards instead.");
        s.setShift(target);
    }

    /** Swap the shifts of two guards (keeps 1 day / 2 night). Manager authorisation required. */
    public void swapShifts(String idA, String idB, String managerId) throws ShiftClashException {
        requireManager(managerId);
        Staff a = findStaffById(idA);
        Staff b = findStaffById(idB);
        if (a.getRole() != Role.SECURITY || b.getRole() != Role.SECURITY)
            throw new IllegalArgumentException("Only security staff work shifts.");
        if (a.getShift() == Shift.UNASSIGNED || b.getShift() == Shift.UNASSIGNED)
            throw new ShiftClashException("Both guards must already be assigned before swapping.");
        Shift tmp = a.getShift();
        a.setShift(b.getShift());
        b.setShift(tmp);
    }

    /** Throws ShiftClashException unless exactly 1 guard is on Day and exactly 2 on Night. */
    public void validateSecurityCoverage() throws ShiftClashException {
        int day = countOnShift(Shift.DAY), night = countOnShift(Shift.NIGHT);
        if (day != Shift.DAY.getRequired())
            throw new ShiftClashException("Shift clash: Day shift has " + day + " security (needs exactly 1).");
        if (night != Shift.NIGHT.getRequired())
            throw new ShiftClashException("Shift clash: Night shift has " + night + " security (needs exactly 2).");
    }

    // ---------- Reports ----------

    /** Comparator: custom role order (Manager, Cleaner, Custodian, Security, Secretary), then name. */
    public static final Comparator<Staff> BY_ROLE =
        Comparator.comparingInt((Staff s) -> s.getRole().getReportRank())
                  .thenComparing(Staff::getFullName, String.CASE_INSENSITIVE_ORDER);

    public ArrayList<Staff> staffSortedByRole() {
        ArrayList<Staff> copy = new ArrayList<>(staffList);
        copy.sort(BY_ROLE);
        return copy;
    }

    public String absenteeReport(LocalDate date) {
        StringBuilder sb = new StringBuilder("ABSENT STAFF - " + date.format(AttendanceEntry.DATE_FMT) + "\n");
        int n = 0;
        for (AttendanceEntry a : attendanceList) {
            if (!a.isPresent() && a.getDate().equals(date)) {
                sb.append('\n').append(a.getStaff().getId()).append(" - ").append(a.getStaffName())
                  .append(" - ").append(a.getStaff().getRole()).append('\n')
                  .append("Reason: ").append(a.getAbsenceReason()).append('\n');
                n++;
            }
        }
        if (n == 0) sb.append("\nNo absentees recorded.\n");
        return sb.toString();
    }

    // ---------- Polymorphism demo ----------

    public ArrayList<Record> getAllRecords() {
        ArrayList<Record> all = new ArrayList<>();
        all.addAll(staffList);        // Staff objects
        all.addAll(attendanceList);   // AttendanceEntry objects
        return all;
    }

    public void describeAll() {
        for (Record record : getAllRecords()) {
            System.out.println(record.describe()); // same call, different behaviour per object
        }
    }

    // ---------- File storage ----------

    public void save() throws IOException {
        ArrayList<String> sl = new ArrayList<>();
        for (Staff s : staffList) sl.add(s.toFileLine());
        Files.write(staffFile, sl);
        ArrayList<String> al = new ArrayList<>();
        for (AttendanceEntry a : attendanceList) al.add(a.toFileLine());
        Files.write(attendanceFile, al);
    }

    /** Loads both files. Missing files = fresh start. Corrupt lines throw IOException. */
    public void load() throws IOException {
        staffList.clear();
        attendanceList.clear();
        nextStaffNo = 1;
        nextAttNo = 1;
        if (Files.exists(staffFile)) {
            for (String line : Files.readAllLines(staffFile)) {
                if (line.trim().isEmpty()) continue;
                try {
                    Staff s = Staff.fromFileLine(line);
                    staffList.add(s);
                    nextStaffNo = Math.max(nextStaffNo, numberOf(s.getId()) + 1);
                } catch (RuntimeException e) {
                    throw new IOException("Corrupt staff file line: " + line, e);
                }
            }
        }
        if (Files.exists(attendanceFile)) {
            for (String line : Files.readAllLines(attendanceFile)) {
                if (line.trim().isEmpty()) continue;
                try {
                    String[] p = line.split("\\|", -1);
                    if (p.length != 6) throw new IllegalArgumentException("wrong field count");
                    Staff s = findStaffById(p[1]);
                    LocalDate d = LocalDate.parse(p[2]);
                    AttendanceEntry e = Boolean.parseBoolean(p[3])
                        ? AttendanceEntry.signedIn(p[0], s, d, LocalTime.parse(p[4]))
                        : AttendanceEntry.absent(p[0], s, d, p[5]);
                    attendanceList.add(e);
                    nextAttNo = Math.max(nextAttNo, numberOf(p[0]) + 1);
                } catch (RuntimeException e) {
                    throw new IOException("Corrupt attendance file line: " + line, e);
                }
            }
        }
    }

    private static int numberOf(String id) {
        return Integer.parseInt(id.substring(id.length() - 3));
    }
}
