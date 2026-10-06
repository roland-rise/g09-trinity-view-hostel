package ug.ac.vu.g09.payments;

import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * A late fee, charged per week after the first week of late rent.
 * @author Mande Roland
 */
public class LateFeePayment extends Payment {
    private static final double WEEKLY_FEE = 10000;
    private static final int MAX_WEEKS = 16;

    private int weeksLate;

    public LateFeePayment(String id, String receiptNo, Tenant tenant, Room room, String paymentDate,
                          String dueDate, int weeksLate, double amountPaid, String method) {
        super(id, receiptNo, tenant, room, paymentDate, dueDate, feeFor(weeksLate), amountPaid, method);
        this.weeksLate = weeksLate;
    }

    // has to be static because it runs before the parent constructor
    private static double feeFor(int weeks) {
        if (weeks < 1 || weeks > MAX_WEEKS) {
            throw new IllegalArgumentException("Weeks late must be from 1 to " + MAX_WEEKS);
        }
        return weeks * WEEKLY_FEE;
    }

    public int getWeeksLate() {
        return weeksLate;
    }

    @Override
    public String describe() {
        return "LATE FEE (" + weeksLate + " weeks) " + super.describe();
    }

    @Override
    public String toFileLine() {
        return "LATE|" + getId() + "|" + getName() + "|" + getTenant().getId() + "|"
                + getRoom().getName() + "|" + getPaymentDate() + "|" + getDueDate() + "|"
                + weeksLate + "|" + getAmountPaid() + "|" + getMethod();
    }
}
