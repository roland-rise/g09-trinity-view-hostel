package ug.ac.vu.g09.rooms;

import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

import java.util.List;

/**
 * Sub-menu for Rooms module.
 * @author Nsubuga Abdul
 */
public class RoomMenu {

    private final RoomService svc;
    private final TenantService tenants;
    private final InputHelper input;

    public RoomMenu(RoomService svc, TenantService tenants, InputHelper input) {
        this.svc = svc;
        this.tenants = tenants;
        this.input = input;
    }

    public void run() {
        boolean go = true;
        while (go) {
            System.out.println();
            System.out.println("===== ROOMS MODULE (Nsubuga Abdul) =====");
            System.out.println("1. Add room");
            System.out.println("2. Find room by number");
            System.out.println("3. List available rooms (most free beds first)");
            System.out.println("4. List all rooms");
            System.out.println("5. List blocks");
            System.out.println("6. Assign tenant (stub) to room");
            System.out.println("7. Set room status");
            System.out.println("8. Poly demo (Room + Block)");
            System.out.println("0. Exit module");
            int c = input.readInt("Select: ", 0, 8);
            switch (c) {
                case 1 -> addRoom();
                case 2 -> findRoom();
                case 3 -> listAvailable();
                case 4 -> listAll();
                case 5 -> listBlocks();
                case 6 -> assign();
                case 7 -> setStatus();
                case 8 -> svc.polyDemo();
                case 0 -> go = false;
                default -> System.out.println("bad choice");
            }
        }
    }

    private void addRoom() {
        try {
            String id = svc.nextRoomId();
            String number = input.readText("Room number (e.g. B205): ");
            String type = input.readText("Type (single/double/triple): ");
            boolean sc = input.readYesNo("Self-contained?");
            String letter = input.readText("Block letter (A/B/C/D): ");
            Block b = svc.findBlockByLetter(letter);
            if (b == null) {
                System.out.println("Unknown block. Creating it as male for now.");
                b = new Block(svc.nextBlockId(), letter.toUpperCase(), "Male");
                svc.addBlock(b);
            }
            Room r = new Room(id, number, type, sc, b);
            svc.addRoom(r);
            System.out.println("Added: " + r.describe());
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }

    private void findRoom() {
        String num = input.readText("Room number: ");
        Room r = svc.findRoomByNumber(num);
        if (r == null) System.out.println("not found");
        else System.out.println(r.describe());
    }

    private void listAvailable() {
        List<Room> list = svc.availableSortedByFreeBeds();
        if (list.isEmpty()) {
            System.out.println("No free beds right now.");
            return;
        }
        System.out.println("--- rooms with free beds ---");
        for (Room r : list) System.out.println(r.describe() + "  free=" + r.freeBeds());
    }

    private void listAll() {
        for (Room r : svc.allRooms()) System.out.println(r.describe());
    }

    private void listBlocks() {
        for (Block b : svc.allBlocks()) System.out.println(b.describe());
    }

    private void assign() {
        String roomId = input.readText("Room ID (G09-Rxxx): ");
        String tenantId = input.readText("Tenant ID (G09-Txxx): ");
        Tenant t = tenants.findTenantById(tenantId);
        if (t == null) {
            System.out.println("No tenant with that ID.");
            return;
        }
        try {
            svc.assignTenant(roomId, t);
            System.out.println("Tenant assigned.");
        } catch (RoomFullException e) {
            System.out.println("REJECTED: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void setStatus() {
        String id = input.readText("Room ID: ");
        Room r = svc.findRoom(id);
        if (r == null) {
            System.out.println("not found");
            return;
        }
        String st = input.readText("New status (available/partly occupied/full/booked/maintenance/locked): ");
        try {
            r.setStatus(st);
            svc.updateRoom(r);
            System.out.println("Status updated.");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
