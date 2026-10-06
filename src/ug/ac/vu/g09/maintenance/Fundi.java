package ug.ac.vu.g09.maintenance;

import ug.ac.vu.g09.core.Record;

/**
 * A fundi (repair worker) who can be assigned to maintenance requests.
 * The name field of Record holds the full name.
 *
 * @author Mutebi Herbert
 */
public class Fundi extends Record {

    private String trade;
    private String phone;

    public Fundi(String id, String name, String trade, String phone) {
        super(checkId(id), cleanText(name, "Fundi name"));
        setTrade(trade);
        setPhone(phone);
    }

    // ---------- getters and setters ----------

    public String getTrade() {
        return trade;
    }

    public void setTrade(String trade) {
        String t = cleanText(trade, "Trade").toLowerCase();
        for (String c : MaintenanceRequest.CATEGORIES) {
            if (c.equals(t)) {
                this.trade = t;
                return;
            }
        }
        throw new IllegalArgumentException("Trade must be plumbing, electricity, doors, windows or other");
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        // Ugandan number: 0 followed by 9 digits, or +256 followed by 9 digits
        if (phone == null || !phone.trim().matches("(\\+256|0)\\d{9}")) {
            throw new IllegalArgumentException("Phone must look like 0772123456 or +256772123456");
        }
        this.phone = phone.trim();
    }

    // ---------- business methods ----------

    @Override
    public String describe() {
        return id + " | " + name + " | Trade: " + trade + " | Phone: " + phone;
    }

    /** One line for the file: id|name|trade|phone */
    public String toFileLine() {
        return id + "|" + name + "|" + trade + "|" + phone;
    }

    // ---------- helpers used before super() is called ----------

    private static String checkId(String id) {
        if (id == null || !id.matches("G09-F\\d{3,}")) {
            throw new IllegalArgumentException("Fundi ID must look like G09-F001");
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
