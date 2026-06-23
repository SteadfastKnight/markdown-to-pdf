package com.steadfastknight.md2pdf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Layout constants, ported verbatim from the Python original (md2a6_app.py).
 * Everything is in PDF points (1 pt = 1/72 in) unless noted.
 */
public final class Layout {
    private Layout() {}

    /** Points per millimetre. */
    public static final double MM = 72.0 / 25.4;

    // Rendered A6 page and its margins (left wide for hole-punch).
    public static final double A6_W = 105.0 * MM;
    public static final double A6_H = 148.5 * MM;
    public static final double ML = 12.0 * MM;
    public static final double MT = 10.0 * MM;
    public static final double MR = 5.5 * MM;
    public static final double MB = 10.0 * MM;

    // A4 sheet (exact PDF points) + imposition spacing.
    public static final double A4_W = 595.276;
    public static final double A4_H = 841.890;
    public static final double EDGE = 1.0 * MM;   // safety inset from sheet edge
    public static final double GUTTER = 0.0;       // gap between cells

    /** pages-per-sheet -> grid + rotation. cols x rows, rot in degrees. */
    public record Grid(int cols, int rows, int rot) {
        public int per() { return cols * rows; }
    }

    public static final Map<Integer, Grid> LAYOUTS = new LinkedHashMap<>();
    static {
        LAYOUTS.put(1, new Grid(1, 1, 0));   // one A4 page
        LAYOUTS.put(2, new Grid(1, 2, 90));  // two A5 pages, turned 90 deg
        LAYOUTS.put(4, new Grid(2, 2, 0));   // four A6 pages (default)
    }

    /** Trimmed page size label per layout, used in the suggested file name. */
    public static final Map<Integer, String> SIZE_LABEL = Map.of(1, "A4", 2, "A5", 4, "A6");
}
