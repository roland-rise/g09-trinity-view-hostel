import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.staff.*;

/** Plain-Java test runner (no JUnit needed). Run: java -cp out StaffModuleTests */
public class StaffModuleTests {
    interface Body { void run() throws Exception; }
    static int pass = 0, fail = 0;
    static final LocalDate D = LocalDate.of(2026, 10, 4);

    static void check(String id, String what, Body b) {
        try { b.run(); pass++; System.out.println("PASS " + id + " " + what); }
        catch (Throwable t) { fail++; System.out.println("FAIL " + id + " " + what + " -> " + t); }
    }
    static void expect(boolean c, String m) { if (!c) throw new AssertionError(m); }
    static <T extends Throwable> void throwsEx(Class<T> type, Body b) {
        try { b.run(); } catch (Throwable t) { if (type.isInstance(t)) return; throw new AssertionError("wrong exception " + t); }
        throw new AssertionError("no exception thrown");
    }

    static StaffService fresh() throws Exception {
        Path dir = Files.createTempDirectory("g09");
        return new StaffService(dir.resolve("staff.txt").toString(), dir.resolve("att.txt").toString());
    }
    /** Full roster: S001 Mgr, S002-4 Security, S005-6 Cleaner, S007-8 Custodian, S009 Secretary */
    static StaffService roster() throws Exception {
        StaffService s = fresh();
        String[][] d = {{"Alice Mwesigwa","Manager"},{"Sam Okello","Security"},{"Jane Akello","Security"},
            {"Paul Kato","Security"},{"Mary Jane","Cleaner"},{"Ruth Nabirye","Cleaner"},
            {"John Doe","Custodian"},{"Ivan Lubega","Custodian"},{"Grace Atim","Secretary"}};
        for (String[] x : d) s.addStaff(x[0], x[1]);
        return s;
    }
    static void fullShifts(StaffService s) throws Exception {
        s.assignShift("G09-S002", "Day", "G09-S001");
        s.assignShift("G09-S003", "Night", "G09-S001");
        s.assignShift("G09-S004", "Night", "G09-S001");
    }

