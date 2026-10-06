package ug.ac.vu.g09.tenants;

import ug.ac.vu.g09.core.Record;

/**
 * Clearance form signed by a tenant who leaves early or is asked to leave.
 * No refund is given; the form simply records the departure.
 * @author Mugira Grace
 */
public class TenantClearance extends Record {

    private String tenantId;        // G09-Txxx of the departing tenant
    private String reason;          // e.g. "early departure", "misconduct"
    private String dateSigned;      // simple string date for storage

    /**
     * @param id must start with G09-C
     * @param tenantName name of the tenant (stored in name field)
     * @param tenantId the tenant's record id
     * @param reason why the clearance was issued
     * @param dateSigned date the form was signed
     */
    public TenantClearance(String id, String tenantName, String tenantId,
                           String reason, String dateSigned) {
        super(id, tenantName);
        setTenantId(tenantId);
        setReason(reason);
        setDateSigned(dateSigned);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        if (tenantId == null || !tenantId.startsWith("G09-T")) {
            throw new IllegalArgumentException("Clearance must reference a valid tenant id (G09-T...)");
        }
        this.tenantId = tenantId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason for clearance is required");
        }
        this.reason = reason.trim();
    }

    public String getDateSigned() {
        return dateSigned;
    }

    public void setDateSigned(String dateSigned) {
        if (dateSigned == null || dateSigned.trim().isEmpty()) {
            throw new IllegalArgumentException("Date signed is required");
        }
        this.dateSigned = dateSigned.trim();
    }

    @Override
    public String describe() {
        return String.format("Clearance[%s] for %s (tenant %s) - reason: %s - signed: %s",
                getId(), getName(), tenantId, reason, dateSigned);
    }

    @Override
    public String toFileLine() {
        return String.join("|",
                getId(),
                getName(),
                tenantId,
                reason,
                dateSigned);
    }

    public static TenantClearance fromFileLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 5) {
            throw new IllegalArgumentException("Corrupt clearance line: " + line);
        }
        return new TenantClearance(p[0], p[1], p[2], p[3], p[4]);
    }
}
