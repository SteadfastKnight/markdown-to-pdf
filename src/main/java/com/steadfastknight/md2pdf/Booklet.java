package com.steadfastknight.md2pdf;

import com.steadfastknight.md2pdf.FontRegistry.Font;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Top-level pipeline + filename helpers, mirroring the Python generate(),
 * title_from_md() and default_out_name().
 */
public final class Booklet {
    private Booklet() {}

    public record GenResult(int sheets, double wMm, double hMm) {}

    /** Title from the first line of Markdown (leading '#' + illegal chars stripped). */
    public static String titleFromMd(String mdText, String fallback) {
        int nl = indexOfNewline(mdText);
        String first = (nl < 0) ? mdText : mdText.substring(0, nl);
        String title = first.replaceFirst("^#+", "").strip()
                .replaceAll("[<>:\"/\\\\|?*]", "");
        return title.isEmpty() ? fallback : title;
    }

    private static int indexOfNewline(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r') return i;
        }
        return -1;
    }

    /** "<title> - <A4|A5|A6> <N>up.pdf" -- mirrors default_out_name. */
    public static String defaultOutName(String title, int perSheet) {
        return title + " - " + Layout.SIZE_LABEL.get(perSheet) + " " + perSheet + "up.pdf";
    }

    public static GenResult generate(Path mdPath, String fontKey, String outPath, int perSheet)
            throws IOException {
        String mdText = Files.readString(mdPath, StandardCharsets.UTF_8);
        Font font = FontRegistry.FONTS.get(fontKey);
        byte[] a6 = PdfRenderer.renderA6(mdText, font);
        Imposer.Result r = Imposer.impose(a6, outPath, perSheet);
        return new GenResult(r.sheets(), r.dwPt() / Layout.MM, r.dhPt() / Layout.MM);
    }
}
