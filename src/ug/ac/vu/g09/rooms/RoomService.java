package ug.ac.vu.g09.rooms;

import java.util.ArrayList;

/**
 * Temporary stand-in for Nsubuga Abdul's RoomService, with a few invented rooms.
 * @author Mande Roland (for Nsubuga Abdul)
 */
public class RoomService {
    private ArrayList<Room> rooms;

    public RoomService() {
        rooms = new ArrayList<Room>();
        rooms.add(new Room("G09-R001", "A101", "Single", 1200000));
        rooms.add(new Room("G09-R002", "B205", "Double", 750000));
        rooms.add(new Room("G09-R003", "C103", "Triple", 550000));
    }

    public Room findByNumber(String number) {
        for (Room r : rooms) {
            if (r.getName().equalsIgnoreCase(number)) {
                return r;
            }
        }
        return null;
    }

    public ArrayList<Room> getAll() {
        return new ArrayList<Room>(rooms);
    }
}
