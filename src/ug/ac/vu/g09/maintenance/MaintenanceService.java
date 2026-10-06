package ug.ac.vu.g09.maintenance;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.rooms.RoomService;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Keeps all maintenance requests and fundis in two ArrayLists and saves them to files.
 * One file line per record, fields separated by the pipe symbol.
 *
 * @author Mutebi Herbert
 */
public class MaintenanceService {

    private ArrayList<MaintenanceRequest> requests = new ArrayList<MaintenanceRequest>();
    private ArrayList<Fundi> fundis = new ArrayList<Fundi>();

    private TenantService tenants;
    private RoomService rooms;
    private String requestFile;
    private String fundiFile;

    /** Uses the default file names inside a data folder. */
    public MaintenanceService(TenantService tenants, RoomService rooms) {
        this(tenants, rooms, "data/maintenance_requests.txt", "data/fundis.txt");
    }

    /** Lets the caller choose the files (used by the tests). */
    public MaintenanceService(TenantService tenants, RoomService rooms, String requestFile, String fundiFile) {
        this.tenants = tenants;
        this.rooms = rooms;
        this.requestFile = requestFile;
        this.fundiFile = fundiFile;
    }

    // ---------- requests: add, find, update, remove ----------

    public void addRequest(MaintenanceRequest r) {
        if (findRequest(r.getId()) != null) {
            throw new IllegalArgumentException("A request with ID " + r.getId() + " already exists");
        }
        requests.add(r);
        saveToFile();
    }

    /** Returns the request with this ID, or null when there is none. */
    public MaintenanceRequest findRequest(String id) {
        for (MaintenanceRequest r : requests) {
            if (r.getId().equalsIgnoreCase(id)) {
                return r;
            }
        }
        return null;
    }

    /** Moves a request to its next stage and saves. */
    public void advanceStage(String id) throws InvalidStageException {
        MaintenanceRequest r = findRequest(id);
        if (r == null) {
            throw new IllegalArgumentException("No request with ID " + id);
        }
        r.nextStage();
        saveToFile();
    }

    /** Assigns a fundi to a request (it must be at the inspected stage) and saves. */
    public void assignFundi(String requestId, String fundiId) throws InvalidStageException {
        MaintenanceRequest r = findRequest(requestId);
        if (r == null) {
            throw new IllegalArgumentException("No request with ID " + requestId);
        }
        Fundi f = findFundi(fundiId);
        if (f == null) {
            throw new IllegalArgumentException("No fundi with ID " + fundiId);
        }
        r.assignFundi(f);
        saveToFile();
    }

    public boolean removeRequest(String id) {
        MaintenanceRequest r = findRequest(id);
        if (r == null) {
            return false;
        }
        requests.remove(r);
        saveToFile();
        return true;
    }

    // ---------- fundis ----------

    public void addFundi(Fundi f) {
        if (findFundi(f.getId()) != null) {
            throw new IllegalArgumentException("A fundi with ID " + f.getId() + " already exists");
        }
        fundis.add(f);
        saveToFile();
    }

    public Fundi findFundi(String id) {
        for (Fundi f : fundis) {
            if (f.getId().equalsIgnoreCase(id)) {
                return f;
            }
        }
        return null;
    }

    public ArrayList<Fundi> getFundis() {
        return new ArrayList<Fundi>(fundis);
    }

    // ---------- reports ----------

    /** Report: open requests sorted by level (urgent first); same level, oldest date first. */
    public ArrayList<MaintenanceRequest> getByPriority() {
        ArrayList<MaintenanceRequest> open = new ArrayList<MaintenanceRequest>();
        for (MaintenanceRequest r : requests) {
            if (r.isOpen()) {
                open.add(r);
            }
        }
        Collections.sort(open, new Comparator<MaintenanceRequest>() {
            @Override
            public int compare(MaintenanceRequest a, MaintenanceRequest b) {
                if (a.getLevel() != b.getLevel()) {
                    return a.getLevel() - b.getLevel();
                }
                int byDate = a.getRequestDate().compareTo(b.getRequestDate());
                if (byDate != 0) {
                    return byDate;
                }
                return a.getId().compareTo(b.getId());
            }
        });
        return open;
    }

    /** Requests and fundis in one list of Record, for the polymorphism loop in the menu. */
    public ArrayList<Record> getAllRecords() {
        ArrayList<Record> all = new ArrayList<Record>();
        all.addAll(requests);
        all.addAll(fundis);
        return all;
    }

    public int countRequests() {
        return requests.size();
    }

    // ---------- ID generation ----------

    public String nextRequestId() {
        int max = 0;
        for (MaintenanceRequest r : requests) {
            max = Math.max(max, numberPart(r.getId()));
        }
        return String.format("G09-M%03d", max + 1);
    }

    public String nextFundiId() {
        int max = 0;
        for (Fundi f : fundis) {
            max = Math.max(max, numberPart(f.getId()));
        }
        return String.format("G09-F%03d", max + 1);
    }

    private int numberPart(String id) {
        try {
            return Integer.parseInt(id.substring(5));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ---------- file storage ----------

    /**
     * Reads both files. A missing file just means there is nothing saved yet.
     * A damaged line is skipped, so a bad file never crashes the program.
     */
    public void loadFromFile() {
        requests.clear();
        fundis.clear();
        int skipped = loadFundis() + loadRequests();
        if (skipped > 0) {
            System.out.println("Warning: " + skipped + " damaged line(s) in the maintenance files were skipped.");
        }
    }

    private int loadFundis() {
        int skipped = 0;
        File file = new File(fundiFile);
        if (!file.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    String[] p = line.split("\\|", -1);
                    Fundi f = new Fundi(p[0], p[1], p[2], p[3]);
                    if (findFundi(f.getId()) == null) {
                        fundis.add(f);
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    skipped++;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + fundiFile + ": " + e.getMessage());
        }
        return skipped;
    }

    private int loadRequests() {
        int skipped = 0;
        File file = new File(requestFile);
        if (!file.exists()) {
            return 0;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    String[] p = line.split("\\|", -1);
                    Tenant tenant = tenants.findTenantById(p[2]);
                    Room room = rooms.findRoom(p[3]);
                    Fundi fundi = p[8].equals("NONE") ? null : findFundi(p[8]);
                    if (tenant == null || room == null || (fundi == null && !p[8].equals("NONE"))) {
                        skipped++;   // refers to a tenant, room or fundi that no longer exists
                        continue;
                    }
                    MaintenanceRequest r = new MaintenanceRequest(p[0], p[1], tenant, room,
                            p[4], p[5], Integer.parseInt(p[6]), p[7], fundi);
                    if (findRequest(r.getId()) == null) {
                        requests.add(r);
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    skipped++;
                }
            }
        } catch (IOException e) {
            System.out.println("Could not read " + requestFile + ": " + e.getMessage());
        }
        return skipped;
    }

    /** Writes both lists to their files. Called after every change. */
    public void saveToFile() {
        writeLines(fundiFile, true);
        writeLines(requestFile, false);
    }

    private void writeLines(String path, boolean forFundis) {
        File file = new File(path);
        File folder = file.getParentFile();
        if (folder != null && !folder.exists()) {
            folder.mkdirs();
        }
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            if (forFundis) {
                for (Fundi f : fundis) {
                    writer.println(f.toFileLine());
                }
            } else {
                for (MaintenanceRequest r : requests) {
                    writer.println(r.toFileLine());
                }
            }
        } catch (IOException e) {
            System.out.println("Could not save " + path + ": " + e.getMessage());
        }
    }
}
