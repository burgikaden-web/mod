package dev.kaden.oneforall.client;

public final class OFAClientData {
    public static int vestiges;
    public static int percentage;
    public static int energy;
    public static boolean active = true;
    public static boolean dangerSense;
    public static boolean blackwhipUnlocked;
    public static boolean dangerSenseUnlocked;
    public static boolean smashUnlocked;
    public static boolean receivedSync;
    public static String quirkId = "one_for_all";

    private OFAClientData() {}

    public static void update(int newVestiges, int newPercentage, int newEnergy, boolean newActive,
                              boolean newDangerSense, boolean newBlackwhip, boolean newDangerUnlocked,
                              boolean newSmash, String newQuirkId) {
        vestiges = newVestiges;
        percentage = newPercentage;
        energy = newEnergy;
        active = newActive;
        dangerSense = newDangerSense;
        blackwhipUnlocked = newBlackwhip;
        dangerSenseUnlocked = newDangerUnlocked;
        smashUnlocked = newSmash;
        quirkId = newQuirkId;
        receivedSync = true;
    }
}
