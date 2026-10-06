package ug.ac.vu.g09.tenants;

import ug.ac.vu.g09.core.Record;

/**
 * Temporary stand-in for Mugira Grace's Tenant class so the payments module can run.
 * @author Mande Roland (for Mugira Grace)
 */
public class Tenant extends Record {
    private String gender;
    private String phone;

    public Tenant(String id, String name, String gender, String phone) {
        super(id, name);
        this.gender = gender;
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String describe() {
        return id + " | " + name + " | " + gender + " | " + phone;
    }
}
