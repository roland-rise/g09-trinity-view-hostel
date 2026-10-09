package ug.ac.vu.g09.bookings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import ug.ac.vu.g09.core.Payable;
import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;

/**
 * A room booking. The booking fee is not refundable and a booking is held for 3 days.
 * @author Nayiga Patricia
 */
public class Booking extends Record implements Payable {
    private static final double FEE = 50000.0;
    private static final int HOLD_DAYS = 3;

    private String phone;
    private Room room;
    private LocalDate bookingDate;
    private LocalDate checkInDate;
    private double amountPaid;
    private String status; // Pending, Paid or Lapsed

    public Booking(String id, String bookerName, String phone, Room room, String bookingDateStr, String checkInDateStr) {
        super(id, bookerName);
        if (!id.startsWith("G09-B")) {
            throw new IllegalArgumentException("Booking ID must start with G09-B");
        }
        if (phone == null || phone.trim().isEmpty() || phone.contains("|")) {
            throw new IllegalArgumentException("A phone number is needed and cannot contain |");
        }
        if (room == null) {
            throw new IllegalArgumentException("A booking needs a room");
        }
        this.phone = phone.trim();
        this.room = room;
        try {
            this.bookingDate = LocalDate.parse(bookingDateStr);
            this.checkInDate = LocalDate.parse(checkInDateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates must look like 2026-10-05");
        }
        if (checkInDate.isBefore(bookingDate)) {
            throw new IllegalArgumentException("The check-in date cannot be before the booking date");
        }
        this.amountPaid = 0.0;
        this.status = "Pending";
    }

    public Room getRoom() {
        return room;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public String getStatus() {
        return status;
    }

    public void pay(double amount) throws BookingLapsedException {
        if (amount <= 0) {
            throw new IllegalArgumentException("The amount must be above zero");
        }
        if (isLapsed(LocalDate.now())) {
            this.status = "Lapsed";
            throw new BookingLapsedException("Cannot pay. This booking has lapsed after " + HOLD_DAYS + " days.");
        }
        this.amountPaid += amount;
        if (this.amountPaid >= FEE) {
            this.status = "Paid";
        }
    }

    public boolean isLapsed(LocalDate today) {
        if ("Paid".equals(this.status)) {
            return false;
        }
        return today.isAfter(bookingDate.plusDays(HOLD_DAYS));
    }

    // used when a saved booking is loaded
    public void restore(double amountPaid, String status) {
        if (amountPaid < 0) {
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        this.amountPaid = amountPaid;
        this.status = status;
    }

    // the fee that has to be paid for this booking
    @Override
    public double getPaymentAmount() {
        return FEE;
    }

    @Override
    public String describe() {
        return "Booking " + getId() + " for " + getName() + " | Room: " + room.getName() + " | Check-in: " + checkInDate
                + " | Paid " + String.format("%,.0f", amountPaid) + " of " + String.format("%,.0f", FEE) + " | Status: " + status;
    }

    @Override
    public String toFileLine() {
        return getId() + "|" + getName() + "|" + phone + "|" + room.getId() + "|" + bookingDate + "|" + checkInDate
                + "|" + amountPaid + "|" + status;
    }
}
