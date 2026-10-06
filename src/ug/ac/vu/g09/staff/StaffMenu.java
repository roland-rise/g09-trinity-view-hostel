package ug.ac.vu.g09.staff;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import ug.ac.vu.g09.core.InputHelper;

/** Console submenu for the Staff and Shifts module. Called from MainMenu.
 * @author Douth Nhial
 */
public class StaffMenu {

    private final StaffService service;
    private final InputHelper input;

    public StaffMenu(StaffService service, InputHelper input) {
        this.service = service;
        this.input = input;
    }

    public void run() {
        try { service.load(); }
        catch (IOException e) { System.out.println("Warning: could not load staff data - " + e.getMessage()); }

        boolean running = true;
        while (running) {
            System.out.println("\n=== STAFF & SHIFTS ===");
            System.out.println("1. Add staff          6. Record attendance");
            System.out.println("2. View all staff     7. Record absence");
            System.out.println("3. Find staff         8. View absentees by date");
            System.out.println("4. Update staff       9. Staff-by-role report");
            System.out.println("5. Remove staff      10. Shift management");
            System.out.println("11. Show all records (polymorphism demo)");
            System.out.println("0. Save and back");
            int choice = input.readInt("Choice: ", 0, 11);
            try {
                switch (choice) {
                    case 1:  add(); break;
                    case 2:  for (Staff s : service.getAllStaff()) System.out.println(s.describe()); break;
                    case 3:  find(); break;
                    case 4:  update(); break;
                    case 5:  service.removeStaff(input.readText("Staff ID: ")); System.out.println("Removed."); break;
                    case 6:  attendance(); break;
                    case 7:  absence(); break;
                    case 8:  System.out.println(service.absenteeReport(readDate())); break;
                    case 9:  for (Staff s : service.staffSortedByRole()) System.out.println(s.describe()); break;
                    case 10: shifts(); break;
                    case 11: service.describeAll(); break;
                    case 0:  running = false; break;
                    default: System.out.println("Choose 0-11.");
                }
            } catch (ShiftClashException e) {
                System.out.println("SHIFT ERROR: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("INVALID INPUT: " + e.getMessage());
            }
        }
        try { service.save(); System.out.println("Staff data saved."); }
        catch (IOException e) { System.out.println("ERROR saving staff data: " + e.getMessage()); }
    }

    private void add() {
        Staff s = service.addStaff(input.readText("Full name: "),
                                   input.readText("Role (Manager/Custodian/Security/Cleaner/Secretary): "));
        System.out.print("Added: "); s.describe();
    }

    private void find() {
        String q = input.readText("Staff ID or part of name: ");
        try { service.findStaffById(q).describe(); return; }
        catch (IllegalArgumentException ignored) { /* not an ID, try name */ }
        if (service.findStaffByName(q).isEmpty()) System.out.println("No match.");
        for (Staff s : service.findStaffByName(q)) s.describe();
    }

    private void update() {
        service.updateStaff(input.readText("Staff ID: "),
            input.readOptional("New name (blank = keep): "),
            input.readOptional("New role (blank = keep): "));
        System.out.println("Updated.");
    }

    private void attendance() {
        String id = input.readText("Staff ID: ");
        LocalDate d = readDate();
        LocalTime t = readTime();
        System.out.println(service.recordAttendance(id, d, t).describe());
    }

    private void absence() {
        String id = input.readText("Staff ID: ");
        LocalDate d = readDate();
        System.out.println(service.recordAbsence(id, d, input.readText("Absence reason: ")).describe());
    }

    private void shifts() throws ShiftClashException {
        System.out.println("a) Assign shift  b) Swap two guards  c) Check coverage");
        String c = input.readText("Option: ").toLowerCase();
        if (c.equals("a")) {
            String mgr = input.readText("Manager ID (authorisation): ");
            service.assignShift(input.readText("Security staff ID: "),
                                input.readText("Shift (Day/Night): "), mgr);
            System.out.println("Shift assigned.");
        } else if (c.equals("b")) {
            String mgr = input.readText("Manager ID (authorisation): ");
            service.swapShifts(input.readText("First guard ID: "),
                               input.readText("Second guard ID: "), mgr);
            System.out.println("Shifts swapped.");
        } else if (c.equals("c")) {
            service.validateSecurityCoverage();
            System.out.println("OK: 1 guard on Day, 2 on Night.");
        } else {
            System.out.println("Choose a, b or c.");
        }
    }

    private LocalDate readDate() {
        while (true) {
            try { return LocalDate.parse(input.readText("Date (dd/MM/yyyy): "), AttendanceEntry.DATE_FMT); }
            catch (DateTimeParseException e) { System.out.println("Invalid date. Example: 04/10/2026"); }
        }
    }

    private LocalTime readTime() {
        while (true) {
            try { return LocalTime.parse(input.readText("Sign-in time (HH:mm): "), AttendanceEntry.TIME_FMT); }
            catch (DateTimeParseException e) { System.out.println("Invalid time. Example: 07:45"); }
        }
    }
}
