package ug.ac.vu.g09.visitors;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * One visit to a tenant. Name holds the visitor's name.
 * Dates are text as yyyy-MM-dd and times as HH:mm (zero padded), so they sort as text.
 * @author Josemaria Karitani Wamala
 */
public class VisitRecord extends Record {
    private String idNumber;
    private Tenant tenant;
    private String visitDate;
    private String timeIn;
    private String timeOut = "";
    private boolean overnight = false;
    private String approvedBy = "";

    public VisitRecord(String id, String name, String idNumber, Tenant tenant,
                       String visitDate, String timeIn) {
        super(id, name);
        setIdNumber(idNumber);
        setTenant(tenant);
        setVisitDate(visitDate);
        setTimeIn(timeIn);
    }

    public String getIdNumber() { return idNumber; }
    public Tenant getTenant() { return tenant; }
    public String getVisitDate() { return visitDate; }
    public String getTimeIn() { return timeIn; }
    public String getTimeOut() { return timeOut; }
    public boolean isOvernight() { return overnight; }
    public String getApprovedBy() { return approvedBy; }

    public void setIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty())
            throw new IllegalArgumentException("Visitor must show an ID");
        this.idNumber = idNumber.trim();
    }

    public void setTenant(Tenant tenant) {
        if (tenant == null) throw new IllegalArgumentException("Tenant is required");
        this.tenant = tenant;
    }

    public void setVisitDate(String d) {
        if (d == null || !d.matches("\\d{4}-\\d{2}-\\d{2}"))
            throw new IllegalArgumentException("Date must look like 2026-10-05");
        this.visitDate = d;
    }

    public void setTimeIn(String t) {
        checkTime(t);
        this.timeIn = t;
    }

    public void setTimeOut(String t) {
        checkTime(t);
        this.timeOut = t;
    }

    static void checkTime(String t) {
        if (t == null || !t.matches("([01]\\d|2[0-3]):[0-5]\\d"))
            throw new IllegalArgumentException("Time must look like 14:30");
    }

    public void signOut(String time) {
        if (!isInside()) throw new IllegalStateException("Visitor already signed out");
        setTimeOut(time);
    }

    public boolean isInside() {
        return timeOut.isEmpty();
    }

    /** Overnight stay needs the manager's approval, and we store who approved. */
    public void approveOvernight(String by) {
        if (by == null || by.trim().isEmpty())
            throw new IllegalArgumentException("Approver name is required");
        this.overnight = true;
        this.approvedBy = by.trim();
    }

    @Override
    public String describe() {
        String out = isInside() ? "still inside" : "out " + timeOut;
        String night = overnight ? ", overnight approved by " + approvedBy : "";
        return getId() + ": " + getName() + " visiting " + tenant.getName() + " on "
                + visitDate + ", in " + timeIn + ", " + out + night;
    }

    public String toFileLine() {
        return getId() + "|" + getName() + "|" + idNumber + "|" + tenant.getId() + "|"
                + visitDate + "|" + timeIn + "|" + timeOut + "|" + overnight + "|" + approvedBy;
    }
}
