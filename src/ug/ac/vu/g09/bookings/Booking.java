package ug.ac.vu.g09.bookings;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.rooms.Room;
import java.time.LocalDate;
public class Booking extends Record implements Payable {
    private String phone;
    private Room room;
    private LocalDate bookingDate;
    private LocalDate checkInDate;
    private final double FEE = 50000.0; // Fixed non-refundable fee
    private double amountPaid;
    private String status; // "Pending", "Paid", "Lapsed"

    public Booking(String id, String bookerName, String phone, Room room, String bookingDateStr, String checkInDateStr) {
        super(id, bookerName); // Passes ID and Name to the shared Record class
        if (!id.startsWith("G09-B")) {
            throw new IllegalArgumentException("Booking ID must start with G09-B");
        }
        this.phone = phone;
        this.room = room;
        this.bookingDate = LocalDate.parse(bookingDateStr);
        this.checkInDate = LocalDate.parse(checkInDateStr);
        this.amountPaid = 0.0;
        this.status = "Pending";
    }

    public void pay(double amount) throws BookingLapsedException {
        if (isLapsed(LocalDate.now())) {
            this.status = "Lapsed";
            throw new BookingLapsedException("Cannot pay! This booking has lapsed past 3 days.");
        }
        this.amountPaid += amount;
        if (this.amountPaid >= FEE) {
            this.status = "Paid";
        }
    }

    public boolean isLapsed(LocalDate today) {
        if ("Paid".equals(this.status)) return false;
        // Checks if current date is more than 3 days after the booking registration date
        return today.isAfter(bookingDate.plusDays(3));
    }

    @Override
    public double getPaymentAmount() {
        return this.amountPaid;
    }

    @Override
    public String describe() {
        return "Booking " + getId() + " for " + getName() + " | Status: " + status + " | Room: " + room.getName();
    }

    public String toFileLine() {
        return getId() + "|" + getName() + "|" + phone + "|" + room.getId() + "|" + bookingDate + "|" + checkInDate + "|" + amountPaid + "|" + status;
    }
}
