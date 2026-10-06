package ug.ac.vu.g09.maintenance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Block;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Plain Java test runner for the maintenance module (no JUnit needed).
 * Each check prints PASS or FAIL.
 *
 * @author Mutebi Herbert
 */
public class MaintenanceTest {

    private static int passed = 0;
    private static int failed = 0;

    private static Tenant sarah = new Tenant("G09-T001", "Akello Sarah", "Female", "0770000001", 2);
    private static Room b205 = new Room("G09-R001", "B205", "double", true, new Block("G09-K002", "B", "MALE"));

    private static void check(String id, String what, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS " + id + "  " + what);
        } else {
            failed++;
            System.out.println("FAIL " + id + "  " + what);
        }
    }

    private static MaintenanceRequest sample(int level) {
        return new MaintenanceRequest("G09-M001", "Leaking tap", sarah, b205, "2026-03-14", "plumbing", level);
    }

    private static MaintenanceService freshService(String folder) {
        // the real services keep their own files, so start each test from empty ones
        new File("tenants.txt").delete();
        new File("clearances.txt").delete();
        new File("rooms.txt").delete();
        new File("blocks.txt").delete();
        TenantService ts = new TenantService();
        ts.addTenant(sarah);
        RoomService rs = new RoomService();
        rs.addRoom(b205);
        return new MaintenanceService(ts, rs, folder + "/requests.txt", folder + "/fundis.txt");
    }

    public static void main(String[] args) throws Exception {
        File temp = new File(System.getProperty("java.io.tmpdir"), "g09_maint_test_" + System.nanoTime());
        temp.mkdirs();
        String folder = temp.getPath();

        // ---- days to fix ----
        check("TC01", "level 1 gives 1 day", sample(1).getDaysToFix() == 1);
        check("TC02", "level 2 gives 3 days", sample(2).getDaysToFix() == 3);
        check("TC03", "level 3 gives 7 days", sample(3).getDaysToFix() == 7);

        // ---- stages in order ----
        MaintenanceRequest r = sample(1);
        check("TC04", "new request starts at reported", r.getStage().equals("reported") && r.isOpen());
        boolean walked = true;
        try {
            r.nextStage();                       // logged
            r.nextStage();                       // inspected
            r.assignFundi(new Fundi("G09-F001", "Mukasa John", "plumbing", "0772123456")); // assigned
            r.nextStage();                       // done
        } catch (InvalidStageException e) {
            walked = false;
        }
        check("TC05", "stages go reported-logged-inspected-assigned-done", walked && r.getStage().equals("done") && !r.isOpen());

        boolean threw = false;
        try { r.nextStage(); } catch (InvalidStageException e) { threw = true; }
        check("TC06", "moving past done throws InvalidStageException", threw);

        threw = false;
        try { sample(1).moveToStage("inspected"); } catch (InvalidStageException e) { threw = true; }
        check("TC07", "skipping a stage throws InvalidStageException", threw);

        threw = false;
        MaintenanceRequest early = sample(1);
        try { early.assignFundi(new Fundi("G09-F001", "Mukasa John", "plumbing", "0772123456")); }
        catch (InvalidStageException e) { threw = true; }
        check("TC08", "assigning a fundi before inspection throws InvalidStageException", threw);

        threw = false;
        MaintenanceRequest noFundi = sample(1);
        try { noFundi.nextStage(); noFundi.nextStage(); noFundi.nextStage(); }
        catch (InvalidStageException e) { threw = true; }
        check("TC09", "inspected to assigned without a fundi throws InvalidStageException", threw);

        // ---- invalid input ----
        check("TC10", "level 5 is rejected", rejects(5, "plumbing", "G09-M001", "Leaking tap", "2026-03-14"));
        check("TC11", "level 0 is rejected", rejects(0, "plumbing", "G09-M001", "Leaking tap", "2026-03-14"));
        check("TC12", "category 'roof' is rejected", rejects(1, "roof", "G09-M001", "Leaking tap", "2026-03-14"));
        check("TC13", "ID not starting with G09-M is rejected", rejects(1, "plumbing", "M001", "Leaking tap", "2026-03-14"));
        check("TC14", "empty problem title is rejected", rejects(1, "plumbing", "G09-M001", "   ", "2026-03-14"));
        check("TC15", "bad date '14/03/2026' is rejected", rejects(1, "plumbing", "G09-M001", "Leaking tap", "14/03/2026"));
        check("TC16", "month 13 is rejected", rejects(1, "plumbing", "G09-M001", "Leaking tap", "2026-13-01"));

        threw = false;
        try { new MaintenanceRequest("G09-M002", "Broken door", null, b205, "2026-03-14", "doors", 2); }
        catch (IllegalArgumentException e) { threw = true; }
        check("TC17", "null tenant is rejected", threw);

        threw = false;
        try { new Fundi("G09-F001", "Mukasa John", "plumbing", "12345"); }
        catch (IllegalArgumentException e) { threw = true; }
        check("TC18", "bad fundi phone number is rejected", threw);

        check("TC19", "category is stored in lower case", new MaintenanceRequest("G09-M003", "Light off",
                sarah, b205, "2026-03-14", "ELECTRICITY", 2).getCategory().equals("electricity"));
        check("TC20", "pipe symbol in title is replaced so the file stays valid",
                new MaintenanceRequest("G09-M004", "Tap | sink", sarah, b205, "2026-03-14", "plumbing", 3)
                        .getName().equals("Tap / sink"));

        // ---- service: add, find, remove, duplicates ----
        MaintenanceService s = freshService(folder);
        s.addRequest(new MaintenanceRequest("G09-M001", "Leaking tap", sarah, b205, "2026-03-14", "plumbing", 3));
        s.addRequest(new MaintenanceRequest("G09-M002", "Sparking socket", sarah, b205, "2026-03-15", "electricity", 1));
        s.addRequest(new MaintenanceRequest("G09-M003", "Stuck window", sarah, b205, "2026-03-10", "windows", 2));
        s.addRequest(new MaintenanceRequest("G09-M004", "Loose door handle", sarah, b205, "2026-03-12", "doors", 1));
        check("TC21", "findRequest returns the right request", s.findRequest("G09-M002") != null
                && s.findRequest("g09-m002") != null && s.findRequest("G09-M999") == null);

        threw = false;
        try { s.addRequest(new MaintenanceRequest("G09-M001", "Duplicate", sarah, b205, "2026-03-14", "other", 2)); }
        catch (IllegalArgumentException e) { threw = true; }
        check("TC22", "duplicate request ID is rejected", threw);
        check("TC23", "nextRequestId continues after the highest ID", s.nextRequestId().equals("G09-M005"));

        // ---- report: open requests sorted by level ----
        s.addFundi(new Fundi("G09-F001", "Mukasa John", "plumbing", "0772123456"));
        s.addFundi(new Fundi("G09-F002", "Ssemakula Paul", "electricity", "+256701234567"));
        ArrayList<MaintenanceRequest> byPriority = s.getByPriority();
        String order = "";
        for (MaintenanceRequest x : byPriority) { order += x.getId() + " "; }
        check("TC24", "open requests sorted by level, then oldest date (M004 M002 M003 M001)",
                order.trim().equals("G09-M004 G09-M002 G09-M003 G09-M001"));

        // finish M002 completely, it must disappear from the open report
        try {
            s.advanceStage("G09-M002");
            s.advanceStage("G09-M002");
            s.assignFundi("G09-M002", "G09-F002");
            s.advanceStage("G09-M002");
        } catch (InvalidStageException e) {
            System.out.println("unexpected: " + e.getMessage());
        }
        check("TC25", "a done request is left out of the open report", s.getByPriority().size() == 3
                && s.findRequest("G09-M002").getStage().equals("done"));

        threw = false;
        try { s.advanceStage("G09-M999"); } catch (IllegalArgumentException e) { threw = true; }
        catch (InvalidStageException e) { }
        check("TC26", "advancing an unknown request ID is rejected", threw);

        // ---- polymorphism: one loop over Record ----
        ArrayList<Record> all = s.getAllRecords();
        boolean allDescribe = all.size() == 6;
        for (Record rec : all) {
            if (rec.describe() == null || !rec.describe().startsWith("G09-")) { allDescribe = false; }
        }
        check("TC27", "one loop over Record calls describe() on requests and fundis", allDescribe);

        // ---- file storage ----
        MaintenanceService reloaded = freshService(folder);
        reloaded.loadFromFile();
        check("TC28", "saved data loads back (4 requests, 2 fundis)",
                reloaded.countRequests() == 4 && reloaded.getFundis().size() == 2);
        MaintenanceRequest back = reloaded.findRequest("G09-M002");
        check("TC29", "stage and fundi survive a save and load",
                back != null && back.getStage().equals("done") && back.getFundi() != null
                        && back.getFundi().getId().equals("G09-F002"));

        check("TC30", "removeRequest returns true then false",
                s.removeRequest("G09-M004") && !s.removeRequest("G09-M004"));
        MaintenanceService afterRemove = freshService(folder);
        afterRemove.loadFromFile();
        check("TC31", "a removal is saved to the file", afterRemove.countRequests() == 3);

        // missing file
        MaintenanceService missing = freshService(folder + "/does_not_exist");
        missing.loadFromFile();
        check("TC32", "missing file does not crash and starts empty", missing.countRequests() == 0);

        // damaged file
        File bad = new File(folder + "/bad");
        bad.mkdirs();
        try (PrintWriter w = new PrintWriter(new FileWriter(bad.getPath() + "/requests.txt"))) {
            w.println("G09-M001|Leaking tap|G09-T001|G09-R001|2026-03-14|plumbing|2|reported|NONE");
            w.println("this line is rubbish");
            w.println("G09-M002|Bad level|G09-T001|G09-R001|2026-03-14|plumbing|9|reported|NONE");
            w.println("G09-M003|Unknown tenant|G09-T777|G09-R001|2026-03-14|doors|2|reported|NONE");
            w.println("G09-M004|Done with no fundi|G09-T001|G09-R001|2026-03-14|doors|2|done|NONE");
            w.println("");
        } catch (IOException e) {
            System.out.println("could not build damaged file");
        }
        MaintenanceService damaged = freshService(bad.getPath());
        damaged.loadFromFile();
        check("TC33", "damaged lines are skipped and the good line is kept",
                damaged.countRequests() == 1 && damaged.findRequest("G09-M001") != null);

        System.out.println();
        System.out.println("Result: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static boolean rejects(int level, String category, String id, String title, String date) {
        try {
            new MaintenanceRequest(id, title, sarah, b205, date, category, level);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
