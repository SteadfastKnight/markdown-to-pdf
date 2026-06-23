package com.steadfastknight.md2pdf.ui;

import com.steadfastknight.md2pdf.Layout;
import com.steadfastknight.md2pdf.Layout.Grid;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

/**
 * A small live schematic of the A4 imposition for the selected layout: the
 * sheet, the inner cut lines, and a stylised mini-page in each cell (hole-punch
 * margin band, text lines, page number) -- rotated to match 2-up. Mirrors what
 * {@link com.steadfastknight.md2pdf.Imposer} actually produces.
 */
public class SheetPreview extends Canvas {

    private static final Color PAGE     = Color.web("#fbf8f1");
    private static final Color PAGE_EDGE= Color.web("#c8bb9d");
    private static final Color TEXT_LN  = Color.web("#cdc2a8");
    private static final Color MARGIN   = Color.web("#b1442f", 0.12);
    private static final Color CUT      = Color.web("#b1442f", 0.60);
    private static final Color PAGENO   = Color.web("#b1442f");
    private static final Color SHADOW   = Color.web("#000000", 0.28);

    private int perSheet = 4;

    public SheetPreview(double w, double h) {
        super(w, h);
        DropShadow ds = new DropShadow(16, 0, 8, SHADOW);
        setEffect(ds);
        render(perSheet);
    }

    public void render(int per) {
        this.perSheet = per;
        Grid g = Layout.LAYOUTS.get(per);
        GraphicsContext gc = getGraphicsContext2D();
        double W = getWidth(), H = getHeight();
        gc.clearRect(0, 0, W, H);

        // A4 page box (sqrt(2) aspect), centred with breathing room.
        double pad = 14;
        double availW = W - 2 * pad, availH = H - 2 * pad;
        double pw = availW, ph = pw * Math.sqrt(2);
        if (ph > availH) { ph = availH; pw = ph / Math.sqrt(2); }
        double px = (W - pw) / 2, py = (H - ph) / 2;

        gc.setFill(PAGE);
        gc.fillRect(px, py, pw, ph);
        gc.setStroke(PAGE_EDGE);
        gc.setLineWidth(1);
        gc.strokeRect(px, py, pw, ph);

        int cols = g.cols(), rows = g.rows();
        double edge = pw * (Layout.EDGE / Layout.A4_W);
        double cellW = (pw - 2 * edge) / cols;
        double cellH = (ph - 2 * edge) / rows;
        boolean rotated = g.rot() != 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double cx = px + edge + c * cellW;
                double cy = py + edge + r * cellH;
                miniPage(gc, cx + cellW * 0.06, cy + cellH * 0.06,
                        cellW * 0.88, cellH * 0.88, rotated);
            }
        }

        // inner cut lines only
        gc.setStroke(CUT);
        gc.setLineWidth(1);
        gc.setLineDashes(4, 3);
        for (int c = 1; c < cols; c++) {
            double x = px + edge + c * cellW;
            gc.strokeLine(x, py, x, py + ph);
        }
        for (int r = 1; r < rows; r++) {
            double y = py + edge + r * cellH;
            gc.strokeLine(px, y, px + pw, y);
        }
        gc.setLineDashes(null);
    }

    /** A stylised page: hole-punch margin band, text lines, page number. */
    private void miniPage(GraphicsContext gc, double x, double y, double w, double h, boolean rotated) {
        if (!rotated) {
            double marginW = w * (12.0 / 105.0);
            gc.setFill(MARGIN);
            gc.fillRect(x, y, marginW, h);

            gc.setFill(TEXT_LN);
            double lx = x + marginW + w * 0.06, rx = x + w * 0.92;
            double gap = h * 0.072, lineH = Math.max(1.2, h * 0.02);
            for (int i = 0; i < 11 && y + h * 0.10 + i * gap < y + h - h * 0.10; i++) {
                double yy = y + h * 0.10 + i * gap;
                double ww = (i % 4 == 3) ? (rx - lx) * 0.6 : (rx - lx);
                gc.fillRect(lx, yy, ww, lineH);
            }
            gc.setFill(PAGENO);
            gc.fillOval(x + w - w * 0.13, y + h - h * 0.11, Math.max(2, w * 0.035), Math.max(2, w * 0.035));
        } else {
            // 90 deg CCW: hole-punch margin lands at the bottom, lines run vertical
            double marginH = h * (12.0 / 105.0);
            gc.setFill(MARGIN);
            gc.fillRect(x, y + h - marginH, w, marginH);

            gc.setFill(TEXT_LN);
            double topY = y + h * 0.08, botY = y + h - marginH - h * 0.06;
            double gap = w * 0.072, lineW = Math.max(1.2, w * 0.02);
            for (int i = 0; i < 13 && x + w * 0.08 + i * gap < x + w * 0.92; i++) {
                double xx = x + w * 0.08 + i * gap;
                double hh = (i % 4 == 3) ? (botY - topY) * 0.6 : (botY - topY);
                gc.fillRect(xx, topY, lineW, hh);
            }
            // source bottom-right -> top-right after the turn
            gc.setFill(PAGENO);
            gc.fillOval(x + w - w * 0.10, y + h * 0.08, Math.max(2, h * 0.035), Math.max(2, h * 0.035));
        }
    }
}
