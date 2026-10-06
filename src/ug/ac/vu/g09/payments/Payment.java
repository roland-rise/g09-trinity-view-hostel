package ug.ac.vu.g09.payments;

import java.util.Locale;
import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * One rent payment by a tenant for a room. The record name is the receipt number.
 * @author Mande Roland
 */
public class Payment extends Record implements Payable {
    private static final String[] METHODS = {"Cash", "MTN", "Airtel", "Bank"};

    private Tenant tenant;
    private Room room;
    private String paymentDate;
    private String dueDate;
    private double amountDue;
    private double amountPaid;
    private String method;
    private String transactionId = "";

    public Payment(String id, String receiptNo, Tenant tenant, Room room, String paymentDate,
                   String dueDate, double amountDue, double amountPaid, String method) {
        super(id, receiptNo);
        if (tenant == null || room == null) {
            throw new IllegalArgumentException("A payment needs a tenant and a room");
        }
        if (amountDue <= 0) {
            throw new IllegalArgumentException("Amount due must be above zero");
        }
        if (amountPaid < 0) {
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        this.tenant = tenant;
        this.room = room;
        this.amountDue = amountDue;
        this.amountPaid = amountPaid;
        setPaymentDate(paymentDate);
        setDueDate(dueDate);
        setMethod(method);
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Room getRoom() {
        return room;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public String getMethod() {
        return method;
    }

    public void setPaymentDate(String paymentDate) {
        if (!InputHelper.isValidDate(paymentDate)) {
            throw new IllegalArgumentException("Payment date must look like yyyy-mm-dd");
        }
        this.paymentDate = paymentDate;
    }

    public void setDueDate(String dueDate) {
        if (!InputHelper.isValidDate(dueDate)) {
            throw new IllegalArgumentException("Due date must look like yyyy-mm-dd");
        }
        this.dueDate = dueDate;
    }

    public void setMethod(String method) {
        for (String allowed : METHODS) {
            if (allowed.equalsIgnoreCase(method)) {
                this.method = allowed;
                return;
            }
        }
        throw new IllegalArgumentException("Method must be Cash, MTN, Airtel or Bank");
    }

    public String getTransactionId() {
        return transactionId;
    }

    // empty means no transaction ID, which is only allowed for cash and bank payments
    public void setTransactionId(String transactionId) {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            this.transactionId = "";
            return;
        }
        if (!isValidTransactionId(transactionId.trim())) {
            throw new IllegalArgumentException("Transaction ID must be 6 to 20 letters or digits");
        }
        this.transactionId = transactionId.trim().toUpperCase();
    }

    public static boolean isValidTransactionId(String id) {
        return id != null && id.matches("[A-Za-z0-9]{6,20}");
    }

    public boolean isMobileMoney() {
        return method.equals("MTN") || method.equals("Airtel");
    }

    public static String[] getMethods() {
        return METHODS.clone();
    }

    public void pay(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be above zero");
        }
        amountPaid += amount;
    }

    // a negative balance means the tenant has paid too much, so it is kept as credit
    public double getBalance() {
        return getPaymentAmount() - amountPaid;
    }

    @Override
    public double getPaymentAmount() {
        return amountDue;
    }

    @Override
    public String describe() {
        double balance = getBalance();
        String balanceText;
        if (balance < 0) {
            balanceText = "Credit " + money(-balance);
        } else {
            balanceText = "Balance " + money(balance);
        }
        return id + " | " + name + " | " + tenant.getName() + " | Room " + room.getName()
                + " | Due " + money(getPaymentAmount()) + " | Paid " + money(amountPaid)
                + " | " + balanceText + " | " + method + refText() + " | " + paymentDate;
    }

    private String refText() {
        if (transactionId.isEmpty()) {
            return "";
        }
        return " (Ref " + transactionId + ")";
    }

    // the room number is saved, not the room id, because rooms are found by number
    public String toFileLine() {
        return "RENT|" + id + "|" + name + "|" + tenant.getId() + "|" + room.getName() + "|"
                + paymentDate + "|" + dueDate + "|" + amountDue + "|" + amountPaid + "|" + method + "|" + transactionId;
    }

    public static String money(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }
}
