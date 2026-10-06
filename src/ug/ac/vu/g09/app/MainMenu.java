package ug.ac.vu.g09.app;

import ug.ac.vu.g09.core.InputHelper;
import ug.ac.vu.g09.payments.PaymentMenu;
import ug.ac.vu.g09.payments.PaymentService;
import java.io.File;
import ug.ac.vu.g09.bookings.BookingMenu;
import ug.ac.vu.g09.bookings.BookingService;
import ug.ac.vu.g09.maintenance.MaintenanceMenu;
import ug.ac.vu.g09.maintenance.MaintenanceService;
import ug.ac.vu.g09.rooms.RoomMenu;
import ug.ac.vu.g09.visitors.VisitorMenu;
import ug.ac.vu.g09.visitors.VisitorService;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.staff.StaffMenu;
import ug.ac.vu.g09.staff.StaffService;
import ug.ac.vu.g09.tenants.TenantMenu;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * The main menu of the Trinity View Hostel system. It starts everything and joins the modules.
 * @author Mande Roland
 */
public class MainMenu {
    private InputHelper input;
    private TenantMenu tenantMenu;
    private RoomMenu roomMenu;
    private PaymentMenu paymentMenu;
    private StaffMenu staffMenu;
    private BookingMenu bookingMenu;
    private MaintenanceMenu maintenanceMenu;
    private VisitorMenu visitorMenu;

    public MainMenu() {
        new File("data").mkdirs();
        input = new InputHelper();
        TenantService tenants = new TenantService();
        RoomService rooms = new RoomService();
        PaymentService payments = new PaymentService();
        payments.loadFromFile(tenants, rooms);
        tenantMenu = new TenantMenu(tenants, rooms, input);
        roomMenu = new RoomMenu(rooms, tenants, input);
        staffMenu = new StaffMenu(new StaffService("data/staff.txt", "data/attendance.txt"), input);
        BookingService bookings = new BookingService();
        bookings.loadFromFile();
        bookingMenu = new BookingMenu(bookings, input);
        // the maintenance service does not load itself, so it is loaded here
        MaintenanceService maintenance = new MaintenanceService(tenants, rooms);
        maintenance.loadFromFile();
        maintenanceMenu = new MaintenanceMenu(maintenance, tenants, rooms, input);
        visitorMenu = new VisitorMenu(new VisitorService(tenants, "data/visits.txt", "data/banned.txt"), tenants, input);
        paymentMenu = new PaymentMenu(payments, tenants, rooms, input);
    }

    public void run() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== Trinity View Hostel ===");
            System.out.println("1. Tenants");
            System.out.println("2. Rooms");
            System.out.println("3. Rent payments");
            System.out.println("4. Bookings and deposits");
            System.out.println("5. Maintenance requests");
            System.out.println("6. Visitors");
            System.out.println("7. Staff and shifts");
            System.out.println("0. Exit");
            int choice = input.readInt("Choose an option: ", 0, 7);
            switch (choice) {
                case 1:
                    tenantMenu.run();
                    break;
                case 2:
                    roomMenu.run();
                    break;
                case 3:
                    paymentMenu.run();
                    break;
                case 4:
                    bookingMenu.run();
                    break;
                case 5:
                    maintenanceMenu.run();
                    break;
                case 6:
                    visitorMenu.run();
                    break;
                case 7:
                    staffMenu.run();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    // the other members' menus get plugged in here when their code is merged
                    System.out.println("That module is not connected yet.");
                    break;
            }
        }
        System.out.println("Goodbye.");
    }

    public static void main(String[] args) {
        new MainMenu().run();
    }
}
