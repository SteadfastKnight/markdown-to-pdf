package com.steadfastknight.md2pdf;

import com.steadfastknight.md2pdf.Layout.Grid;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.steadfastknight.md2pdf.Layout.*;

/**
 * Places rendered A6 pages N-up on A4 with inner cut lines + page numbers --
 * the PDFBox port of the Python impose(). PyMuPDF uses a top-left origin (y
 * down); PDFBox uses a bottom-left origin (y up), so every y is flipped here.
 */
public final class Imposer {
    private Imposer() {}

    /** Final placed-page size (points) of the last cell + total A4 sheet count. */
    public record Result(int sheets, double dwPt, double dhPt) {}

    private record Placed(PDFormXObject form, Matrix matrix) {}

    public static Result impose(byte[] a6Bytes, String outPath, int perSheet) throws IOException {
        Grid grid = LAYOUTS.get(perSheet);
        int cols = grid.cols(), rows = grid.rows(), rot = grid.rot();
        int per = grid.per();

        double cellW = (A4_W - 2 * EDGE - (cols - 1) * GUTTER) / cols;
        double cellH = (A4_H - 2 * EDGE - (rows - 1) * GUTTER) / rows;

        double dw = 0, dh = 0;

        try (PDDocument src = PDDocument.load(a6Bytes);
             PDDocument out = new PDDocument()) {

            LayerUtility lu = new LayerUtility(out);
            int n = src.getNumberOfPages();

            for (int start = 0; start < n; start += per) {
                PDPage sheet = new PDPage(new PDRectangle((float) A4_W, (float) A4_H));
                out.addPage(sheet);

                List<Placed> placed = new ArrayList<>();
                for (int i = 0; i < per; i++) {
                    int pno = start + i;
                    if (pno >= n) break;

                    int col = i % cols, row = i / cols;
                    double x0 = EDGE + col * (cellW + GUTTER);
                    double y0 = EDGE + row * (cellH + GUTTER);   // from sheet TOP

                    PDPage sp = src.getPage(pno);
                    PDRectangle box = sp.getMediaBox();
                    double pw = box.getWidth(), ph = box.getHeight();

                    stampPageNumber(src, sp, pno + 1, pw, ph);

                    // footprint after the optional 90 deg turn
                    double fw = (rot == 90 || rot == 270) ? ph : pw;
                    double fh = (rot == 90 || rot == 270) ? pw : ph;
                    double scale = Math.min(cellW / fw, cellH / fh);
                    dw = fw * scale;
                    dh = fh * scale;

                    // centre the footprint in the cell (top-left coords)
                    double dx = x0 + (cellW - dw) / 2;
                    double dyTop = y0 + (cellH - dh) / 2;
                    // bottom-left of the placed box in PDFBox (bottom-origin) coords
                    double imgX = dx;
                    double imgYBottom = A4_H - (dyTop + dh);

                    PDFormXObject form = lu.importPageAsForm(src, pno);
                    placed.add(new Placed(form, transform(rot, scale, imgX, imgYBottom, dw, dh)));
                }

                try (PDPageContentStream cs =
                             new PDPageContentStream(out, sheet, AppendMode.APPEND, true, true)) {
                    for (Placed p : placed) {
                        cs.saveGraphicsState();
                        cs.transform(p.matrix());
                        cs.drawForm(p.form());
                        cs.restoreGraphicsState();
                    }
                    drawInnerCutLines(cs, cols, rows, cellW, cellH);
                }
            }

            int sheets = out.getNumberOfPages();
            out.save(outPath);
            return new Result(sheets, dw, dh);
        }
    }

    /** Builds the form->sheet transform: scale-to-fit, optional 90 deg turn, place. */
    private static Matrix transform(int rot, double s, double imgX, double imgYBottom,
                                    double dw, double dh) {
        AffineTransform at = new AffineTransform();
        if (rot == 90) {
            // counter-clockwise, matching PyMuPDF show_pdf_page(rotate=90):
            // source (x,y) -> (-y,x); land in the target box
            at.translate(imgX + dw, imgYBottom);
            at.rotate(Math.toRadians(90));
            at.scale(s, s);
        } else {
            at.translate(imgX, imgYBottom);
            at.scale(s, s);
        }
        return new Matrix(at);
    }

    /**
     * Page number on the SOURCE page, bottom-right (Times-Roman 8pt, grey) so it
     * turns together with the page in rotated layouts -- mirrors the Python
     * insert_text at (pw-5.5mm-tw, ph-4mm) with a top-left origin.
     */
    private static void stampPageNumber(PDDocument src, PDPage sp, int number,
                                        double pw, double ph) throws IOException {
        String label = Integer.toString(number);
        double tw = PDType1Font.TIMES_ROMAN.getStringWidth(label) / 1000.0 * 8.0;
        double x = pw - 5.5 * MM - tw;
        double yBottom = 4.0 * MM;   // 4 mm above the bottom edge
        try (PDPageContentStream cs =
                     new PDPageContentStream(src, sp, AppendMode.APPEND, true, true)) {
            cs.setNonStrokingColor(new Color(0.35f, 0.35f, 0.35f));
            cs.beginText();
            cs.setFont(PDType1Font.TIMES_ROMAN, 8);
            cs.newLineAtOffset((float) x, (float) yBottom);
            cs.showText(label);
            cs.endText();
        }
    }

    /** Inner cut lines only (no outer border), grey 0.3pt -- generalised from cols/rows. */
    private static void drawInnerCutLines(PDPageContentStream cs, int cols, int rows,
                                          double cellW, double cellH) throws IOException {
        cs.setStrokingColor(new Color(0.6f, 0.6f, 0.6f));
        cs.setLineWidth(0.3f);
        for (int c = 1; c < cols; c++) {
            float x = (float) (EDGE + c * (cellW + GUTTER));
            cs.moveTo(x, 0);
            cs.lineTo(x, (float) A4_H);
            cs.stroke();
        }
        for (int r = 1; r < rows; r++) {
            float y = (float) (A4_H - (EDGE + r * (cellH + GUTTER)));  // flip to bottom-origin
            cs.moveTo(0, y);
            cs.lineTo((float) A4_W, y);
            cs.stroke();
        }
    }
}
