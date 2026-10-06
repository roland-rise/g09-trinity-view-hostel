package ug.ac.vu.g09.visitors;

/**
 * Thrown when a visit breaks a client rule: banned visitor, no ID, or outside hours.
 * @author Josemaria Karitani Wamala
 */
public class VisitorNotAllowedException extends Exception {
    public VisitorNotAllowedException(String msg) {
        super(msg);
    }
}
