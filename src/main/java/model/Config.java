package model;


public final class Config {
    private Config() {}

    public enum Mutant {
        NONE,

        // 1) Mutierte Verträge
        L1_LIST_ADD_LEN_STAYS,            // List.add behauptet: new == old (statt old ++ [e])

        // 2) Mutierter Übersetzer (Java -> SMT)
        E1_ENCODER_DROP_LAST_ELEMENT,     // Übersetzer verliert letztes Element jeder Collection

        // 3) Manipulierte Historie
        H1_HISTORY_FLIP_CONTAINS_RESULT   // Ergebnis von contains wird invertiert geloggt
    }

    public static Mutant mutant = Mutant.NONE;
    public static boolean VERBOSE = false;
}
