package ug.ac.vu.g09.staff;

/** Thrown when a security shift rule is broken (too many/too few, or unauthorised change). */
public class ShiftClashException extends Exception {
    public ShiftClashException(String message) { super(message); }
}
