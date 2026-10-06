package ug.ac.vu.g09.maintenance;

import java.util.ArrayList;

import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Text menu for the maintenance module. All input goes through InputHelper.
 *
 * @author Mutebi Herbert
 */
public class MaintenanceMenu {

    private MaintenanceService service;
    private TenantService tenants;
    private RoomService rooms;
    private InputHelper input;

    public MaintenanceMenu(MaintenanceService service, TenantService tenants, RoomService rooms, InputHelper input) {
        this.input = input;
        this.service = service;
        this.tenants = tenants;
        this.rooms = rooms;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== MAINTENANCE REQUESTS =====");
            System.out.println("1. Report a new request");
            System.out.println("2. Move a request to its next stage");
            System.out.println("3. Assign a fundi to a request");
            System.out.println("4. Add a fundi");
            System.out.println("5. Report: open requests by priority");
            System.out.println("6. Find a request by ID");
            System.out.println("7. Remove a request");
            System.out.println("8. Show all requests and fundis");
            System.out.println("0. Back");
            int choice = input.readInt("Choose: ", 0, 8);

            try {
                switch (choice) {
                    case 1: reportRequest(); break;
                    case 2: nextStage(); break;
                    case 3: assignFundi(); break;
                    case 4: addFundi(); break;
                    case 5: showByPriority(); break;
                    case 6: findRequest(); break;
                    case 7: removeRequest(); break;
                    case 8: showAll(); break;
                    default: running = false;
                }
            } catch (InvalidStageException e) {
                System.out.println("Not allowed: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Not saved: " + e.getMessage());
            }
        }
    }

    private void reportRequest() {
        String title = input.readText("Short problem title: ");
        Tenant tenant = tenants.findTenantById(input.readText("Tenant ID (e.g. G09-T001): "));
        if (tenant == null) {
            System.out.println("No tenant with that ID. Register the tenant first.");
            return;
        }
        Room room = rooms.findRoom(input.readText("Room ID (e.g. G09-R001): "));
        if (room == null) {
            System.out.println("No room with that ID.");
            return;
        }
        String date = input.readDate("Date reported (yyyy-mm-dd): ");

        System.out.println("Category: 1 plumbing, 2 electricity, 3 doors, 4 windows, 5 other");
        int cat = input.readInt("Category number: ", 1, 5);
        int level = input.readInt("Level (1 urgent, 2 medium, 3 low): ", 1, 3);

        MaintenanceRequest r = new MaintenanceRequest(service.nextRequestId(), title, tenant, room,
                date, MaintenanceRequest.CATEGORIES[cat - 1], level);
        service.addRequest(r);
        System.out.println("Saved. " + r.describe());
    }

    private void nextStage() throws InvalidStageException {
        String id = input.readText("Request ID: ");
        service.advanceStage(id);
        System.out.println("Updated. " + service.findRequest(id).describe());
    }

    private void assignFundi() throws InvalidStageException {
        if (service.getFundis().isEmpty()) {
            System.out.println("There are no fundis yet. Add one first.");
            return;
        }
        for (Fundi f : service.getFundis()) {
            System.out.println("  " + f.describe());
        }
        String requestId = input.readText("Request ID: ");
        String fundiId = input.readText("Fundi ID: ");
        service.assignFundi(requestId, fundiId);
        System.out.println("Assigned. " + service.findRequest(requestId).describe());
    }

    private void addFundi() {
        String name = input.readText("Fundi full name: ");
        System.out.println("Trade: 1 plumbing, 2 electricity, 3 doors, 4 windows, 5 other");
        int trade = input.readInt("Trade number: ", 1, 5);
        String phone = input.readText("Phone (e.g. 0772123456): ");
        Fundi f = new Fundi(service.nextFundiId(), name, MaintenanceRequest.CATEGORIES[trade - 1], phone);
        service.addFundi(f);
        System.out.println("Saved. " + f.describe());
    }

    private void showByPriority() {
        ArrayList<MaintenanceRequest> open = service.getByPriority();
        if (open.isEmpty()) {
            System.out.println("There are no open requests.");
            return;
        }
        System.out.println("Open requests, most urgent first:");
        for (MaintenanceRequest r : open) {
            System.out.println("  " + r.describe());
        }
    }

    private void findRequest() {
        MaintenanceRequest r = service.findRequest(input.readText("Request ID: "));
        System.out.println(r == null ? "No request with that ID." : r.describe());
    }

    private void removeRequest() {
        String id = input.readText("Request ID to remove: ");
        System.out.println(service.removeRequest(id) ? "Removed." : "No request with that ID.");
    }

    /** One loop over Record objects: each object answers describe() in its own way. */
    private void showAll() {
        ArrayList<Record> all = service.getAllRecords();
        if (all.isEmpty()) {
            System.out.println("Nothing recorded yet.");
            return;
        }
        for (Record r : all) {
            System.out.println("  " + r.describe());
        }
    }
}
