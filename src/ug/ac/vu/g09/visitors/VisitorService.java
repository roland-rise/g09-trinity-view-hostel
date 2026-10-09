package ug.ac.vu.g09.visitors;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import ug.ac.vu.g09.tenants.Tenant;
import ug.ac.vu.g09.tenants.TenantService;

/**
 * Keeps visits and banned visitors, enforces the visiting rules, saves to file.
 * @author Josemaria Karitani Wamala
 */
public class VisitorService {
    private static final String OPEN = "09:00";
    private static final String CLOSE = "22:00";

    private final ArrayList<VisitRecord> visits = new ArrayList<>();
    private final ArrayList<BannedVisitor> banned = new ArrayList<>();
    private final String visitFile;
    private final String bannedFile;
    private final TenantService tenants;

    public VisitorService(TenantService tenants, String visitFile, String bannedFile) {
        this.tenants = tenants;
        this.visitFile = visitFile;
        this.bannedFile = bannedFile;
        loadFromFile();
    }

    /** Checks the three client rules, then adds the visit and saves. */
    public void addVisit(VisitRecord v) throws VisitorNotAllowedException {
        if (v.getIdNumber() == null || v.getIdNumber().isEmpty())
            throw new VisitorNotAllowedException("Visitor has no ID");
        if (isBanned(v.getIdNumber()))
            throw new VisitorNotAllowedException(v.getName() + " is banned");
        boolean insideHours = v.getTimeIn().compareTo(OPEN) >= 0
                && v.getTimeIn().compareTo(CLOSE) <= 0;
        if (!insideHours && !(v.isOvernight() && !v.getApprovedBy().isEmpty()))
            throw new VisitorNotAllowedException(
                    "Visiting hours are 09:00 to 22:00. Overnight needs manager approval");
        visits.add(v);
        saveToFile();
    }

    public VisitRecord findVisit(String id) {
        for (VisitRecord v : visits)
            if (v.getId().equals(id)) return v;
        return null;
    }

    public boolean signOut(String id, String time) {
        VisitRecord v = findVisit(id);
        if (v == null || !v.isInside()) return false;
        v.signOut(time);
        saveToFile();
        return true;
    }

    public boolean removeVisit(String id) {
        VisitRecord v = findVisit(id);
        if (v == null) return false;
        visits.remove(v);
        saveToFile();
        return true;
    }

    public void ban(BannedVisitor b) {
        if (isBanned(b.getIdNumber())) {
            throw new IllegalArgumentException("That ID number is already banned");
        }
        banned.add(b);
        saveToFile();
    }

    public boolean isBanned(String idNumber) {
        for (BannedVisitor b : banned)
            if (b.getIdNumber().equalsIgnoreCase(idNumber)) return true;
        return false;
    }

    /** Report: visits sorted by visit date, then time in. */
    public ArrayList<VisitRecord> getSortedByTimeIn() {
        ArrayList<VisitRecord> copy = new ArrayList<>(visits);
        Collections.sort(copy, new Comparator<VisitRecord>() {
            @Override
            public int compare(VisitRecord a, VisitRecord b) {
                int d = a.getVisitDate().compareTo(b.getVisitDate());
                return d != 0 ? d : a.getTimeIn().compareTo(b.getTimeIn());
            }
        });
        return copy;
    }

    /** Report: visitors who have not signed out. */
    public ArrayList<VisitRecord> getStillInside() {
        ArrayList<VisitRecord> inside = new ArrayList<>();
        for (VisitRecord v : visits)
            if (v.isInside()) inside.add(v);
        return inside;
    }

    /** Records are kept one academic year. Pass the cutoff date, e.g. 2025-10-05. */
    public int removeOlderThan(String cutoffDate) {
        int before = visits.size();
        visits.removeIf(v -> v.getVisitDate().compareTo(cutoffDate) < 0);
        if (visits.size() != before) saveToFile();
        return before - visits.size();
    }

    public ArrayList<BannedVisitor> getBanned() { return banned; }

    public String nextVisitId() { return "G09-V" + String.format("%03d", highest(visits) + 1); }
    public String nextBanId() { return "G09-X" + String.format("%03d", highest(banned) + 1); }

    // the next ID is one more than the biggest in use, so a removed record never causes a repeat
    private int highest(ArrayList<? extends ug.ac.vu.g09.core.Record> list) {
        int top = 0;
        for (ug.ac.vu.g09.core.Record r : list) {
            try {
                top = Math.max(top, Integer.parseInt(r.getId().substring(5)));
            } catch (RuntimeException e) {
                // an ID that does not end in a number is ignored
            }
        }
        return top;
    }

    public void loadFromFile() {
        visits.clear();
        banned.clear();
        File f = new File(visitFile);
        if (f.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    try {
                        String[] p = line.split("\\|", -1);
                        Tenant t = tenants.findTenantById(p[3]);
                        if (t == null) continue;
                        VisitRecord v = new VisitRecord(p[0], p[1], p[2], t, p[4], p[5]);
                        if (!p[6].isEmpty()) v.setTimeOut(p[6]);
                        if (Boolean.parseBoolean(p[7])) v.approveOvernight(p[8]);
                        visits.add(v);
                    } catch (RuntimeException bad) {
                        System.out.println("Skipped damaged visit line: " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not read " + visitFile + ": " + e.getMessage());
            }
        }
        File b = new File(bannedFile);
        if (b.exists()) {
            try (BufferedReader r = new BufferedReader(new FileReader(b))) {
                String line;
                while ((line = r.readLine()) != null) {
                    try {
                        banned.add(BannedVisitor.fromFileLine(line));
                    } catch (RuntimeException bad) {
                        System.out.println("Skipped damaged ban line: " + line);
                    }
                }
            } catch (IOException e) {
                System.out.println("Could not read " + bannedFile + ": " + e.getMessage());
            }
        }
    }

    public void saveToFile() {
        try (PrintWriter w = new PrintWriter(new FileWriter(visitFile))) {
            for (VisitRecord v : visits) w.println(v.toFileLine());
        } catch (IOException e) {
            System.out.println("Could not save " + visitFile + ": " + e.getMessage());
        }
        try (PrintWriter w = new PrintWriter(new FileWriter(bannedFile))) {
            for (BannedVisitor b : banned) w.println(b.toFileLine());
        } catch (IOException e) {
            System.out.println("Could not save " + bannedFile + ": " + e.getMessage());
        }
    }
}
