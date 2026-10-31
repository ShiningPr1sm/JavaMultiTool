package util;

import java.awt.Color;

/**
 * Generates stable, deterministic colors for product/entry keys:
 * hue is spread with the golden angle, saturation and lightness are fixed,
 * so every chart keeps the same color for the same name across sessions.
 */
public final class ColorGenerator {

    private static final float GOLDEN_ANGLE = 137.508f;
    private static final float SATURATION = 0.65f; // juicy, but not eye-burning
    private static final float LIGHTNESS = 0.55f;  // good contrast for diagrams

    private ColorGenerator() {}

    /**
     * Returns HEX color (e.g. "#FF5733") stably for a concrete ID or name.
     */
    public static String getColorForProduct(long productId) {
        float hue = (productId * GOLDEN_ANGLE) % 360.0f;
        if (hue < 0) hue += 360.0f;
        return hslToHex(hue, SATURATION, LIGHTNESS);
    }

    /** Same scheme as HEX variant, but as an AWT color for direct painting. */
    public static Color colorFor(String key) {
        long seed = Math.abs((long) key.hashCode());
        float hue = (seed * GOLDEN_ANGLE) % 360.0f;
        if (hue < 0) hue += 360.0f;
        return hslToColor(hue, SATURATION, LIGHTNESS);
    }

    private static Color hslToColor(float h, float s, float l) {
        int[] rgb = hslToRgb(h, s, l);
        return new Color(rgb[0], rgb[1], rgb[2]);
    }

    private static String hslToHex(float h, float s, float l) {
        int[] rgb = hslToRgb(h, s, l);
        return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
    }

    private static int[] hslToRgb(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;

        float r = 0, g = 0, b = 0;
        if (h < 60)       { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else              { r = c; g = 0; b = x; }

        return new int[]{
                Math.round((r + m) * 255),
                Math.round((g + m) * 255),
                Math.round((b + m) * 255)
        };
    }
}
