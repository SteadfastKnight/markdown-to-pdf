package com.steadfastknight.md2pdf;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bundled font registry, mirroring the Python FONTS dict.
 * Key = label shown in the UI/CLI. Resources live under /fonts on the classpath.
 */
public final class FontRegistry {
    private FontRegistry() {}

    public record Font(String reg, String bold, double size) {}

    public static final Map<String, Font> FONTS = new LinkedHashMap<>();
    static {
        FONTS.put("EB Garamond (classic serif)",
                new Font("EBGaramond-Regular.ttf", "EBGaramond-Bold.ttf", 8.4));
        FONTS.put("Lora (modern serif)",
                new Font("Lora-Regular.ttf", "Lora-Bold.ttf", 7.6));
        FONTS.put("Source Sans (clean sans)",
                new Font("SourceSans-Regular.ttf", "SourceSans-Bold.ttf", 7.5));
    }

    /** First key = default font, mirroring list(FONTS)[0]. */
    public static String defaultKey() {
        return FONTS.keySet().iterator().next();
    }

    public static String fontResource(String file) {
        return "/fonts/" + file;
    }
}
