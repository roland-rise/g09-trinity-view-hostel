package ug.ac.vu.g09.bookings;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * @author Nayiga Patricia
 */
public class Deposit extends Record implements Payable {
    private Tenant tenant;
    private final double AMOUNT = 40000.0; // Fixed refundable deposit
    private String status; // "Held", "Refunded", "Forfeited"

    public Deposit(String id, Tenant tenant) {
        super(id, tenant.getName()); // Deposit name holds tenant's name
        if (!id.startsWith("G09-D")) {
            throw new IllegalArgumentException("Deposit ID must start with G09-D");
        }
        this.tenant = tenant;
        this.status = "Held";
    }

    public void refund() {
        // Business Rule: Refunded only if there's no damage or unpaid bills
        this.status = "Refunded";
    }

    public void forfeit() {
        this.status = "Forfeited";
    }

    @Override
    public double getPaymentAmount() {
        return this.AMOUNT;
    }

    @Override
    public String describe() {
        return "Security Deposit " + getId() + " for Tenant: " + getName() + " | Status: " + status;
    }

    public String toFileLine() {
        return getId() + "|" + tenant.getId() + "|" + AMOUNT + "|" + status;
    }
}
