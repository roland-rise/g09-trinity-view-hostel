package ug.ac.vu.g09.payments;

/**
 * Thrown when a payment breaks one of the hostel's payment rules.
 * @author Mande Roland
 */
public class InvalidPaymentException extends Exception {
    public InvalidPaymentException(String message) {
        super(message);
    }
}
