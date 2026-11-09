package dev.kaden.oneforall.data;

public final class OFAPlayerProgress {
    public static final int MAX_VESTIGES = 50;
    public static final int MAX_ENERGY = 100;

    private int vestiges;
    private int energy;
    private boolean active;
    private boolean dangerSense;

    public OFAPlayerProgress() {
        this(0, MAX_ENERGY, true, false);
    }

    public OFAPlayerProgress(int vestiges, int energy, boolean active, boolean dangerSense) {
        this.vestiges = clamp(vestiges, 0, MAX_VESTIGES);
        this.energy = clamp(energy, 0, MAX_ENERGY);
        this.active = active;
        this.dangerSense = dangerSense;
    }

    public int getVestiges() { return vestiges; }
    public int getEnergy() { return energy; }
    public boolean isActive() { return active; }
    public boolean isDangerSenseActive() { return dangerSense; }
    public int getPercentage() { return Math.min(100, 5 + vestiges * 2); }
    public boolean hasBlackwhip() { return vestiges >= 10; }
    public boolean hasDangerSense() { return vestiges >= 20; }
    public boolean hasDetroitSmash() { return vestiges >= 5; }
    public boolean hasDelawareSmash() { return vestiges >= 2; }

    public void setVestiges(int amount) {
        vestiges = clamp(amount, 0, MAX_VESTIGES);
    }

    public boolean addVestiges(int amount) {
        int before = vestiges;
        vestiges = clamp(vestiges + amount, 0, MAX_VESTIGES);
        return before != vestiges;
    }

    public boolean spendEnergy(int amount) {
        if (energy < amount) return false;
        energy -= amount;
        return true;
    }

    public boolean regenerateEnergy() {
        if (energy >= MAX_ENERGY) return false;
        energy++;
        return true;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) dangerSense = false;
    }

    public void toggleDangerSense() { dangerSense = !dangerSense; }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
