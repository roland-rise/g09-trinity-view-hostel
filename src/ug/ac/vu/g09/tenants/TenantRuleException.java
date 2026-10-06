package ug.ac.vu.g09.tenants;

/**
 * Thrown when a tenant violates hostel allocation rules
 * (unpaid balance, past misconduct, wrong gender block, etc.).
 * @author Mugira Grace
 */
public class TenantRuleException extends Exception {

    public TenantRuleException(String message) {
        super(message);
    }
}
