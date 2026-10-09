package ug.ac.vu.g09.bookings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Keeps the bookings and deposits, and saves them to a file.
 * @author Nayiga Patricia
 */
public class BookingService {
    private static final String FOLDER = "data";
    private static final String FILE_NAME = "data/bookings.txt";

    private ArrayList<Booking> bookings = new ArrayList<>();
    private ArrayList<Deposit> deposits = new ArrayList<>();

    public void addBooking(Booking b) {
        if (findBooking(b.getId()) != null) {
            throw new IllegalArgumentException("That booking ID is already used");
        }
        bookings.add(b);
        saveToFile();
    }

    public void addDeposit(Deposit d) {
        if (findDeposit(d.getId()) != null) {
            throw new IllegalArgumentException("That deposit ID is already used");
        }
        deposits.add(d);
        saveToFile();
    }

    public Booking findBooking(String id) {
        for (Booking b : bookings) {
            if (b.getId().equalsIgnoreCase(id)) {
                return b;
            }
        }
        return null;
    }

    public Deposit findDeposit(String id) {
        for (Deposit d : deposits) {
            if (d.getId().equalsIgnoreCase(id)) {
                return d;
            }
        }
        return null;
    }

    public boolean payBooking(String id, double amount) {
        Booking b = findBooking(id);
        if (b == null) {
            return false;
        }
        try {
            b.pay(amount);
            saveToFile();
            return true;
        } catch (BookingLapsedException e) {
            System.out.println("Error: " + e.getMessage());
            saveToFile();
            return false;
        }
    }

    // saves the new state after a deposit was refunded or forfeited
    public void depositChanged() {
        saveToFile();
    }

    public boolean removeBooking(String id) {
        Booking b = findBooking(id);
        if (b == null) {
            return false;
        }
        bookings.remove(b);
        saveToFile();
        return true;
    }

    // report: sorted by check-in date with a Comparator
    public ArrayList<Booking> getSortedByCheckIn() {
        ArrayList<Booking> sorted = new ArrayList<>(bookings);
        sorted.sort(new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                return b1.getCheckInDate().compareTo(b2.getCheckInDate());
            }
        });
        return sorted;
    }

    // one loop over bookings and deposits as Payable, each answers for itself
    public void printFinancialSummary() {
        ArrayList<Payable> financialRecords = new ArrayList<>();
        financialRecords.addAll(bookings);
        financialRecords.addAll(deposits);
        System.out.println("=== POLYMORPHIC FINANCIAL REPORT ===");
        if (financialRecords.isEmpty()) {
            System.out.println("No bookings or deposits yet.");
            return;
        }
        double total = 0;
        for (Payable record : financialRecords) {
            System.out.println(String.format("Amount to pay: UGX %,.0f", record.getPaymentAmount()));
            total += record.getPaymentAmount();
        }
        System.out.println(String.format("%d record(s). Total: UGX %,.0f", financialRecords.size(), total));
    }

    public String nextBookingId() {
        int top = 0;
        for (Booking b : bookings) {
            top = Math.max(top, numberIn(b.getId()));
        }
        return "G09-B" + String.format("%03d", top + 1);
    }

    public String nextDepositId() {
        int top = 0;
        for (Deposit d : deposits) {
            top = Math.max(top, numberIn(d.getId()));
        }
        return "G09-D" + String.format("%03d", top + 1);
    }

    private int numberIn(String id) {
        try {
            return Integer.parseInt(id.substring(5));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public void saveToFile() {
        new File(FOLDER).mkdirs();
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Booking b : bookings) {
                out.println("BOOKING|" + b.toFileLine());
            }
            for (Deposit d : deposits) {
                out.println("DEPOSIT|" + d.toFileLine());
            }
        } catch (IOException e) {
            System.out.println("File error while saving bookings: " + e.getMessage());
        }
    }

    // a missing file means a fresh start, and a damaged line is skipped
    public void loadFromFile(RoomService rooms, TenantService tenants) {
        bookings.clear();
        deposits.clear();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }
        int skipped = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while (line != null) {
                if (!line.trim().isEmpty() && !loadLine(line, rooms, tenants)) {
                    skipped++;
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            System.out.println("Could not read " + FILE_NAME + ". Starting with no bookings.");
        }
        if (skipped > 0) {
            System.out.println("Skipped " + skipped + " damaged line(s) in " + FILE_NAME + ".");
        }
    }

    private boolean loadLine(String line, RoomService rooms, TenantService tenants) {
        String[] p = line.split("\\|", -1);
        try {
            if (p[0].equals("BOOKING") && p.length == 9) {
                Room room = rooms.findRoom(p[4]);
                if (room == null) {
                    return false;
                }
                Booking b = new Booking(p[1], p[2], p[3], room, p[5], p[6]);
                b.restore(Double.parseDouble(p[7]), p[8]);
                bookings.add(b);
                return true;
            }
            if (p[0].equals("DEPOSIT") && p.length == 5) {
                Tenant t = tenants.findTenantById(p[2]);
                if (t == null) {
                    return false;
                }
                Deposit d = new Deposit(p[1], t);
                d.restoreStatus(p[4]);
                deposits.add(d);
                return true;
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }
}
