package ug.ac.vu.g09.rooms;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.tenants.Tenant;

import java.util.ArrayList;
import java.util.List;

/**
 * A single room in Trinity View Hostel.
 * Capacity is 1 (single), 2 (double) or 3 (triple).
 * Holds its Block and the list of current occupants.
 * @author Nsubuga Abdul
 */
public class Room extends Record {

    private String type;            // single | double | triple
    private int capacity;           // 1, 2 or 3
    private double feePerPerson;    // UGX
    private boolean selfContained;
    private String status;          // available, partly occupied, full, booked, maintenance, locked
    private Block block;
    private final ArrayList<Tenant> occupants = new ArrayList<>();

    public Room(String id, String roomNumber, String type, boolean selfContained, Block block) {
        super(id, roomNumber);
        setType(type);              // also sets capacity & fee
        this.selfContained = selfContained;
        this.status = "available";
        setBlock(block);
    }

    // ---- type / capacity / fee linked ----
    public String getType() { return type; }

    public void setType(String type) {
        if (type == null) throw new IllegalArgumentException("type null");
        String t = type.trim().toLowerCase();
        switch (t) {
            case "single":
                this.type = "single";
                this.capacity = 1;
                this.feePerPerson = 1_200_000;
                break;
            case "double":
                this.type = "double";
                this.capacity = 2;
                this.feePerPerson = 750_000;
                break;
            case "triple":
                this.type = "triple";
                this.capacity = 3;
                this.feePerPerson = 550_000;
                break;
            default:
                throw new IllegalArgumentException("type must be single, double or triple");
        }
    }

    public int getCapacity() { return capacity; }
    public double getFeePerPerson() { return feePerPerson; }
    public boolean isSelfContained() { return selfContained; }

    public void setSelfContained(boolean sc) { this.selfContained = sc; }

    public String getStatus() { return status; }

    public void setStatus(String status) {
        if (status == null) throw new IllegalArgumentException("status null");
        String s = status.trim().toLowerCase();
        switch (s) {
            case "available":
            case "partly occupied":
            case "full":
            case "booked":
            case "maintenance":
            case "locked":
                this.status = s;
                break;
            default:
                throw new IllegalArgumentException("unknown status: " + status);
        }
    }

    public Block getBlock() { return block; }

    public void setBlock(Block block) {
        if (block == null) throw new IllegalArgumentException("block required");
        this.block = block;
    }

    public List<Tenant> getOccupants() {
        return new ArrayList<>(occupants);
    }

    public int freeBeds() {
        return capacity - occupants.size();
    }

    /**
     * Adds a tenant. Checks capacity and gender rule.
     * @throws RoomFullException if no free bed
     * @throws IllegalArgumentException if gender mismatch
     */
    public void addOccupant(Tenant tenant) throws RoomFullException {
        if (tenant == null) {
            throw new IllegalArgumentException("A tenant is needed.");
        }
        if (occupants.contains(tenant)) {
            throw new IllegalArgumentException("Tenant " + tenant.getName() + " is already in this room.");
        }
        // client rule: a blocked tenant cannot be given a bed
        if (tenant.isBlocked()) {
            throw new IllegalArgumentException("Tenant " + tenant.getName() + " is blocked and cannot be given a bed.");
        }
        // client rule J1: men and women are kept in separate blocks
        if (block != null && !block.allows(tenant.getGender())) {
            throw new IllegalArgumentException("Tenant " + tenant.getName() + " (" + tenant.getGender()
                    + ") cannot be placed in block " + block.getName() + ", which is for " + block.getGenderRule() + " only.");
        }
        if (occupants.size() >= capacity) {
            throw new RoomFullException("Room " + getName() + " is full (" + capacity + " beds).");
        }
        occupants.add(tenant);
        refreshStatus();
    }

    // used when data is loaded, so the rules are not checked a second time
    public void restoreOccupant(Tenant tenant) {
        if (tenant != null && !occupants.contains(tenant)) {
            occupants.add(tenant);
            refreshStatus();
        }
    }

    public void removeOccupant(Tenant tenant) {
        occupants.remove(tenant);
        refreshStatus();
    }

    private void refreshStatus() {
        if (occupants.isEmpty()) status = "available";
        else if (occupants.size() >= capacity) status = "full";
        else status = "partly occupied";
    }

    @Override
    public String describe() {
        return String.format("Room[%s] %s type=%s beds=%d/%d fee=%.0f status=%s block=%s",
                getId(), getName(), type, occupants.size(), capacity, feePerPerson, status,
                block == null ? "?" : block.getName());
    }

    @Override
    public String toFileLine() {
        // id|roomNumber|type|selfContained|status|blockId
        String bid = block == null ? "NONE" : block.getId();
        return String.join("|",
                getId(),
                getName(),
                type,
                String.valueOf(selfContained),
                status,
                bid);
    }

    public static Room fromLine(String line, Block block) {
        String[] p = line.split("\\|", -1);
        if (p.length < 5) throw new IllegalArgumentException("bad room line");
        Room r = new Room(p[0], p[1], p[2], Boolean.parseBoolean(p[3]), block);
        r.setStatus(p[4]);
        return r;
    }
}
