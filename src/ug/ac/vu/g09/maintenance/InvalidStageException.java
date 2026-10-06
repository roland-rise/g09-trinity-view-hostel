package ug.ac.vu.g09.maintenance;

/**
 * Thrown when a maintenance request is moved to a stage that is not allowed,
 * for example skipping a stage or advancing a request that is already done.
 *
 * @author Mutebi Herbert
 */
public class InvalidStageException extends Exception {

    public InvalidStageException(String msg) {
        super(msg);
    }
}
