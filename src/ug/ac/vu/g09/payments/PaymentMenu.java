package ug.ac.vu.g09.payments;

import java.util.ArrayList;
import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * The rent payments submenu that the secretary uses.
 * @author Mande Roland
 */
public class PaymentMenu {
    private static final double MAX_AMOUNT = 100000000;

    private PaymentService service;
    private TenantService tenants;
    private RoomService rooms;
    private InputHelper input;

    public PaymentMenu(PaymentService service, TenantService tenants, RoomService rooms, InputHelper input) {
        this.service = service;
        this.tenants = tenants;
        this.rooms = rooms;
        this.input = input;
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== Rent Payments ===");
            System.out.println("1. Record a rent payment");
            System.out.println("2. Record a late fee");
            System.out.println("3. Find a payment by ID");
            System.out.println("4. Add money to a payment");
            System.out.println("5. Remove a payment");
            System.out.println("6. List all payments");
            System.out.println("7. Debtors report");
            System.out.println("8. Payments of one tenant");
            System.out.println("0. Back to main menu");
            int choice = input.readInt("Choose an option: ", 0, 8);
            switch (choice) {
                case 1:
                    addRent();
                    break;
                case 2:
                    addLateFee();
                    break;
                case 3:
                    findOne();
                    break;
                case 4:
                    topUp();
                    break;
                case 5:
                    removeOne();
                    break;
                case 6:
                    listAll();
                    break;
                case 7:
                    showDebtors();
                    break;
                case 8:
                    showForTenant();
                    break;
                default:
                    running = false;
                    break;
            }
        }
    }

    private void addRent() {
        Tenant tenant = askTenant();
        if (tenant == null) {
            return;
        }
        Room room = askRoom();
        if (room == null) {
            return;
        }
        double fee = room.getFeePerPerson();
        System.out.println("Room fee per person: " + Payment.money(fee) + " UGX");
        double discount = input.readDouble("Discount in UGX (0 if none, up to 50,000): ", 0, 50000);
        double amountDue = fee - discount;
        System.out.println("Amount due this semester: " + Payment.money(amountDue) + " UGX");
        String date = input.readDate("Payment date (yyyy-mm-dd): ");
        String due = input.readDate("Due date for the rest (yyyy-mm-dd): ");
        double paid = input.readDouble("Amount paid now: ", 0, MAX_AMOUNT);
        String method = askMethod();
        String transactionId = askTransactionId(method);
        try {
            Payment p = new Payment(service.nextId(), service.nextReceipt(), tenant, room,
                    date, due, amountDue, paid, method);
            p.setTransactionId(transactionId);
            service.addPayment(p);
            System.out.println("Saved. Receipt " + p.getName() + ", ID " + p.getId());
        } catch (InvalidPaymentException e) {
            System.out.println("Not saved: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Not saved: " + e.getMessage());
        }
    }

    private void addLateFee() {
        Tenant tenant = askTenant();
        if (tenant == null) {
            return;
        }
        Room room = askRoom();
        if (room == null) {
            return;
        }
        int weeks = input.readInt("Weeks late (1 to 16): ", 1, 16);
        String date = input.readDate("Payment date (yyyy-mm-dd): ");
        String due = input.readDate("Due date for the rest (yyyy-mm-dd): ");
        double paid = input.readDouble("Amount paid now: ", 0, MAX_AMOUNT);
        String method = askMethod();
        String transactionId = askTransactionId(method);
        try {
            LateFeePayment p = new LateFeePayment(service.nextId(), service.nextReceipt(), tenant, room,
                    date, due, weeks, paid, method);
            p.setTransactionId(transactionId);
            service.addPayment(p);
            System.out.println("Saved. Receipt " + p.getName() + ", ID " + p.getId());
        } catch (InvalidPaymentException e) {
            System.out.println("Not saved: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("Not saved: " + e.getMessage());
        }
    }

    private void findOne() {
        String id = input.readText("Payment ID: ");
        Payment p = service.findById(id);
        if (p == null) {
            System.out.println("No payment with that ID.");
        } else {
            System.out.println(p.describe());
        }
    }

    private void topUp() {
        String id = input.readText("Payment ID: ");
        double extra = input.readDouble("Amount to add: ", 1, MAX_AMOUNT);
        try {
            if (service.updatePaid(id, extra)) {
                System.out.println("Updated: " + service.findById(id).describe());
            } else {
                System.out.println("No payment with that ID.");
            }
        } catch (InvalidPaymentException e) {
            System.out.println("Not updated: " + e.getMessage());
        }
    }

    private void removeOne() {
        String id = input.readText("Payment ID to remove: ");
        Payment p = service.findById(id);
        if (p == null) {
            System.out.println("No payment with that ID.");
            return;
        }
        System.out.println(p.describe());
        int sure = input.readInt("Type 1 to remove it, 2 to cancel: ", 1, 2);
        if (sure == 1) {
            service.removePayment(id);
            System.out.println("Removed.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // rent payments and late fees sit in the same list, each one describes itself
    private void listAll() {
        ArrayList<Payment> all = service.getAll();
        if (all.isEmpty()) {
            System.out.println("No payments recorded yet.");
            return;
        }
        double total = 0;
        for (Payment p : all) {
            System.out.println(p.describe());
            total += p.getPaymentAmount();
        }
        System.out.println(all.size() + " payment(s). Total due: " + Payment.money(total) + " UGX");
    }

    private void showDebtors() {
        ArrayList<Payment> debtors = service.getDebtors();
        if (debtors.isEmpty()) {
            System.out.println("Nobody owes money.");
            return;
        }
        System.out.printf("%-20s %-8s %-14s %12s  %s%n", "Name", "Room", "Phone", "Balance", "Due date");
        for (Payment p : debtors) {
            System.out.printf("%-20s %-8s %-14s %12s  %s%n", p.getTenant().getName(), p.getRoom().getName(),
                    p.getTenant().getPhone(), Payment.money(p.getBalance()), p.getDueDate());
        }
    }

    private void showForTenant() {
        Tenant tenant = askTenant();
        if (tenant == null) {
            return;
        }
        ArrayList<Payment> list = service.getForTenant(tenant.getId());
        if (list.isEmpty()) {
            System.out.println(tenant.getName() + " has no payments yet.");
            return;
        }
        for (Payment p : list) {
            System.out.println(p.describe());
        }
    }

    private Tenant askTenant() {
        System.out.println("Tenants:");
        for (Tenant t : tenants.getAllTenants()) {
            System.out.println("  " + t.getId() + "  " + t.getName());
        }
        String id = input.readText("Tenant ID: ");
        Tenant tenant = tenants.findTenantById(id);
        if (tenant == null) {
            System.out.println("No tenant with that ID.");
        }
        return tenant;
    }

    private Room askRoom() {
        String number = input.readText("Room number (for example B205): ").toUpperCase();
        Room room = rooms.findRoomByNumber(number);
        if (room == null) {
            System.out.println("No room with that number.");
        }
        return room;
    }

    // only MTN and Airtel payments have a mobile money transaction ID
    private String askTransactionId(String method) {
        if (!method.equals("MTN") && !method.equals("Airtel")) {
            return "";
        }
        while (true) {
            String id = input.readText("Mobile money transaction ID (6 to 20 letters or digits): ");
            if (Payment.isValidTransactionId(id)) {
                return id;
            }
            System.out.println("The ID must be 6 to 20 letters or digits.");
        }
    }

    private String askMethod() {
        String[] methods = Payment.getMethods();
        for (int i = 0; i < methods.length; i++) {
            System.out.println((i + 1) + ". " + methods[i]);
        }
        int pick = input.readInt("Payment method: ", 1, methods.length);
        return methods[pick - 1];
    }
}
