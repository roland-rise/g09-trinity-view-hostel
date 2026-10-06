package ug.ac.vu.g09.bookings;
import ug.ac.vu.g09.core.InputHelper;
public class BookingMenu {
    private BookingService service;
    private InputHelper input;

    public BookingMenu(BookingService service, InputHelper input) {
        this.service = service;
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
            
            // Forces input processing safely through the shared core helper
            choice = input.readInt("Select an option: ", 1, 4);

            if (choice == 1) {
                for (Booking b : service.getSortedByCheckIn()) System.out.println(b.describe());
            } else if (choice == 3) {
                service.printFinancialSummary();
            }
        } while (choice != 4);
    }
}
