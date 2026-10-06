package ug.ac.vu.g09.rooms;

import ug.ac.vu.g09.core.Record;
import ug.ac.vu.g09.tenants.Tenant;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service layer for rooms and blocks.
 * Owns the ArrayLists, file I/O and reports.
 * As Storage lead I defined the pipe-separated format used by every module.
 * @author Nsubuga Abdul
 */
public class RoomService {

    private final ArrayList<Room> rooms = new ArrayList<>();
    private final ArrayList<Block> blocks = new ArrayList<>();
    private final String roomFile = "rooms.txt";
    private final String blockFile = "blocks.txt";
    private int nextRoom = 1;
    private int nextBlock = 1;

    public RoomService() {
        loadFromFile();
        if (blocks.isEmpty()) seedDefaultBlocks();
    }

    // seed A/B male, C/D female if file missing
    private void seedDefaultBlocks() {
        blocks.add(new Block("G09-K001", "A", "Male"));
        blocks.add(new Block("G09-K002", "B", "Male"));
        blocks.add(new Block("G09-K003", "C", "Female"));
        blocks.add(new Block("G09-K004", "D", "Female"));
        nextBlock = 5;
        saveToFile();
    }

    // ---- Block CRUD ----
    public void addBlock(Block b) {
        blocks.add(b);
        saveToFile();
    }

    public Block findBlock(String id) {
        for (Block b : blocks) if (b.getId().equals(id)) return b;
        return null;
    }

    public Block findBlockByLetter(String letter) {
        for (Block b : blocks) if (b.getName().equalsIgnoreCase(letter)) return b;
        return null;
    }

    public List<Block> allBlocks() { return new ArrayList<>(blocks); }

    // ---- Room CRUD ----
    public void addRoom(Room r) {
        if (findRoom(r.getId()) != null)
            throw new IllegalArgumentException("duplicate room id");
        rooms.add(r);
        saveToFile();
    }

    public Room findRoom(String id) {
        for (Room r : rooms) if (r.getId().equals(id)) return r;
        return null;
    }

    public Room findRoomByNumber(String number) {
        for (Room r : rooms) if (r.getName().equalsIgnoreCase(number)) return r;
        return null;
    }

    public void updateRoom(Room r) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getId().equals(r.getId())) {
                rooms.set(i, r);
                saveToFile();
                return;
            }
        }
        throw new IllegalArgumentException("room not found");
    }

    public boolean removeRoom(String id) {
        Room r = findRoom(id);
        if (r == null) return false;
        rooms.remove(r);
        saveToFile();
        return true;
    }

    public List<Room> allRooms() { return new ArrayList<>(rooms); }

    public String nextRoomId() {
        String id;
        do {
            id = String.format("G09-R%03d", nextRoom++);
        } while (findRoom(id) != null);
        return id;
    }

    public String nextBlockId() {
        return String.format("G09-K%03d", nextBlock++);
    }

    /**
     * Try to put a tenant into a room.
     * Throws RoomFullException if full.
     */
    public void assignTenant(String roomId, Tenant tenant) throws RoomFullException {
        Room r = findRoom(roomId);
        if (r == null) throw new IllegalArgumentException("room not found");
        r.addOccupant(tenant);
        updateRoom(r);
    }

    // ---- Reports ----
    /** Available / partly occupied rooms ordered by free beds descending */
    public List<Room> availableSortedByFreeBeds() {
        List<Room> list = new ArrayList<>();
        for (Room r : rooms) {
            if (r.freeBeds() > 0 &&
                (r.getStatus().equals("available") || r.getStatus().equals("partly occupied"))) {
                list.add(r);
            }
        }
        list.sort(Comparator.comparingInt(Room::freeBeds).reversed());
        return list;
    }

    /** Polymorphic walk over rooms + blocks */
    public void polyDemo() {
        ArrayList<Record> mixed = new ArrayList<>();
        mixed.addAll(rooms);
        mixed.addAll(blocks);
        System.out.println("--- mixed Room & Block list ---");
        for (Record rec : mixed) {
            System.out.println(rec.describe());
        }
    }

    // ---- File I/O (never crashes on missing / damaged file) ----
    public void loadFromFile() {
        rooms.clear();
        blocks.clear();

        // blocks first
        try (BufferedReader br = new BufferedReader(new FileReader(blockFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    Block b = Block.fromLine(line);
                    blocks.add(b);
                    try {
                        int n = Integer.parseInt(b.getId().substring(5));
                        if (n >= nextBlock) nextBlock = n + 1;
                    } catch (Exception ignored) {}
                } catch (Exception ex) {
                    System.out.println("skip bad block: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            // first run ok
        } catch (IOException e) {
            System.out.println("block load warning: " + e.getMessage());
        }

        // rooms
        try (BufferedReader br = new BufferedReader(new FileReader(roomFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                try {
                    String[] p = line.split("\\|", -1);
                    Block b = null;
                    if (p.length > 5) b = findBlock(p[5]);
                    if (b == null && !blocks.isEmpty()) b = blocks.get(0); // fallback
                    Room r = Room.fromLine(line, b);
                    rooms.add(r);
                    try {
                        int n = Integer.parseInt(r.getId().substring(5));
                        if (n >= nextRoom) nextRoom = n + 1;
                    } catch (Exception ignored) {}
                } catch (Exception ex) {
                    System.out.println("skip bad room: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            // ok
        } catch (IOException e) {
            System.out.println("room load warning: " + e.getMessage());
        }
    }

    public void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(blockFile))) {
            pw.println("# id|letter|genderRule");
            for (Block b : blocks) pw.println(b.toFileLine());
        } catch (IOException e) {
            System.out.println("block save error: " + e.getMessage());
        }
        try (PrintWriter pw = new PrintWriter(new FileWriter(roomFile))) {
            pw.println("# id|number|type|selfContained|status|blockId");
            for (Room r : rooms) pw.println(r.toFileLine());
        } catch (IOException e) {
            System.out.println("room save error: " + e.getMessage());
        }
    }
}
