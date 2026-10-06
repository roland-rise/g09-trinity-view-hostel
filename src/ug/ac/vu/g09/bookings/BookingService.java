package ug.ac.vu.g09.bookings;

import ug.ac.vu.g09.core.Payable;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.*;
/**
 * @author Nayiga Patricia
 */
public class BookingService {
    private ArrayList<Booking> bookings = new ArrayList<>();
    private ArrayList<Deposit> deposits = new ArrayList<>();

    public void addBooking(Booking b) { bookings.add(b); saveToFile(); }
    public void addDeposit(Deposit d) { deposits.add(d); saveToFile(); }

    public Booking findBooking(String id) {
        for (Booking b : bookings) {
            if (b.getId().equalsIgnoreCase(id)) return b;
        }
        return null;
    }

    public boolean payBooking(String id, double amount) {
        Booking b = findBooking(id);
        if (b != null) {
            try {
                b.pay(amount);
                saveToFile();
                return true;
            } catch (BookingLapsedException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
        return false;
    }

    // Required Report: Sorted by Check-In date using a Comparator
    public ArrayList<Booking> getSortedByCheckIn() {
        ArrayList<Booking> sorted = new ArrayList<>(bookings);
        sorted.sort(new Comparator<Booking>() {
            @Override
            public int compare(Booking b1, Booking b2) {
                return b1.toFileLine().split("\\|")[5].compareTo(b2.toFileLine().split("\\|")[5]); 
            }
        });
        return sorted;
    }

    // Required Mandatory Polymorphic Loop Processing Mixed Objects
    public void printFinancialSummary() {
        ArrayList<Payable> financialRecords = new ArrayList<>();
        financialRecords.addAll(bookings);
        financialRecords.addAll(deposits);

        System.out.println("=== POLYMORPHIC FINANCIAL REPORT ===");
        for (Payable record : financialRecords) {
            // Polymorphism in action: calling interface method regardless of actual object class
            System.out.println("Record Value Collected: UGX " + record.getPaymentAmount());
        }
    }

    public void saveToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter("bookings.txt"))) {
            for (Booking b : bookings) out.println("BOOKING|" + b.toFileLine());
            for (Deposit d : deposits) out.println("DEPOSIT|" + d.toFileLine());
        } catch (IOException e) {
            System.out.println("File error while saving data safely.");
        }
    }

    public void loadFromFile() {
        File file = new File("bookings.txt");
        if (!file.exists()) return; // Never crash if file is missing!
        // File parsing logic would be placed here by storage rules
    }
}