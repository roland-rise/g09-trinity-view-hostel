package ug.ac.vu.g09.visitors;

import ug.ac.vu.g09.core.Record;

/**
 * A visitor who is not allowed into the hostel. Name holds the visitor's name.
 * @author Josemaria Karitani Wamala
 */
public class BannedVisitor extends Record {
    private String idNumber;
    private String reason;
    private String bannedBy;

    public BannedVisitor(String id, String name, String idNumber, String reason, String bannedBy) {
        super(id, name);
        setIdNumber(idNumber);
        setReason(reason);
        setBannedBy(bannedBy);
    }

    public String getIdNumber() { return idNumber; }
    public String getReason() { return reason; }
    public String getBannedBy() { return bannedBy; }

    public void setIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty())
            throw new IllegalArgumentException("ID number is required");
        this.idNumber = idNumber.trim();
    }

    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty())
            throw new IllegalArgumentException("Reason is required");
        this.reason = reason.trim();
    }

    /** Only the manager, secretary or security guard can ban a visitor. */
    public void setBannedBy(String bannedBy) {
        if (bannedBy == null || bannedBy.trim().isEmpty())
            throw new IllegalArgumentException("Who banned this visitor is required");
        this.bannedBy = bannedBy.trim();
    }

    @Override
    public String describe() {
        return "BANNED " + getId() + ": " + getName() + " (ID " + idNumber + "), reason: "
                + reason + ", banned by " + bannedBy;
    }

    public String toFileLine() {
        return getId() + "|" + getName() + "|" + idNumber + "|" + reason + "|" + bannedBy;
    }

    public static BannedVisitor fromFileLine(String line) {
        String[] p = line.split("\\|", -1);
        return new BannedVisitor(p[0], p[1], p[2], p[3], p[4]);
    }
}
