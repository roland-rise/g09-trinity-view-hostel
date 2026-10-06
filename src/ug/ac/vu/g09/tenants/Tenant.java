package ug.ac.vu.g09.tenants;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;

/**
 * Represents a student tenant living at Trinity View Hostel.
 * Holds a reference to the allocated Room.
 * @author Mugira Grace
 */
public class Tenant extends Record {

    private String gender;          // Male or Female
    private String phone;
    private int yearOfStudy;        // 1 - 5
    private boolean blocked;        // true if unpaid balance or misconduct
    private Room room;              // allocated room (may be null)

    /**
     * Creates a new Tenant.
     * @param id must be of the form G09-Txxx
     * @param fullName tenant's full name
     * @param gender Male or Female
     * @param phone contact number (invented)
     * @param yearOfStudy between 1 and 5 inclusive
     * @throws IllegalArgumentException for invalid field values
     */
    public Tenant(String id, String fullName, String gender, String phone, int yearOfStudy) {
        super(id, fullName);
        setGender(gender);
        setPhone(phone);
        setYearOfStudy(yearOfStudy);
        this.blocked = false;
        this.room = null;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        if (gender == null) {
            throw new IllegalArgumentException("Gender cannot be null");
        }
        String g = gender.trim();
        if (!g.equalsIgnoreCase("Male") && !g.equalsIgnoreCase("Female")) {
            throw new IllegalArgumentException("Gender must be Male or Female");
        }
        this.gender = g.substring(0, 1).toUpperCase() + g.substring(1).toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        this.phone = phone.trim();
    }

    public int getYearOfStudy() {
        return yearOfStudy;
    }

    public void setYearOfStudy(int yearOfStudy) {
        if (yearOfStudy < 1 || yearOfStudy > 5) {
            throw new IllegalArgumentException("Year of study must be between 1 and 5");
        }
        this.yearOfStudy = yearOfStudy;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Room getRoom() {
        return room;
    }

    /**
     * Allocates this tenant to a room. Rejects if the tenant is blocked.
     * @param room the Room object
     * @throws TenantRuleException if tenant is blocked
     */
    public void allocateRoom(Room room) throws TenantRuleException {
        if (this.blocked) {
            throw new TenantRuleException(
                "Tenant " + getName() + " is blocked and cannot be allocated a room.");
        }
        this.room = room;
    }

    public void clearRoom() {
        this.room = null;
    }

    @Override
    public String describe() {
        String roomInfo = (room == null) ? "unallocated" : room.getName();
        String status = blocked ? "BLOCKED" : "active";
        return String.format("Tenant[%s] %s (%s, Year %d) - room: %s - %s",
                getId(), getName(), gender, yearOfStudy, roomInfo, status);
    }

    @Override
    public String toFileLine() {
        // id|name|gender|phone|year|blocked|roomId
        String roomId = (room == null) ? "NONE" : room.getId();
        return String.join("|",
                getId(),
                getName(),
                gender,
                phone,
                String.valueOf(yearOfStudy),
                String.valueOf(blocked),
                roomId);
    }

    /**
     * Rebuilds a Tenant from a pipe-separated file line.
     * Room reference is left null; service will re-link later if needed.
     */
    public static Tenant fromFileLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 6) {
            throw new IllegalArgumentException("Corrupt tenant line: " + line);
        }
        Tenant t = new Tenant(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]));
        t.setBlocked(Boolean.parseBoolean(p[5]));
        // room is re-attached by service if required
        return t;
    }
}
