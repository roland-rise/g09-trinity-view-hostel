package ug.ac.vu.g09.rooms;

import ug.ac.vu.g09.core.Record;

/**
 * Temporary stand-in for Nsubuga Abdul's Room class so the payments module can run.
 * @author Mande Roland (for Nsubuga Abdul)
 */
public class Room extends Record {
    private String roomType;
    private double fee;

    public Room(String id, String roomNumber, String roomType, double fee) {
        super(id, roomNumber);
        this.roomType = roomType;
        this.fee = fee;
    }

    public String getRoomType() {
        return roomType;
    }

    public double getFeePerPerson() {
        return fee;
    }

    @Override
    public String describe() {
        return id + " | " + name + " | " + roomType + " | " + fee;
    }
}
