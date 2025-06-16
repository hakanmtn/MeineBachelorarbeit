package util;

import com.microsoft.z3.*;
import java.util.HashMap;


public class Z3Utils {
    private static boolean loaded = false;

    public static synchronized void loadZ3Libraries() {
        if (!loaded) {
            try {
                System.load("/Users/hakanmetin/Downloads/z3-4.12.2-arm64-osx-11.0/bin/libz3.dylib");
                System.load("/Users/hakanmetin/Downloads/z3-4.12.2-arm64-osx-11.0/bin/libz3java.dylib");
                System.out.println("Z3-Bibliotheken erfolgreich geladen");
                loaded = true;
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Fehler beim Laden der Z3-Bibliotheken: " + e.getMessage());
                throw e;
            }
        }
    }

}
