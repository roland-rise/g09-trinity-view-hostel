package ug.ac.vu.g09.bookings;

import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * The bookings and deposits submenu.
 * @author Nayiga Patricia
 */
public class BookingMenu {
    private BookingService service;
    private TenantService tenants;
    private RoomService rooms;
    private InputHelper input;

    public BookingMenu(BookingService service, TenantService tenants, RoomService rooms, InputHelper input) {
        this.service = service;
        this.tenants = tenants;
        this.rooms = rooms;
        this.input = input;
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n--- TRINITY VIEW HOSTEL: BOOKINGS MENU ---");
            System.out.println("1. View Bookings Sorted by Check-in");
            System.out.println("2. Record Booking Payment");
            System.out.println("3. Run Polymorphic Revenue Report");
            System.out.println("4. Back to Main Menu");
            System.out.println("5. Add a Booking");
            System.out.println("6. Add a Deposit");
            System.out.println("7. Refund or Forfeit a Deposit");
            System.out.println("8. Find a Booking");
            System.out.println("9. Cancel a Booking");
            choice = input.readInt("Select an option: ", 1, 9);
            if (choice == 1) {
                listBookings();
            } else if (choice == 2) {
                payBooking();
            } else if (choice == 3) {
                service.printFinancialSummary();
            } else if (choice == 5) {
                addBooking();
            } else if (choice == 6) {
                addDeposit();
            } else if (choice == 7) {
                closeDeposit();
            } else if (choice == 8) {
                findBooking();
            } else if (choice == 9) {
                cancelBooking();
            }
        } while (choice != 4);
    }

    private void listBookings() {
        if (service.getSortedByCheckIn().isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        for (Booking b : service.getSortedByCheckIn()) {
            System.out.println(b.describe());
        }
    }

    private void addBooking() {
        String name = input.readText("Booker's name: ");
        String phone = input.readText("Phone number: ");
        String roomNumber = input.readText("Room number (for example B205): ").toUpperCase();
        Room room = rooms.findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("No room with that number.");
            return;
        }
        String bookingDate = input.readDate("Booking date (yyyy-mm-dd): ");
        String checkIn = input.readDate("Check-in date (yyyy-mm-dd): ");
        try {
            Booking b = new Booking(service.nextBookingId(), name, phone, room, bookingDate, checkIn);
            service.addBooking(b);
            System.out.println("Saved. " + b.getId() + ". The booking is held for 3 days until the 50,000 UGX fee is paid.");
        } catch (IllegalArgumentException e) {
            System.out.println("Not saved: " + e.getMessage());
        }
    }

    private void payBooking() {
        String id = input.readText("Booking ID: ");
        Booking b = service.findBooking(id);
        if (b == null) {
            System.out.println("No booking with that ID.");
            return;
        }
        double amount = input.readDouble("Amount paid: ", 1, 100000000);
        try {
            if (service.payBooking(id, amount)) {
                System.out.println("Payment recorded. " + b.describe());
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Not recorded: " + e.getMessage());
        }
    }

    private void addDeposit() {
        String tenantId = input.readText("Tenant ID: ");
        Tenant tenant = tenants.findTenantById(tenantId);
        if (tenant == null) {
            System.out.println("No tenant with that ID.");
            return;
        }
        try {
            Deposit d = new Deposit(service.nextDepositId(), tenant);
            service.addDeposit(d);
            System.out.println("Saved. " + d.describe());
        } catch (IllegalArgumentException e) {
            System.out.println("Not saved: " + e.getMessage());
        }
    }

    private void closeDeposit() {
        String id = input.readText("Deposit ID: ");
        Deposit d = service.findDeposit(id);
        if (d == null) {
            System.out.println("No deposit with that ID.");
            return;
        }
        int pick = input.readInt("1 = refund, 2 = forfeit: ", 1, 2);
        try {
            if (pick == 1) {
                boolean damage = input.readYesNo("Is there damage in the room?");
                boolean key = input.readYesNo("Is the key lost?");
                boolean bills = input.readYesNo("Are there unpaid bills?");
                d.refund(damage, key, bills);
            } else {
                d.forfeit();
            }
            service.depositChanged();
            System.out.println("Updated. " + d.describe());
        } catch (IllegalArgumentException e) {
            System.out.println("Not changed: " + e.getMessage());
        }
    }

    private void findBooking() {
        String id = input.readText("Booking ID: ");
        Booking b = service.findBooking(id);
        if (b == null) {
            System.out.println("No booking with that ID.");
        } else {
            System.out.println(b.describe());
        }
    }

    private void cancelBooking() {
        String id = input.readText("Booking ID to cancel: ");
        Booking b = service.findBooking(id);
        if (b == null) {
            System.out.println("No booking with that ID.");
            return;
        }
        System.out.println(b.describe());
        if (input.readYesNo("Cancel this booking?")) {
            service.removeBooking(id);
            System.out.println("Cancelled. The booking fee is not refundable.");
        } else {
            System.out.println("Kept.");
        }
    }
}
