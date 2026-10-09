package ug.ac.vu.g09.tenants;

import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomFullException;
import ug.ac.vu.g09.rooms.RoomService;

import java.util.List;

/**
 * Console submenu for the Tenants module.
 * All input is obtained exclusively through InputHelper.
 * @author Mugira Grace
 */
public class TenantMenu {

    private final TenantService service;
    private final RoomService rooms;
    private final InputHelper input;

    public TenantMenu(TenantService service, RoomService rooms, InputHelper input) {
        this.service = service;
        this.rooms = rooms;
        this.input = input;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== TENANTS MODULE (Mugira Grace) =====");
            System.out.println("1. Register new tenant");
            System.out.println("2. Find tenant by ID");
            System.out.println("3. List all tenants (sorted by name)");
            System.out.println("4. List blocked tenants");
            System.out.println("5. Block a tenant");
            System.out.println("6. Allocate room to tenant");
            System.out.println("7. Issue early-departure clearance");
            System.out.println("8. Demonstrate polymorphism");
            System.out.println("0. Back to main menu");
            int choice = input.readInt("Choice: ", 0, 8);

            switch (choice) {
                case 1 -> registerTenant();
                case 2 -> findTenant();
                case 3 -> listSorted();
                case 4 -> listBlocked();
                case 5 -> blockTenant();
                case 6 -> allocateRoom();
                case 7 -> issueClearance();
                case 8 -> service.demonstratePolymorphism();
                case 0 -> running = false;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void registerTenant() {
        try {
            String id = service.generateTenantId();
            String name = input.readText("Full name: ");
            String gender = input.readText("Gender (Male/Female): ");
            String phone = input.readText("Phone (invented): ");
            int year = input.readInt("Year of study (1-5): ", 1, 5);
            Tenant t = new Tenant(id, name, gender, phone, year);
            service.addTenant(t);
            System.out.println("Tenant registered successfully: " + t.describe());
        } catch (IllegalArgumentException e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void findTenant() {
        String id = input.readText("Tenant ID (e.g. G09-T001): ");
        Tenant t = service.findTenantById(id);
        if (t == null) {
            System.out.println("No tenant found with that ID.");
        } else {
            System.out.println(t.describe());
        }
    }

    private void listSorted() {
        List<Tenant> list = service.reportAllSortedByName();
        if (list.isEmpty()) {
            System.out.println("No tenants registered yet.");
            return;
        }
        System.out.println("--- All tenants (alphabetical) ---");
        for (Tenant t : list) {
            System.out.println(t.describe());
        }
    }

    private void listBlocked() {
        List<Tenant> list = service.reportBlockedTenants();
        if (list.isEmpty()) {
            System.out.println("No blocked tenants.");
            return;
        }
        System.out.println("--- Blocked tenants ---");
        for (Tenant t : list) {
            System.out.println(t.describe());
        }
    }

    private void blockTenant() {
        String id = input.readText("Tenant ID to block: ");
        String reason = input.readText("Reason (unpaid / misconduct): ");
        try {
            service.blockTenant(id, reason);
            System.out.println("Tenant blocked.");
        } catch (TenantRuleException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void allocateRoom() {
        String tenantId = input.readText("Tenant ID: ");
        String roomName = input.readText("Room number (e.g. B205): ").toUpperCase();
        Room room = rooms.findRoomByNumber(roomName);
        if (room == null) {
            System.out.println("No room with that number.");
            return;
        }
        Tenant tenant = service.findTenantById(tenantId);
        if (tenant == null) {
            System.out.println("Tenant not found.");
            return;
        }
        try {
            // the room checks gender, blocked tenants and space first
            rooms.assignTenant(room.getId(), tenant);
            service.allocateRoomToTenant(tenantId, room);
            System.out.println("Room allocated.");
        } catch (TenantRuleException e) {
            System.out.println("Allocation refused: " + e.getMessage());
        } catch (RoomFullException e) {
            System.out.println("Allocation refused: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Allocation refused: " + e.getMessage());
        }
    }

    private void issueClearance() {
        String tenantId = input.readText("Tenant ID leaving: ");
        Tenant t = service.findTenantById(tenantId);
        if (t == null) {
            System.out.println("Tenant not found.");
            return;
        }
        String reason = input.readText("Reason (early departure / misconduct): ");
        String date = input.readText("Date signed (e.g. 2026-05-15): ");
        String clearanceId = service.generateClearanceId();
        TenantClearance c = new TenantClearance(clearanceId, t.getName(), tenantId, reason, date);
        service.addClearance(c);
        t.clearRoom();
        service.updateTenant(t);
        System.out.println("Clearance issued: " + c.describe());
    }
}
