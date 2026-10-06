package ug.ac.vu.g09.tenants;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the collection of Tenant and TenantClearance records.
 * Provides add / find / update / remove, file persistence and reports.
 * @author Mugira Grace
 */
public class TenantService {

    private final ArrayList<Tenant> tenants = new ArrayList<>();
    private final ArrayList<TenantClearance> clearances = new ArrayList<>();
    private final String tenantFile = "tenants.txt";
    private final String clearanceFile = "clearances.txt";
    private int nextTenantNum = 1;
    private int nextClearanceNum = 1;

    public TenantService() {
        loadFromFile();
    }

    // ------------------------------------------------------------------
    // CRUD - Tenants
    // ------------------------------------------------------------------

    public void addTenant(Tenant t) {
        if (findTenantById(t.getId()) != null) {
            throw new IllegalArgumentException("Tenant id already exists: " + t.getId());
        }
        tenants.add(t);
        saveToFile();
    }

    public Tenant findTenantById(String id) {
        for (Tenant t : tenants) {
            if (t.getId().equals(id)) return t;
        }
        return null;
    }

    public Tenant findTenantByName(String name) {
        for (Tenant t : tenants) {
            if (t.getName().equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    public void updateTenant(Tenant updated) {
        for (int i = 0; i < tenants.size(); i++) {
            if (tenants.get(i).getId().equals(updated.getId())) {
                tenants.set(i, updated);
                saveToFile();
                return;
            }
        }
        throw new IllegalArgumentException("Tenant not found for update: " + updated.getId());
    }

    public boolean removeTenant(String id) {
        Tenant t = findTenantById(id);
        if (t == null) return false;
        tenants.remove(t);
        saveToFile();
        return true;
    }

    public List<Tenant> getAllTenants() {
        return new ArrayList<>(tenants);
    }

    // ------------------------------------------------------------------
    // CRUD - Clearances
    // ------------------------------------------------------------------

    public void addClearance(TenantClearance c) {
        clearances.add(c);
        saveToFile();
    }

    public List<TenantClearance> getAllClearances() {
        return new ArrayList<>(clearances);
    }

    // ------------------------------------------------------------------
    // Business helpers
    // ------------------------------------------------------------------

    public String generateTenantId() {
        String id;
        do {
            id = String.format("G09-T%03d", nextTenantNum++);
        } while (findTenantById(id) != null);
        return id;
    }

    public String generateClearanceId() {
        return String.format("G09-C%03d", nextClearanceNum++);
    }

    /**
     * Blocks a tenant (e.g. unpaid balance or misconduct).
     */
    public void blockTenant(String tenantId, String reason) throws TenantRuleException {
        Tenant t = findTenantById(tenantId);
        if (t == null) {
            throw new TenantRuleException("No tenant found with id " + tenantId);
        }
        t.setBlocked(true);
        updateTenant(t);
    }

    /**
     * Allocates a room to a tenant; throws if the tenant is blocked.
     */
    public void allocateRoomToTenant(String tenantId, Room room) throws TenantRuleException {
        Tenant t = findTenantById(tenantId);
        if (t == null) {
            throw new TenantRuleException("Tenant not found: " + tenantId);
        }
        t.allocateRoom(room);
        updateTenant(t);
    }

    // ------------------------------------------------------------------
    // Reports (Comparator)
    // ------------------------------------------------------------------

    /**
     * Returns all tenants sorted alphabetically by name.
     */
    public List<Tenant> reportAllSortedByName() {
        List<Tenant> copy = new ArrayList<>(tenants);
        copy.sort(Comparator.comparing(Tenant::getName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    /**
     * Returns only blocked tenants.
     */
    public List<Tenant> reportBlockedTenants() {
        List<Tenant> blocked = new ArrayList<>();
        for (Tenant t : tenants) {
            if (t.isBlocked()) blocked.add(t);
        }
        return blocked;
    }

    /**
     * Polymorphic demonstration: processes a mixed list of Tenant and TenantClearance
     * by calling describe() on each Record.
     */
    public void demonstratePolymorphism() {
        ArrayList<Record> mixed = new ArrayList<>();
        mixed.addAll(tenants);
        mixed.addAll(clearances);
        System.out.println("--- Polymorphic list of tenants & clearances ---");
        for (Record r : mixed) {
            System.out.println(r.describe());
        }
    }

    // ------------------------------------------------------------------
    // File persistence
    // ------------------------------------------------------------------

    public void loadFromFile() {
        tenants.clear();
        clearances.clear();
        // Tenants
        try (BufferedReader br = new BufferedReader(new FileReader(tenantFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    Tenant t = Tenant.fromFileLine(line);
                    tenants.add(t);
                    // keep next number ahead of existing ids
                    try {
                        int num = Integer.parseInt(t.getId().substring(5));
                        if (num >= nextTenantNum) nextTenantNum = num + 1;
                    } catch (NumberFormatException ignored) {}
                } catch (Exception e) {
                    System.out.println("Skipping damaged tenant line: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            // first run - no file yet, that is fine
        } catch (IOException e) {
            System.out.println("Warning: could not fully load tenants.txt - " + e.getMessage());
        }

        // Clearances
        try (BufferedReader br = new BufferedReader(new FileReader(clearanceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    TenantClearance c = TenantClearance.fromFileLine(line);
                    clearances.add(c);
                    try {
                        int num = Integer.parseInt(c.getId().substring(5));
                        if (num >= nextClearanceNum) nextClearanceNum = num + 1;
                    } catch (NumberFormatException ignored) {}
                } catch (Exception e) {
                    System.out.println("Skipping damaged clearance line: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            // ok
        } catch (IOException e) {
            System.out.println("Warning: could not fully load clearances.txt - " + e.getMessage());
        }
    }

    public void saveToFile() {
        // Tenants
        try (PrintWriter pw = new PrintWriter(new FileWriter(tenantFile))) {
            pw.println("# id|name|gender|phone|year|blocked|roomId");
            for (Tenant t : tenants) {
                pw.println(t.toFileLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving tenants: " + e.getMessage());
        }
        // Clearances
        try (PrintWriter pw = new PrintWriter(new FileWriter(clearanceFile))) {
            pw.println("# id|name|tenantId|reason|dateSigned");
            for (TenantClearance c : clearances) {
                pw.println(c.toFileLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving clearances: " + e.getMessage());
        }
    }
}
