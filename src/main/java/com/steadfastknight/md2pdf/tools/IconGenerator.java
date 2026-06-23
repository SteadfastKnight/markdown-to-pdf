package com.steadfastknight.md2pdf.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * One-off asset generator for the app icon: a saddle-fold booklet (a sheet with
 * a centre fold crease) in the app's ink / paper / vermilion palette. Produces
 * PNGs at several sizes plus a
 * Windows .ico (PNG-encoded entries). Run manually:
 *   java -cp target/classes com.steadfastknight.md2pdf.tools.IconGenerator
 * Output lands in src/main/resources/icon/.
 */
public final class IconGenerator {

    private static final Color INK      = new Color(0x21, 0x1b, 0x16);
    private static final Color INK_2    = new Color(0x33, 0x2a, 0x1b);
    private static final Color PAPER    = new Color(0xf4, 0xef, 0xe4);
    private static final Color PAPER_SH = new Color(0xd9, 0xcd, 0xb4);
    private static final Color ACCENT   = new Color(0xb1, 0x44, 0x2f);
    private static final Color TEXT_LN  = new Color(0x6b, 0x62, 0x53);

    private static final int[] SIZES = {16, 32, 48, 64, 128, 256};

    public static void main(String[] args) throws IOException {
        File dir = new File("src/main/resources/icon");
        dir.mkdirs();

        List<byte[]> pngs = new ArrayList<>();
        for (int s : SIZES) {
            BufferedImage img = render(s);
            File png = new File(dir, "app-" + s + ".png");
            ImageIO.write(img, "png", png);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            pngs.add(bos.toByteArray());
            System.out.println("wrote " + png);
        }
        writeIco(pngs, new File(dir, "app.ico"));
        System.out.println("wrote " + new File(dir, "app.ico"));
    }

    private static BufferedImage render(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double S = s;
        // rounded ink tile with a soft vertical gradient
        double r = S * 0.22;
        g.setPaint(new GradientPaint(0, 0, INK_2, 0, s, INK));
        g.fill(new RoundRectangle2D.Double(0, 0, S, S, r, r));

        // saddle-fold booklet: a sheet with a centre fold crease
        double m = S * 0.20;
        double x = m, y = m * 1.05, w = S - 2 * m, hh = S - 2 * m * 1.05;
        double rr = S * 0.05;

        // drop shadow under the sheet
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new RoundRectangle2D.Double(x + S * 0.02, y + S * 0.03, w, hh, rr, rr));

        // the sheet, with the right half subtly shaded so it reads as folded
        g.setColor(PAPER);
        g.fill(new RoundRectangle2D.Double(x, y, w, hh, rr, rr));
        g.setColor(new Color(0xe2, 0xd8, 0xc4));
        g.fill(new Rectangle2D.Double(x + w / 2, y + 1, w / 2 - S * 0.025, hh - 2));

        // the fold crease (dashed vermilion)
        g.setColor(ACCENT);
        g.setStroke(new BasicStroke((float) Math.max(1.4, S * 0.02), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 1f, new float[]{(float) (S * 0.05), (float) (S * 0.035)}, 0f));
        g.draw(new Line2D.Double(x + w / 2, y + S * 0.02, x + w / 2, y + hh - S * 0.02));

        // a column of text on each half (skip when tiny)
        if (s >= 48) {
            lines(g, x + w * 0.10, y + hh * 0.18, w * 0.32, hh * 0.62, S);
            lines(g, x + w / 2 + w * 0.06, y + hh * 0.18, w * 0.32, hh * 0.62, S);
        }
        g.dispose();
        return img;
    }

    private static void lines(Graphics2D g, double lx, double ty, double w, double h, double s) {
        double gap = h / 4.2, lh = Math.max(1.4, s * 0.026);
        for (int i = 0; i < 4; i++) {
            double ww = (i == 0) ? w : (i == 3 ? w * 0.55 : w * 0.85);
            g.setColor(i == 0 ? ACCENT : TEXT_LN);
            g.fill(new RoundRectangle2D.Double(lx, ty + i * gap, ww, lh, lh, lh));
        }
    }

    /** Minimal ICO container holding PNG-encoded entries (Vista+). */
    private static void writeIco(List<byte[]> pngs, File out) throws IOException {
        int n = pngs.size();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            ByteArrayOutputStream head = new ByteArrayOutputStream();
            writeShortLE(head, 0);   // reserved
            writeShortLE(head, 1);   // type = icon
            writeShortLE(head, n);   // image count

            int offset = 6 + n * 16;
            for (int i = 0; i < n; i++) {
                int size = SIZES[i];
                byte[] data = pngs.get(i);
                head.write(size >= 256 ? 0 : size);   // width  (0 => 256)
                head.write(size >= 256 ? 0 : size);   // height
                head.write(0);                         // palette
                head.write(0);                         // reserved
                writeShortLE(head, 1);                 // color planes
                writeShortLE(head, 32);                // bits per pixel
                writeIntLE(head, data.length);         // bytes in resource
                writeIntLE(head, offset);              // offset
                offset += data.length;
            }
            fos.write(head.toByteArray());
            for (byte[] data : pngs) fos.write(data);
        }
    }

    private static void writeShortLE(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
    }

    private static void writeIntLE(ByteArrayOutputStream o, int v) {
        o.write(v & 0xFF);
        o.write((v >> 8) & 0xFF);
        o.write((v >> 16) & 0xFF);
        o.write((v >> 24) & 0xFF);
    }
}
