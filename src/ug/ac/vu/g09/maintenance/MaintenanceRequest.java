package ug.ac.vu.g09.maintenance;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.rooms.Room;
import ug.ac.vu.g09.tenants.Tenant;

/**
 * A repair request raised by a tenant for a room.
 * The name field of Record holds a short problem title.
 * Level 1 = urgent (1 day), level 2 = medium (3 days), level 3 = low (7 days).
 * A request moves through the stages reported, logged, inspected, assigned, done, in that order.
 *
 * @author Mutebi Herbert
 */
public class MaintenanceRequest extends Record {

    public static final String[] STAGES = {"reported", "logged", "inspected", "assigned", "done"};
    public static final String[] CATEGORIES = {"plumbing", "electricity", "doors", "windows", "other"};

    private Tenant tenant;
    private Room room;
    private String requestDate;
    private String category;
    private int level;
    private String stage;
    private Fundi fundi;

    /** Creates a brand new request. It always starts at the first stage with no fundi. */
    public MaintenanceRequest(String id, String problem, Tenant tenant, Room room,
                              String requestDate, String category, int level) {
        super(checkId(id), cleanText(problem, "Problem title"));
        setTenant(tenant);
        setRoom(room);
        setRequestDate(requestDate);
        setCategory(category);
        setLevel(level);
        this.stage = STAGES[0];
        this.fundi = null;
    }

    /** Rebuilds a saved request from the file, including its stage and fundi. */
    public MaintenanceRequest(String id, String problem, Tenant tenant, Room room,
                              String requestDate, String category, int level,
                              String stage, Fundi fundi) {
        this(id, problem, tenant, room, requestDate, category, level);
        int index = stageIndex(stage);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown stage: " + stage);
        }
        if (index >= 3 && fundi == null) {
            throw new IllegalArgumentException("A request at assigned or done must have a fundi");
        }
        this.stage = STAGES[index];
        this.fundi = fundi;
    }

    // ---------- getters and setters ----------

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException("A request must belong to a tenant");
        }
        this.tenant = tenant;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        if (room == null) {
            throw new IllegalArgumentException("A request must be linked to a room");
        }
        this.room = room;
    }

    public String getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(String requestDate) {
        if (!isValidDate(requestDate)) {
            throw new IllegalArgumentException("Date must be yyyy-mm-dd, for example 2026-03-14");
        }
        this.requestDate = requestDate.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        String c = category.trim().toLowerCase();
        for (String valid : CATEGORIES) {
            if (valid.equals(c)) {
                this.category = c;
                return;
            }
        }
        throw new IllegalArgumentException("Category must be plumbing, electricity, doors, windows or other");
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("Level must be 1 (urgent), 2 (medium) or 3 (low)");
        }
        this.level = level;
    }

    public String getStage() {
        return stage;
    }

    public Fundi getFundi() {
        return fundi;
    }

    // ---------- business methods ----------

    /** Days allowed to fix the problem, worked out from the level. */
    public int getDaysToFix() {
        if (level == 1) {
            return 1;
        } else if (level == 2) {
            return 3;
        }
        return 7;
    }

    public String getLevelLabel() {
        if (level == 1) {
            return "urgent";
        } else if (level == 2) {
            return "medium";
        }
        return "low";
    }

    /** A request is open until it reaches the last stage. */
    public boolean isOpen() {
        return !stage.equals(STAGES[STAGES.length - 1]);
    }

    /** Moves the request to the next stage in the list. */
    public void nextStage() throws InvalidStageException {
        int current = stageIndex(stage);
        if (current == STAGES.length - 1) {
            throw new InvalidStageException("Request " + id + " is already done");
        }
        moveToStage(STAGES[current + 1]);
    }

    /** Moves to the named stage, but only if it is exactly the next one. Skipping is not allowed. */
    public void moveToStage(String target) throws InvalidStageException {
        int targetIndex = stageIndex(target);
        if (targetIndex < 0) {
            throw new IllegalArgumentException("Unknown stage: " + target);
        }
        int current = stageIndex(stage);
        if (targetIndex != current + 1) {
            String expected = (current + 1 < STAGES.length) ? STAGES[current + 1] : "none";
            throw new InvalidStageException("Cannot go from " + stage + " to " + STAGES[targetIndex]
                    + ". The next stage must be: " + expected);
        }
        if (STAGES[targetIndex].equals("assigned") && fundi == null) {
            throw new InvalidStageException("Assign a fundi first; a request cannot be assigned without one");
        }
        this.stage = STAGES[targetIndex];
    }

    /** Gives the request to a fundi. Only allowed after inspection; moves the request to assigned. */
    public void assignFundi(Fundi f) throws InvalidStageException {
        if (f == null) {
            throw new IllegalArgumentException("Fundi cannot be null");
        }
        if (!stage.equals("inspected")) {
            throw new InvalidStageException("A fundi can only be assigned after inspection. Current stage: " + stage);
        }
        this.fundi = f;
        this.stage = "assigned";
    }

    @Override
    public String describe() {
        return id + " | " + name
                + " | Tenant: " + tenant.getName()
                + " | Room: " + room.getName()
                + " | " + category
                + " | Reported: " + requestDate
                + " | Level " + level + " (" + getLevelLabel() + ")"
                + " | Fix within " + getDaysToFix() + " day(s)"
                + " | Stage: " + stage
                + " | Fundi: " + (fundi == null ? "not assigned" : fundi.getName());
    }

    /** One line for the file: id|title|tenantId|roomId|date|category|level|stage|fundiId (or NONE) */
    public String toFileLine() {
        return id + "|" + name + "|" + tenant.getId() + "|" + room.getId() + "|" + requestDate
                + "|" + category + "|" + level + "|" + stage + "|" + (fundi == null ? "NONE" : fundi.getId());
    }

    // ---------- helpers ----------

    private static int stageIndex(String s) {
        if (s == null) {
            return -1;
        }
        String wanted = s.trim().toLowerCase();
        for (int i = 0; i < STAGES.length; i++) {
            if (STAGES[i].equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isValidDate(String d) {
        if (d == null || !d.trim().matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }
        String[] parts = d.trim().split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        return year >= 2000 && year <= 2100 && month >= 1 && month <= 12 && day >= 1 && day <= 31;
    }

    private static String checkId(String id) {
        if (id == null || !id.matches("G09-M\\d{3,}")) {
            throw new IllegalArgumentException("Request ID must look like G09-M001");
        }
        return id;
    }

    private static String cleanText(String text, String label) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty");
        }
        // the pipe is the file separator, so it cannot appear inside a field
        return text.trim().replace("|", "/");
    }
}
