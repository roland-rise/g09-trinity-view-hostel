package ug.ac.vu.g09.bookings;

import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * The refundable security deposit of a tenant.
 * @author Nayiga Patricia
 */
public class Deposit extends Record implements Payable {
    private static final double AMOUNT = 40000.0;

    private Tenant tenant;
    private String status; // Held, Refunded or Forfeited

    public Deposit(String id, Tenant tenant) {
        super(id, tenant == null ? "" : tenant.getName());
        if (!id.startsWith("G09-D")) {
            throw new IllegalArgumentException("Deposit ID must start with G09-D");
        }
        this.tenant = tenant;
        this.status = "Held";
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getStatus() {
        return status;
    }

    // client rule: refunded only if there is no damage, no lost key and no unpaid bill
    public void refund(boolean damageFound, boolean keyLost, boolean unpaidBills) {
        checkHeld();
        if (damageFound || keyLost || unpaidBills) {
            throw new IllegalArgumentException("The deposit cannot be refunded because of damage, a lost key or unpaid bills");
        }
        this.status = "Refunded";
    }

    public void forfeit() {
        checkHeld();
        this.status = "Forfeited";
    }

    private void checkHeld() {
        if (!"Held".equals(status)) {
            throw new IllegalArgumentException("This deposit is already " + status.toLowerCase());
        }
    }

    // used when a saved deposit is loaded
    public void restoreStatus(String status) {
        this.status = status;
    }

    @Override
    public double getPaymentAmount() {
        return AMOUNT;
    }

    @Override
    public String describe() {
        return "Security Deposit " + getId() + " for tenant " + getName() + " | Amount " + String.format("%,.0f", AMOUNT)
                + " | Status: " + status;
    }

    @Override
    public String toFileLine() {
        return getId() + "|" + tenant.getId() + "|" + AMOUNT + "|" + status;
    }
}
