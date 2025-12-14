package com.rpg;

import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.InputStream;

public class ResourceLoader {
    private static JSONObject strings;

    public static void loadResources() {
        try (InputStream is = ResourceLoader.class.getResourceAsStream("/data/strings.json")) {
            if (is == null) throw new RuntimeException("Cannot find strings.json");
            strings = new JSONObject(new JSONTokener(is));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getString(String key) {
        return strings.optString(key, "MISSING TEXT: " + key);
    }
}