package ug.ac.vu.g09.visitors;

import java.util.ArrayList;
import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Console menu for the visitors module. All input goes through InputHelper.
 * @author Josemaria Karitani Wamala
 */
public class VisitorMenu {
    private final VisitorService service;
    private final TenantService tenants;
    private final InputHelper input;

    public VisitorMenu(VisitorService service, TenantService tenants, InputHelper input) {
        this.service = service;
        this.tenants = tenants;
        this.input = input;
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n--- Visitors ---");
            System.out.println("1. Sign in a visitor");
            System.out.println("2. Sign out a visitor");
            System.out.println("3. Ban a visitor");
            System.out.println("4. Visits by time in");
            System.out.println("5. Visitors still inside");
            System.out.println("6. Show visits and bans (describe)");
            System.out.println("0. Back");
            choice = input.readInt("Choose: ", 0, 6);
            switch (choice) {
                case 1: signIn(); break;
                case 2: signOut(); break;
                case 3: ban(); break;
                case 4: print(service.getSortedByTimeIn()); break;
                case 5: print(service.getStillInside()); break;
                case 6: describeAll(); break;
                default: break;
            }
        } while (choice != 0);
    }

    private void signIn() {
        try {
            Tenant t = tenants.findTenantById(input.readText("Tenant ID (G09-T...): "));
            if (t == null) {
                System.out.println("Tenant not found.");
                return;
            }
            VisitRecord v = new VisitRecord(
                    service.nextVisitId(),
                    input.readText("Visitor name: "),
                    input.readText("Visitor ID number: "),
                    t,
                    input.readText("Date (yyyy-MM-dd): "),
                    input.readText("Time in (HH:mm): "));
            String night = input.readOptional("Overnight? Manager name or press Enter for no: ");
            if (!night.trim().isEmpty()) v.approveOvernight(night);
            service.addVisit(v);
            System.out.println("Signed in as " + v.getId());
        } catch (VisitorNotAllowedException e) {
            System.out.println("Not allowed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private void signOut() {
        try {
            String id = input.readText("Visit ID: ");
            String time = input.readText("Time out (HH:mm): ");
            System.out.println(service.signOut(id, time) ? "Signed out." : "No open visit with that ID.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private void ban() {
        try {
            BannedVisitor b = new BannedVisitor(
                    service.nextBanId(),
                    input.readText("Visitor name: "),
                    input.readText("Visitor ID number: "),
                    input.readText("Reason: "),
                    input.readText("Banned by (manager, secretary or guard): "));
            service.ban(b);
            System.out.println("Banned as " + b.getId());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private void print(ArrayList<VisitRecord> list) {
        if (list.isEmpty()) System.out.println("Nothing to show.");
        for (VisitRecord v : list) System.out.println(v.describe());
    }

    /** Polymorphism: one loop over Record holding VisitRecord and BannedVisitor. */
    private void describeAll() {
        ArrayList<Record> all = new ArrayList<>();
        all.addAll(service.getSortedByTimeIn());
        all.addAll(service.getBanned());
        for (Record r : all) System.out.println(r.describe());
    }
}
