package ug.ac.vu.g09.staff;

import ug.ac.vu.g09.core.Record;

/** A staff member: full name, role and (for security) a shift. No salary data is stored.
 * @author Douth Nhial
 */
public class Staff extends Record {

    public enum Role {
        MANAGER("Manager", 1, 0),
        CLEANER("Cleaner", 2, 1),
        CUSTODIAN("Custodian", 2, 2),
        SECURITY("Security", 3, 3),
        SECRETARY("Secretary", 1, 4);

        private final String label;
        private final int quota;      // number of posts the client wants
        private final int reportRank; // order used by the staff-by-role report

        Role(String label, int quota, int reportRank) {
            this.label = label; this.quota = quota; this.reportRank = reportRank;
        }
        public String getLabel() { return label; }
        public int getQuota() { return quota; }
        public int getReportRank() { return reportRank; }
        @Override public String toString() { return label; }

        public static Role parse(String text) {
            if (text != null) {
                for (Role r : values()) {
                    if (r.label.equalsIgnoreCase(text.trim())) return r;
                }
            }
            throw new IllegalArgumentException(
                "Invalid role '" + text + "'. Use: Manager, Cleaner, Custodian, Security, Secretary.");
        }
    }

    public enum Shift {
        UNASSIGNED("Unassigned", 0),
        DAY("Day (6:00 AM - 6:00 PM)", 1),
        NIGHT("Night (6:00 PM - 6:00 AM)", 2);

        private final String label;
        private final int required; // exact security headcount required
        Shift(String label, int required) { this.label = label; this.required = required; }
        public int getRequired() { return required; }
        @Override public String toString() { return label; }

        public static Shift parse(String text) {
            if (text != null) {
                for (Shift s : values()) {
                    if (s.name().equalsIgnoreCase(text.trim())) return s;
                }
            }
            throw new IllegalArgumentException("Invalid shift '" + text + "'. Use: Day or Night.");
        }
    }

    private Role role;
    private Shift shift = Shift.UNASSIGNED;

    public Staff(String id, String fullName, Role role) {
        super(id, fullName);
        setFullName(fullName);
        setRole(role);
    }

    public String getFullName() { return name; }
    public Role getRole() { return role; }
    public Shift getShift() { return shift; }

    public void setFullName(String fullName) {
        if (fullName == null || !fullName.trim().matches("[\\p{L}][\\p{L} .'-]*")) {
            throw new IllegalArgumentException(
                "Full name is required and may only contain letters, spaces, . ' and -.");
        }
        this.name = fullName.trim();
    }

    public void setRole(Role role) {
        if (role == null) throw new IllegalArgumentException("Role is required.");
        this.role = role;
        if (role != Role.SECURITY) this.shift = Shift.UNASSIGNED; // only security work shifts
    }

    void setShift(Shift shift) { this.shift = shift; } // package-private: service enforces rules

    @Override
    public String describe() {
        return "STAFF       " + getId() + " | " + name + " | " + role
            + (role == Role.SECURITY ? " | Shift: " + shift : "");
    }

    public String toFileLine() {
        return getId() + "|" + name + "|" + role.name() + "|" + shift.name();
    }

    public static Staff fromFileLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 4) throw new IllegalArgumentException("Bad staff line: " + line);
        Staff s = new Staff(p[0], p[1], Role.valueOf(p[2]));
        s.shift = Shift.valueOf(p[3]);
        return s;
    }
}
