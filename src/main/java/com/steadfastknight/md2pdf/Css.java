package com.steadfastknight.md2pdf;

import com.steadfastknight.md2pdf.FontRegistry.Font;

import java.util.Locale;

/**
 * Builds the user CSS, mirroring the Python _css(). Two differences from the
 * original, both required by the openhtmltopdf engine:
 *   - No @font-face rules: the regular+bold TTFs are registered on the renderer
 *     via PdfRendererBuilder.useFont(..., "Body", ...) instead.
 *   - An @page rule sets the A6 page box + margins (hole-punch left), so the
 *     engine paginates content into A6 pages -- the equivalent of the Python
 *     mediabox/where rectangles and the Story.place() loop.
 */
public final class Css {
    private Css() {}

    public static String css(Font font) {
        double size = font.size();
        return String.format(Locale.ROOT, """
                @page { size: 105mm 148.5mm; margin: 10mm 5.5mm 10mm 12mm; }
                * { font-family: Body; }
                body { font-size: %.2fpt; line-height: 1.34; color: #111; text-align: justify; }
                h1 { font-size: %.2fpt; font-weight: bold; line-height: 1.15;
                     margin: 0 0 2.5mm 0; padding-bottom: 1mm; border-bottom: 0.5pt solid #999; }
                h2 { font-size: %.2fpt; font-weight: bold; margin: 3mm 0 1mm 0; }
                p  { margin: 0 0 1.6mm 0; }
                ol, ul { margin: 0 0 1.6mm 0; padding-left: 4.5mm; }
                li { margin: 0 0 1.2mm 0; }
                /* lighter, smaller bullet than openhtmltopdf's default disc */
                ul { list-style: none; }
                ul > li:before { content: "\\2022"; display: inline-block;
                     width: 3.2mm; margin-left: -3.2mm; color: #555; font-size: 0.78em; }
                """, size, size * 1.6, size * 1.15);
    }
}
