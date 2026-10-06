package ug.ac.vu.g09.payments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Keeps all payments in a list, applies the payment rules and saves them to a file.
 * @author Mande Roland
 */
public class PaymentService {
    private static final String FOLDER = "data";
    private static final String FILE_NAME = "data/payments.txt";
    private static final String RECEIPT_PREFIX = "TVH/2026/";

    private ArrayList<Payment> payments;

    public PaymentService() {
        payments = new ArrayList<Payment>();
    }

    public void loadFromFile(TenantService tenants, RoomService rooms) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty()) {
                    Payment p = parseLine(line, tenants, rooms);
                    if (p == null || findById(p.getId()) != null) {
                        skipped++;
                    } else {
                        payments.add(p);
                    }
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            System.out.println("Could not read " + FILE_NAME + ". Starting with no payments.");
        }
        if (skipped > 0) {
            System.out.println("Skipped " + skipped + " damaged line(s) in " + FILE_NAME + ".");
        }
    }

    public void saveToFile() {
        new File(FOLDER).mkdirs();
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Payment p : payments) {
                writer.println(p.toFileLine());
            }
        } catch (IOException e) {
            System.out.println("Could not save payments: " + e.getMessage());
        }
    }

    public void addPayment(Payment p) throws InvalidPaymentException {
        if (findById(p.getId()) != null) {
            throw new InvalidPaymentException("That payment ID is already used.");
        }
        for (Payment existing : payments) {
            if (existing.getName().equals(p.getName())) {
                throw new InvalidPaymentException("That receipt number is already used.");
            }
        }
        // change request: mobile money payments must carry the transaction ID
        if (p.isMobileMoney() && p.getTransactionId().isEmpty()) {
            throw new InvalidPaymentException("A mobile money payment needs its transaction ID.");
        }
        // client rule: the first instalment is at least half of the amount
        double half = p.getPaymentAmount() / 2;
        if (p.getAmountPaid() < half) {
            throw new InvalidPaymentException("The first payment must be at least half of the amount due ("
                    + Payment.money(half) + " UGX).");
        }
        payments.add(p);
        saveToFile();
    }

    public Payment findById(String id) {
        for (Payment p : payments) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    public boolean updatePaid(String id, double extra) throws InvalidPaymentException {
        Payment p = findById(id);
        if (p == null) {
            return false;
        }
        if (extra <= 0) {
            throw new InvalidPaymentException("The amount must be above zero.");
        }
        if (p.getBalance() <= 0) {
            throw new InvalidPaymentException("This payment is already fully paid.");
        }
        p.pay(extra);
        saveToFile();
        return true;
    }

    public boolean removePayment(String id) {
        Payment p = findById(id);
        if (p == null) {
            return false;
        }
        payments.remove(p);
        saveToFile();
        return true;
    }

    public ArrayList<Payment> getAll() {
        return new ArrayList<Payment>(payments);
    }

    public ArrayList<Payment> getForTenant(String tenantId) {
        ArrayList<Payment> result = new ArrayList<Payment>();
        for (Payment p : payments) {
            if (p.getTenant().getId().equalsIgnoreCase(tenantId)) {
                result.add(p);
            }
        }
        return result;
    }

    // tenants who still owe money, the biggest debt first
    public ArrayList<Payment> getDebtors() {
        ArrayList<Payment> debtors = new ArrayList<Payment>();
        for (Payment p : payments) {
            if (p.getBalance() > 0) {
                debtors.add(p);
            }
        }
        Collections.sort(debtors, new Comparator<Payment>() {
            @Override
            public int compare(Payment a, Payment b) {
                return Double.compare(b.getBalance(), a.getBalance());
            }
        });
        return debtors;
    }

    public String nextId() {
        int highest = 0;
        for (Payment p : payments) {
            highest = Math.max(highest, numberAfter(p.getId(), "G09-P".length()));
        }
        return "G09-P" + String.format("%03d", highest + 1);
    }

    public String nextReceipt() {
        int highest = 0;
        for (Payment p : payments) {
            highest = Math.max(highest, numberAfter(p.getName(), RECEIPT_PREFIX.length()));
        }
        return RECEIPT_PREFIX + String.format("%03d", highest + 1);
    }

    // returns 0 when the text does not end in a number, for example an old or hand-edited line
    private int numberAfter(String text, int start) {
        try {
            return Integer.parseInt(text.substring(start));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    // returns null for any line that cannot be turned into a payment
    private Payment parseLine(String line, TenantService tenants, RoomService rooms) {
        String[] part = line.split("\\|");
        // old lines have 10 parts, new lines have the transaction ID as an 11th part
        if (part.length != 10 && part.length != 11) {
            return null;
        }
        Tenant tenant = tenants.findTenantById(part[3]);
        Room room = rooms.findRoomByNumber(part[4]);
        if (tenant == null || room == null) {
            return null;
        }
        try {
            double paid = Double.parseDouble(part[8]);
            Payment p = null;
            if (part[0].equals("RENT")) {
                p = new Payment(part[1], part[2], tenant, room, part[5], part[6],
                        Double.parseDouble(part[7]), paid, part[9]);
            }
            if (part[0].equals("LATE")) {
                p = new LateFeePayment(part[1], part[2], tenant, room, part[5], part[6],
                        Integer.parseInt(part[7]), paid, part[9]);
            }
            if (p != null && part.length == 11) {
                p.setTransactionId(part[10]);
            }
            return p;
        } catch (IllegalArgumentException e) {
            // a bad number or a rule broken in the file, so the line is skipped
            return null;
        }
    }
}
