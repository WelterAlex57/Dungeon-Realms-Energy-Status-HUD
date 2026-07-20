package com.dungeonrealms.circularxp;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/**
 * Wraps a Forge Configuration file. Values are cached in static fields for fast
 * per-frame access, and re-read from the Configuration object whenever the config
 * GUI is closed (see CircularXPConfigGui) or the toggle keybind changes something.
 */
public class ModConfig {

    public static Configuration config;

    public static boolean enabled = true;
    public static float scale = 1.0f;
    public static RingStyle ringStyle = RingStyle.SPLIT;
    public static ColorStyle colorStyle = ColorStyle.GREEN;

    // Only used when colorStyle == CUSTOM
    public static String customColorHex = "4DD958";
    public static float customR = 0.30f, customG = 0.85f, customB = 0.35f;

    public static boolean borderEnabled = true;
    public static float borderThickness = 1.0f;
    public static BorderColor borderColor = BorderColor.BLACK;

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;

    /** Overall ring shape drawn around the crosshair. */
    public enum RingStyle {
        FULL,        // one unbroken circle
        SINGLE_GAP,  // one ring with a single gap at the bottom
        SPLIT        // two half-arcs, gap at both top and bottom (current default look)
    }

    public enum ColorStyle {
        GREEN(0.30f, 0.85f, 0.35f),
        BLUE(0.25f, 0.55f, 0.95f),
        RED(0.90f, 0.25f, 0.20f),
        PURPLE(0.65f, 0.30f, 0.90f),
        GOLD(0.95f, 0.75f, 0.20f),
        WHITE(0.90f, 0.90f, 0.90f),
        CUSTOM(-1f, -1f, -1f); // special-cased in the renderer to use customR/customG/customB

        public final float r, g, b;

        ColorStyle(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public enum BorderColor {
        BLACK(0.0f, 0.0f, 0.0f),
        WHITE(1.0f, 1.0f, 1.0f),
        GRAY(0.5f, 0.5f, 0.5f),
        DARK_GRAY(0.15f, 0.15f, 0.15f),
        GOLD(0.95f, 0.75f, 0.20f),
        MATCH(-1f, -1f, -1f); // special-cased in the renderer to reuse whatever ColorStyle is selected

        public final float r, g, b;

        BorderColor(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            config.load(); // read from disk — only needed once, at startup
            sync();
        }
    }

    /**
     * Re-reads values from the (already in-memory) Configuration object into the cached
     * static fields, and persists any changes to disk. Deliberately does NOT call
     * config.load() here — that would re-read the file from disk and stomp whatever the
     * config GUI just wrote into memory but hasn't necessarily flushed to disk yet.
     */
    public static void load() {
        sync();
    }

    private static void sync() {
        enabled = config.getBoolean(
                "Enabled", CATEGORY_GENERAL, true,
                "Master on/off switch for the circular XP ring. Can also be toggled in-game with a keybind (set it under Controls)."
        );

        scale = (float) config.getFloat(
                "Scale", CATEGORY_GENERAL, 1.0f, 0.5f, 2.5f,
                "Size multiplier for the ring. 1.0 = default size, smaller = smaller ring, larger = bigger ring."
        );

        String ringStyleName = config.getString(
                "Style", CATEGORY_GENERAL, RingStyle.SPLIT.name(),
                "Ring shape. Options: FULL (unbroken circle), SINGLE_GAP (one ring, gap at the bottom), SPLIT (two half-arcs, gap at top and bottom)"
        );
        try {
            ringStyle = RingStyle.valueOf(ringStyleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            ringStyle = RingStyle.SPLIT;
        }

        String styleName = config.getString(
                "ColorStyle", CATEGORY_GENERAL, ColorStyle.GREEN.name(),
                "Ring color style. Options: GREEN, BLUE, RED, PURPLE, GOLD, WHITE, CUSTOM (uses the CustomColorHex value below)"
        );
        try {
            colorStyle = ColorStyle.valueOf(styleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            colorStyle = ColorStyle.GREEN;
        }

        customColorHex = config.getString(
                "CustomColorHex", CATEGORY_GENERAL, "4DD958",
                "Custom ring color as a hex code (RRGGBB, with or without a leading #). Only used when ColorStyle is set to CUSTOM."
        );
        float[] rgb = parseHexColor(customColorHex);
        customR = rgb[0];
        customG = rgb[1];
        customB = rgb[2];

        borderEnabled = config.getBoolean(
                "BorderEnabled", CATEGORY_GENERAL, true,
                "Whether to draw a thin outline around the inner and outer edges of the ring."
        );

        borderThickness = (float) config.getFloat(
                "BorderThickness", CATEGORY_GENERAL, 1.0f, 0.5f, 4.0f,
                "Border thickness in pixels (scales along with the ring's Scale setting)."
        );

        String borderColorName = config.getString(
                "BorderColor", CATEGORY_GENERAL, BorderColor.BLACK.name(),
                "Border color. Options: BLACK, WHITE, GRAY, DARK_GRAY, GOLD, MATCH (matches the ring's current fill color)"
        );
        try {
            borderColor = BorderColor.valueOf(borderColorName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            borderColor = BorderColor.BLACK;
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    /** Writes the current in-memory values back out, e.g. after a keybind toggle. */
    public static void save() {
        if (config == null) {
            return;
        }
        config.get(CATEGORY_GENERAL, "Enabled", true).set(enabled);
        if (config.hasChanged()) {
            config.save();
        }
    }

    private static float[] parseHexColor(String hex) {
        try {
            String h = hex.trim();
            if (h.startsWith("#")) {
                h = h.substring(1);
            }
            if (h.length() != 6) {
                throw new NumberFormatException("Expected 6 hex digits");
            }
            int r = Integer.parseInt(h.substring(0, 2), 16);
            int g = Integer.parseInt(h.substring(2, 4), 16);
            int b = Integer.parseInt(h.substring(4, 6), 16);
            return new float[]{r / 255f, g / 255f, b / 255f};
        } catch (Exception e) {
            // Fall back to the default green if the hex string is malformed.
            return new float[]{0.30f, 0.85f, 0.35f};
        }
    }
}