    public static void main(String[] args) {
        check("TC01", "valid staff added, ID G09-S001", () -> {
            Staff s = fresh().addStaff("Alice Mwesigwa", "Manager");
            expect(s.getId().equals("G09-S001"), "id " + s.getId()); });
        check("TC02", "second Manager rejected (quota 1)", () -> throwsEx(IllegalArgumentException.class, () -> roster().addStaff("Extra Person", "Manager")));
        check("TC03", "invalid role text rejected", () -> throwsEx(IllegalArgumentException.class, () -> fresh().addStaff("Alice Mwesigwa", "Pilot")));
        check("TC04", "name with digits/symbols rejected", () -> throwsEx(IllegalArgumentException.class, () -> fresh().addStaff("B0b!", "Cleaner")));
        check("TC05", "blank name rejected", () -> throwsEx(IllegalArgumentException.class, () -> fresh().addStaff("   ", "Cleaner")));
        check("TC06", "unknown staff ID rejected", () -> throwsEx(IllegalArgumentException.class, () -> roster().findStaffById("G09-S999")));
        check("TC07", "find by ID is case-insensitive; find by partial name", () -> {
            StaffService s = roster();
            expect(s.findStaffById("g09-s005").getFullName().equals("Mary Jane"), "id");
            expect(s.findStaffByName("jane").size() == 2, "name count"); });
        check("TC08", "update name; blank role keeps existing role", () -> {
            StaffService s = roster(); s.updateStaff("G09-S009", "Grace A. Atim", "Cleaner".equals("x") ? "" : "");
            expect(s.findStaffById("G09-S009").getFullName().equals("Grace A. Atim"), "name"); });
        check("TC09", "update role beyond quota rejected", () -> throwsEx(IllegalArgumentException.class, () -> roster().updateStaff("G09-S009", "", "Manager")));
        check("TC10", "sign-in recorded with time, ID G09-A001", () -> {
            AttendanceEntry e = roster().recordAttendance("G09-S002", D, LocalTime.of(5, 55));
            expect(e.getId().equals("G09-A001") && e.getSignInTime().equals(LocalTime.of(5, 55)), "entry"); });
        check("TC11", "absence with reason accepted", () -> {
            AttendanceEntry e = roster().recordAbsence("G09-S005", D, "Family emergency");
            expect(!e.isPresent() && e.getAbsenceReason().equals("Family emergency"), "entry"); });
        check("TC12", "absence with blank reason rejected", () -> throwsEx(IllegalArgumentException.class, () -> roster().recordAbsence("G09-S005", D, "  ")));
        check("TC13", "absence with null reason rejected", () -> throwsEx(IllegalArgumentException.class, () -> roster().recordAbsence("G09-S005", D, null)));
        check("TC14", "attendance for invalid staff ID rejected", () -> throwsEx(IllegalArgumentException.class, () -> roster().recordAttendance("G09-S777", D, LocalTime.NOON)));
        check("TC15", "duplicate attendance same staff/date rejected", () -> {
            StaffService s = roster(); s.recordAttendance("G09-S002", D, LocalTime.of(6, 0));
            throwsEx(IllegalArgumentException.class, () -> s.recordAbsence("G09-S002", D, "Sick")); });
        check("TC16", "manager can assign Day shift", () -> {
            StaffService s = roster(); s.assignShift("G09-S002", "Day", "G09-S001");
            expect(s.findStaffById("G09-S002").getShift() == Staff.Shift.DAY, "shift"); });
        check("TC17", "non-manager authorising shift change refused", () -> {
            StaffService s = roster(); throwsEx(ShiftClashException.class, () -> s.assignShift("G09-S002", "Day", "G09-S005")); });
        check("TC18", "second guard on Day shift -> ShiftClashException", () -> {
            StaffService s = roster(); s.assignShift("G09-S002", "Day", "G09-S001");
            throwsEx(ShiftClashException.class, () -> s.assignShift("G09-S003", "Day", "G09-S001")); });
        check("TC19", "third guard on Night shift -> ShiftClashException", () -> {
            StaffService s = roster(); fullShifts(s);
            throwsEx(ShiftClashException.class, () -> s.assignShift("G09-S002", "Night", "G09-S001")); });
        check("TC20", "non-security staff cannot be given a shift", () -> {
            StaffService s = roster(); throwsEx(IllegalArgumentException.class, () -> s.assignShift("G09-S005", "Day", "G09-S001")); });
        check("TC21", "coverage with too few Night guards -> ShiftClashException", () -> {
            StaffService s = roster(); s.assignShift("G09-S002", "Day", "G09-S001"); s.assignShift("G09-S003", "Night", "G09-S001");
            throwsEx(ShiftClashException.class, s::validateSecurityCoverage); });
        check("TC22", "coverage 1 Day + 2 Night passes", () -> { StaffService s = roster(); fullShifts(s); s.validateSecurityCoverage(); });
        check("TC23", "swap keeps 1 Day / 2 Night; refused without manager", () -> {
            StaffService s = roster(); fullShifts(s);
            s.swapShifts("G09-S002", "G09-S003", "G09-S001"); s.validateSecurityCoverage();
            expect(s.findStaffById("G09-S003").getShift() == Staff.Shift.DAY, "swapped");
            throwsEx(ShiftClashException.class, () -> s.swapShifts("G09-S002", "G09-S003", "G09-S006")); });
        check("TC24", "Comparator report order Manager, Cleaner, Custodian, Security, Secretary", () -> {
            StringBuilder sb = new StringBuilder();
            for (Staff x : roster().staffSortedByRole()) sb.append(x.getRole().getLabel().charAt(2));
            // Manager(n) Cleaner(e) Cleaner(e) Custodian(s) Custodian(s) Security(c) x3 Secretary(c)
            List<String> roles = new ArrayList<>();
            for (Staff x : roster().staffSortedByRole()) roles.add(x.getRole().getLabel());
            expect(roles.equals(Arrays.asList("Manager","Cleaner","Cleaner","Custodian","Custodian","Security","Security","Security","Secretary")), roles.toString()); });
        check("TC25", "absentee report lists only absentees on that date", () -> {
            StaffService s = roster();
            s.recordAbsence("G09-S005", D, "Family emergency"); s.recordAttendance("G09-S002", D, LocalTime.of(6, 0));
            s.recordAbsence("G09-S007", D.plusDays(1), "Medical");
            String r = s.absenteeReport(D);
            expect(r.startsWith("ABSENT STAFF - 04/10/2026") && r.contains("G09-S005 - Mary Jane - Cleaner")
                && r.contains("Reason: Family emergency") && !r.contains("G09-S002") && !r.contains("G09-S007"), r); });
        check("TC26", "absentee report with none shows message", () -> expect(roster().absenteeReport(D).contains("No absentees recorded."), "msg"));
        check("TC27", "save then load restores staff, shifts and attendance", () -> {
            Path dir = Files.createTempDirectory("g09rt");
            String sf = dir.resolve("s.txt").toString(), af = dir.resolve("a.txt").toString();
            StaffService a = new StaffService(sf, af);
            for (String[] x : new String[][]{{"Alice Mwesigwa","Manager"},{"Sam Okello","Security"}}) a.addStaff(x[0], x[1]);
            a.assignShift("G09-S002", "Day", "G09-S001"); a.recordAbsence("G09-S002", D, "Sick"); a.save();
            StaffService b = new StaffService(sf, af); b.load();
            expect(b.getAllRecords().size() == 3, "count"); expect(b.findStaffById("G09-S002").getShift() == Staff.Shift.DAY, "shift");
            expect(b.addStaff("Grace Atim", "Secretary").getId().equals("G09-S003"), "next id continues"); });
        check("TC28", "missing data files -> clean start, no exception", () -> { StaffService s = fresh(); s.load(); expect(s.getAllRecords().isEmpty(), "empty"); });
        check("TC29", "corrupt first line is skipped and the good line is kept", () -> {
            Path dir = Files.createTempDirectory("g09bad"); Files.writeString(dir.resolve("s.txt"), "garbage line\nG09-S001|Alice Mwesigwa|MANAGER|UNASSIGNED\n");
            StaffService s = new StaffService(dir.resolve("s.txt").toString(), dir.resolve("a.txt").toString());
            s.load(); expect(s.getAllStaff().size() == 1, "size " + s.getAllStaff().size()); });
        check("TC30", "attendance line pointing at unknown staff ID is skipped", () -> {
            Path dir = Files.createTempDirectory("g09bad2");
            Files.writeString(dir.resolve("s.txt"), "G09-S001|Alice Mwesigwa|MANAGER|UNASSIGNED\n");
            Files.writeString(dir.resolve("a.txt"), "G09-A001|G09-S999|2026-10-04|false||Sick\n");
            StaffService s = new StaffService(dir.resolve("s.txt").toString(), dir.resolve("a.txt").toString());
            s.load(); expect(s.getAllRecords().size() == 1, "size " + s.getAllRecords().size()); });
        check("TC31", "removing staff also removes their attendance", () -> {
            StaffService s = roster(); s.recordAbsence("G09-S005", D, "Sick"); s.removeStaff("G09-S005");
            expect(s.getAllRecords().size() == 8, "size " + s.getAllRecords().size()); });
        check("TC32", "polymorphism: ArrayList<Record> holds Staff + AttendanceEntry; describe() differs", () -> {
            StaffService s = roster(); s.recordAbsence("G09-S005", D, "Sick");
            ArrayList<Record> all = s.getAllRecords();
            boolean st = false, at = false;
            PrintStream old = System.out; ByteArrayOutputStream buf = new ByteArrayOutputStream();
            System.setOut(new PrintStream(buf));
            for (Record record : all) { System.out.println(record.describe()); st |= record instanceof Staff; at |= record instanceof AttendanceEntry; }
            System.setOut(old);
            String out = buf.toString();
            expect(st && at, "both types"); expect(out.contains("STAFF ") && out.contains("ATTENDANCE "), out); });
        System.out.println("\nRESULT: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
