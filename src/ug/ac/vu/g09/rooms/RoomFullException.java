package ug.ac.vu.g09.rooms;

/**
 * Raised when a person is added to a room that has no free beds.
 * @author Nsubuga Abdul
 */
public class RoomFullException extends Exception {
    public RoomFullException(String msg) {
        super(msg);
    }
}
