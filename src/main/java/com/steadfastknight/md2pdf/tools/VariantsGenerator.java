package com.steadfastknight.md2pdf.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders a labelled contact sheet of candidate app-icon concepts so one can be
 * chosen before wiring it in. Run:
 *   java -cp target/classes com.steadfastknight.md2pdf.tools.VariantsGenerator
 * Output: preview-variants.png in the project root.
 */
public final class VariantsGenerator {

    static final Color INK     = new Color(0x21, 0x1b, 0x16);
    static final Color INK2    = new Color(0x33, 0x2a, 0x1b);
    static final Color PAPER   = new Color(0xf4, 0xef, 0xe4);
    static final Color PAPER2  = new Color(0xe7, 0xdd, 0xca);
    static final Color PAPERSH = new Color(0xcf, 0xc2, 0xa6);
    static final Color ACCENT  = new Color(0xb1, 0x44, 0x2f);
    static final Color TEXTLN  = new Color(0x6b, 0x62, 0x53);

    interface Icon { void draw(Graphics2D g, double s); String name(); }

    public static void main(String[] args) throws Exception {
        Icon[] icons = {
                named("1 · Dog-ear",   VariantsGenerator::dogEar),
                named("2 · Imposition",VariantsGenerator::grid),
                named("3 · Open book", VariantsGenerator::book),
                named("4 · Monogram",  VariantsGenerator::monogram),
                named("5 · Saddle fold",VariantsGenerator::fold),
                named("6 · Stack",     VariantsGenerator::stack),
        };

        int big = 132, small = 52, colW = 150, pad = 24;
        int W = icons.length * colW + pad * 2;
        int H = pad + 22 + big + 18 + small + 30;
        BufferedImage sheet = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0xfb, 0xf9, 0xf3));
        g.fillRect(0, 0, W, H);

        for (int i = 0; i < icons.length; i++) {
            double cx = pad + i * colW + (colW - big) / 2.0;
            double bigY = pad + 22;
            drawAt(g, icons[i], cx, bigY, big);
            double sx = pad + i * colW + (colW - small) / 2.0;
            drawAt(g, icons[i], sx, bigY + big + 18, small);

            g.setColor(INK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            String n = icons[i].name();
            int tw = g.getFontMetrics().stringWidth(n);
            g.drawString(n, (int) (pad + i * colW + (colW - tw) / 2.0), H - 14);
        }
        g.dispose();
        File out = new File("preview-variants.png");
        ImageIO.write(sheet, "png", out);
        System.out.println("wrote " + out.getAbsolutePath());
    }

    private static void drawAt(Graphics2D g, Icon ic, double x, double y, double s) {
        Graphics2D h = (Graphics2D) g.create();
        h.translate(x, y);
        ic.draw(h, s);
        h.dispose();
    }

    private static Icon named(String n, java.util.function.BiConsumer<Graphics2D, Double> d) {
        return new Icon() {
            public void draw(Graphics2D g, double s) { d.accept(g, s); }
            public String name() { return n; }
        };
    }

    // ---- bases ----
    private static void baseInk(Graphics2D g, double s) {
        double r = s * 0.22;
        g.setPaint(new GradientPaint(0, 0, INK2, 0, (float) s, INK));
        g.fill(new RoundRectangle2D.Double(0, 0, s, s, r, r));
    }
    private static void basePaper(Graphics2D g, double s) {
        double r = s * 0.22;
        g.setPaint(new GradientPaint(0, 0, PAPER, 0, (float) s, PAPER2));
        g.fill(new RoundRectangle2D.Double(0, 0, s, s, r, r));
        g.setColor(PAPERSH);
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.012)));
        g.draw(new RoundRectangle2D.Double(0.5, 0.5, s - 1, s - 1, r, r));
    }

    // ---- 1 dog-ear ----
    private static void dogEar(Graphics2D g, double s) {
        baseInk(g, s);
        double m = s * 0.20, x0 = m, y0 = m * 0.95, x1 = s - m, y1 = s - m * 0.95;
        double fold = (x1 - x0) * 0.30;
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(x0 + s * 0.02, y0 + s * 0.03, x1 - x0, y1 - y0, s * 0.05, s * 0.05));
        GeneralPath p = path(x0, y0, x1 - fold, y0, x1, y0 + fold, x1, y1, x0, y1);
        g.setColor(PAPER); g.fill(p);
        g.setColor(ACCENT); g.fill(path(x1 - fold, y0, x1 - fold, y0 + fold, x1, y0 + fold));
        if (s >= 48) lines(g, x0 + (x1 - x0) * 0.16, y0 + fold + (y1 - y0) * 0.16, (x1 - x0) * 0.68, (y1 - y0) * 0.5, s);
    }

    // ---- 2 imposition grid ----
    private static void grid(Graphics2D g, double s) {
        baseInk(g, s);
        double pw = s * 0.54, ph = pw * 1.41;
        if (ph > s * 0.66) { ph = s * 0.66; pw = ph / 1.41; }
        double x = (s - pw) / 2, y = (s - ph) / 2;
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new RoundRectangle2D.Double(x + s * 0.02, y + s * 0.025, pw, ph, s * 0.04, s * 0.04));
        g.setColor(PAPER); g.fill(new Rectangle2D.Double(x, y, pw, ph));
        double mx = x + pw / 2, my = y + ph / 2;
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke((float) Math.max(1, s * 0.018), BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_MITER, 1, new float[]{(float) (s * 0.04), (float) (s * 0.03)}, 0));
        g.draw(new Line2D.Double(mx, y, mx, y + ph));
        g.draw(new Line2D.Double(x, my, x + pw, my));
        // page dots
        g.setStroke(new BasicStroke(1));
        double d = Math.max(2, s * 0.045);
        for (double[] c : new double[][]{{mx - pw * 0.12, my - ph * 0.06}, {x + pw - pw * 0.10, my - ph * 0.06},
                {mx - pw * 0.12, y + ph - ph * 0.05}, {x + pw - pw * 0.10, y + ph - ph * 0.05}})
            g.fill(new Ellipse2D.Double(c[0], c[1], d, d));
    }

    // ---- 3 open book ----
    private static void book(Graphics2D g, double s) {
        baseInk(g, s);
        double cx = s / 2, w = s * 0.30, top = s * 0.30, bot = s * 0.72, dip = s * 0.06;
        GeneralPath left = path(cx, top, cx - w, top + dip, cx - w, bot, cx, bot - dip * 0.6);
        GeneralPath right = path(cx, top, cx + w, top + dip, cx + w, bot, cx, bot - dip * 0.6);
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(path(cx, top + s * 0.03, cx - w, top + dip + s * 0.03, cx - w, bot + s * 0.03, cx, bot - dip * 0.6 + s * 0.03));
        g.setColor(PAPER); g.fill(left); g.fill(right);
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke((float) Math.max(1.4, s * 0.02)));
        g.draw(new Line2D.Double(cx, top, cx, bot - dip * 0.6));
        if (s >= 48) {
            g.setColor(TEXTLN);
            g.setStroke(new BasicStroke((float) Math.max(1, s * 0.016)));
            for (int i = 0; i < 4; i++) {
                double yy = top + dip + s * 0.06 + i * s * 0.10;
                g.draw(new Line2D.Double(cx - w * 0.82, yy + i * 1.0, cx - w * 0.12, yy - s * 0.012 + i * 1.0));
                g.draw(new Line2D.Double(cx + w * 0.12, yy - s * 0.012 + i * 1.0, cx + w * 0.82, yy + i * 1.0));
            }
        }
    }

    // ---- 4 monogram ----
    private static Font EBG;
    private static void monogram(Graphics2D g, double s) {
        basePaper(g, s);
        if (EBG == null) {
            try { EBG = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/resources/fonts/EBGaramond-Bold.ttf")); }
            catch (Exception e) { EBG = new Font("Serif", Font.BOLD, 10); }
        }
        Font f = EBG.deriveFont(Font.BOLD, (float) (s * 0.62));
        g.setFont(f);
        g.setColor(INK);
        FontMetrics fm = g.getFontMetrics();
        String m = "M";
        int tw = fm.stringWidth(m);
        double tx = (s - tw) / 2.0, ty = s * 0.5 + fm.getAscent() * 0.36;
        g.drawString(m, (int) tx, (int) ty);
        g.setColor(ACCENT);
        g.fill(new RoundRectangle2D.Double(s * 0.34, s * 0.72, s * 0.32, Math.max(2, s * 0.04),
                s * 0.04, s * 0.04));
    }

    // ---- 5 saddle fold ----
    private static void fold(Graphics2D g, double s) {
        baseInk(g, s);
        double m = s * 0.20, x = m, y = m * 1.05, w = s - 2 * m, hh = s - 2 * m * 1.05;
        g.setColor(new Color(0, 0, 0, 55));
        g.fill(new RoundRectangle2D.Double(x + s * 0.02, y + s * 0.03, w, hh, s * 0.05, s * 0.05));
        g.setColor(PAPER);
        g.fill(new RoundRectangle2D.Double(x, y, w, hh, s * 0.05, s * 0.05));
        // right half slightly shaded -> implies a fold
        g.setColor(new Color(0xe2, 0xd8, 0xc4));
        g.fill(new Rectangle2D.Double(x + w / 2, y + 1, w / 2 - s * 0.025, hh - 2));
        // crease
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke((float) Math.max(1.4, s * 0.02), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1, new float[]{(float) (s * 0.05), (float) (s * 0.035)}, 0));
        g.draw(new Line2D.Double(x + w / 2, y + s * 0.02, x + w / 2, y + hh - s * 0.02));
        if (s >= 48) {
            lines(g, x + w * 0.10, y + hh * 0.18, w * 0.32, hh * 0.62, s);
            lines(g, x + w / 2 + w * 0.06, y + hh * 0.18, w * 0.32, hh * 0.62, s);
        }
    }

    // ---- 6 stack ----
    private static void stack(Graphics2D g, double s) {
        baseInk(g, s);
        double m = s * 0.22, w = s * 0.50, hh = w * 1.18;
        double x = (s - w) / 2 - s * 0.04, y = (s - hh) / 2 - s * 0.04;
        // back sheet
        g.setColor(PAPER2);
        g.fill(new RoundRectangle2D.Double(x + s * 0.10, y + s * 0.10, w, hh, s * 0.04, s * 0.04));
        // front sheet with dog-ear
        double fold = w * 0.28;
        g.setColor(new Color(0, 0, 0, 50));
        g.fill(path(x + s * 0.02, y + s * 0.03, x + w - fold + s * 0.02, y + s * 0.03,
                x + w + s * 0.02, y + fold + s * 0.03, x + w + s * 0.02, y + hh + s * 0.03, x + s * 0.02, y + hh + s * 0.03));
        g.setColor(PAPER);
        g.fill(path(x, y, x + w - fold, y, x + w, y + fold, x + w, y + hh, x, y + hh));
        g.setColor(ACCENT);
        g.fill(path(x + w - fold, y, x + w - fold, y + fold, x + w, y + fold));
        if (s >= 48) lines(g, x + w * 0.16, y + fold + hh * 0.12, w * 0.64, hh * 0.5, s);
    }

    // ---- helpers ----
    private static void lines(Graphics2D g, double lx, double ty, double w, double h, double s) {
        double gap = h / 4.2, lh = Math.max(1.4, s * 0.026);
        for (int i = 0; i < 4; i++) {
            double ww = i == 0 ? w : (i == 3 ? w * 0.55 : w * 0.85);
            g.setColor(i == 0 ? ACCENT : TEXTLN);
            g.fill(new RoundRectangle2D.Double(lx, ty + i * gap, ww, lh, lh, lh));
        }
    }
    private static GeneralPath path(double... xy) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }
}
