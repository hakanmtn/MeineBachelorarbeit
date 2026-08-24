package util;

import com.microsoft.z3.Version;

public final class Z3Utils {
    private static boolean loaded = false;

    private Z3Utils() {
    }

    public static synchronized void loadZ3Libraries() {
        if (!loaded) {
            // z3-turnkey extracts and loads the matching native library.
            Version.getFullVersion();
            loaded = true;
        }
    }
}
