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
    public static ColorStyle colorStyle = ColorStyle.GREEN;

    private static final String CATEGORY_GENERAL = Configuration.CATEGORY_GENERAL;

    public enum ColorStyle {
        GREEN(0.30f, 0.85f, 0.35f),
        BLUE(0.25f, 0.55f, 0.95f),
        RED(0.90f, 0.25f, 0.20f),
        PURPLE(0.65f, 0.30f, 0.90f),
        GOLD(0.95f, 0.75f, 0.20f),
        WHITE(0.90f, 0.90f, 0.90f);

        public final float r, g, b;

        ColorStyle(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            load();
        }
    }

    /** Reads values from the Configuration object into the cached static fields. */
    public static void load() {
        config.load();

        enabled = config.getBoolean(
                "Enabled", CATEGORY_GENERAL, true,
                "Master on/off switch for the circular XP ring. Can also be toggled in-game with a keybind (set it under Controls)."
        );

        scale = (float) config.getFloat(
                "Scale", CATEGORY_GENERAL, 1.0f, 0.5f, 2.5f,
                "Size multiplier for the ring. 1.0 = default size, smaller = smaller ring, larger = bigger ring."
        );

        String styleName = config.getString(
                "ColorStyle", CATEGORY_GENERAL, ColorStyle.GREEN.name(),
                "Ring color style. Options: GREEN, BLUE, RED, PURPLE, GOLD, WHITE"
        );
        try {
            colorStyle = ColorStyle.valueOf(styleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            colorStyle = ColorStyle.GREEN;
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
}
