package ug.ac.vu.g09.rooms;

import ug.ac.vu.g09.core.Record;

/**
 * Represents a hostel block (A/B male, C/D female).
 * Second model class for the Rooms module.
 * @author Nsubuga Abdul
 */
public class Block extends Record {

    private String genderRule;   // "Male" or "Female"

    public Block(String id, String blockLetter, String genderRule) {
        super(id, blockLetter);
        setGenderRule(genderRule);
    }

    public String getGenderRule() {
        return genderRule;
    }

    public void setGenderRule(String genderRule) {
        if (genderRule == null) throw new IllegalArgumentException("genderRule null");
        String g = genderRule.trim();
        if (!g.equalsIgnoreCase("Male") && !g.equalsIgnoreCase("Female")) {
            throw new IllegalArgumentException("Block gender must be Male or Female");
        }
        this.genderRule = Character.toUpperCase(g.charAt(0)) + g.substring(1).toLowerCase();
    }

    /** true if a tenant of the given gender may live in this block */
    public boolean allows(String tenantGender) {
        return genderRule.equalsIgnoreCase(tenantGender);
    }

    @Override
    public String describe() {
        return "Block " + getName() + " (" + genderRule + " only) id=" + getId();
    }

    @Override
    public String toFileLine() {
        return getId() + "|" + getName() + "|" + genderRule;
    }

    public static Block fromLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length < 3) throw new IllegalArgumentException("bad block line");
        return new Block(p[0], p[1], p[2]);
    }
}
