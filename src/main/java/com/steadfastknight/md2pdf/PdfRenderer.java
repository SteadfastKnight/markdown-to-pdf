package com.steadfastknight.md2pdf;

import com.steadfastknight.md2pdf.FontRegistry.Font;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Markdown -> A6 PDF (bytes), one logical page per A6 sheet -- the openhtmltopdf
 * equivalent of the Python render_a6(). The @page rule in {@link Css} drives A6
 * pagination; the regular+bold TTFs are registered as the "Body" family.
 */
public final class PdfRenderer {
    private PdfRenderer() {}

    public static byte[] renderA6(String mdText, Font font) throws IOException {
        String body = MarkdownRenderer.toBody(mdText);
        String css = Css.css(font);
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"/><style>"
                + css + "</style></head><body>" + body + "</body></html>";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.useFont(() -> resource(font.reg()), "Body", 400, FontStyle.NORMAL, true);
        builder.useFont(() -> resource(font.bold()), "Body", 700, FontStyle.NORMAL, true);
        builder.withHtmlContent(html, null);
        builder.toStream(out);
        builder.run();
        return out.toByteArray();
    }

    /** Opens a bundled font as a classpath resource stream. */
    private static InputStream resource(String file) {
        InputStream in = PdfRenderer.class.getResourceAsStream(FontRegistry.fontResource(file));
        if (in == null) {
            throw new IllegalStateException("Bundled font not found on classpath: " + file);
        }
        return in;
    }
}
