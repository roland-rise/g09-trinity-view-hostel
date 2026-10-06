package ug.ac.vu.g09.tenants;

import java.util.ArrayList;

/**
 * Temporary stand-in for Mugira Grace's TenantService, with a few invented tenants.
 * @author Mande Roland (for Mugira Grace)
 */
public class TenantService {
    private ArrayList<Tenant> tenants;

    public TenantService() {
        tenants = new ArrayList<Tenant>();
        tenants.add(new Tenant("G09-T001", "Akello Faith", "Female", "0770000001"));
        tenants.add(new Tenant("G09-T002", "Kato Brian", "Male", "0770000002"));
        tenants.add(new Tenant("G09-T003", "Namutebi Sarah", "Female", "0770000003"));
    }

    public Tenant findById(String id) {
        for (Tenant t : tenants) {
            if (t.getId().equalsIgnoreCase(id)) {
                return t;
            }
        }
        return null;
    }

    public ArrayList<Tenant> getAll() {
        return new ArrayList<Tenant>(tenants);
    }
}
