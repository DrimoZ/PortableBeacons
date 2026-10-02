import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The logo: the Portable Beacon as an object, in the isometric view the game gives blocks in an
 * inventory, on Immaterial Drawers' plate - so the mods' icons read as a set in a launcher.
 *
 * <p>The object is the item's own design in three dimensions: an obsidian foot with the tier's four
 * lit pips, the metal band, a glass dome with the beam's core glowing inside. Every face is drawn in
 * texture pixels and lit like a block - top brightest, left mid, right darkest - so it sits beside
 * Minecraft art rather than on top of it.
 *
 * <p>512 x 512: the mod list, CurseForge's project avatar and the GitHub social preview all scale it
 * down from there.
 *
 * <pre>java tools/GenerateLogo.java</pre>
 */
public final class GenerateLogo {

    private static final int SIZE = 512;
    /** Screen pixels per texture pixel. */
    private static final double K = 15;
    private static final double COS = Math.cos(Math.toRadians(30));
    private static final double SIN = 0.5;

    private static final Color PLATE_TOP = new Color(0x2A2244);
    private static final Color PLATE_BOTTOM = new Color(0x100D1C);
    private static final Color LIGHT = new Color(0x55D0E0);

    private static double cx;
    private static double cy;

    public static void main(String[] args) throws IOException {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D plate = new RoundRectangle2D.Double(16, 16, SIZE - 32, SIZE - 32, 104, 104);
        g.setPaint(new GradientPaint(0, 16, PLATE_TOP, 0, SIZE - 16, PLATE_BOTTOM));
        g.fill(plate);
        g.setClip(plate);
        grid(g);

        // The object's origin: the centre of its foot's base, placed so the whole thing is centred.
        cx = SIZE / 2.0;
        cy = SIZE / 2.0 + 100;

        glow(g, cx, cy - 15 * K, 240, 110);
        shadow(g);

        // Foot: obsidian, 14 wide, 4 tall, with the four pips on its left face.
        box(g, -7, 0, -7, 7, 4, 7, (face, u, v) -> obsidian(face, u, v));
        // Band: the tier's metal, slightly inset.
        box(g, -6, 4, -6, 6, 6, 6, (face, u, v) -> metal(face, v));
        backEdge(g, -5, 6, -5, 15);
        // Core, then the glass dome over it.
        box(g, -3, 7.5, -3, 3, 13.5, 3, (face, u, v) -> core(face, u, v));
        glass(g, -5, 6, -5, 5, 15, 5);
        glow(g, project(0, 10, 0)[0], project(0, 10, 0)[1], 120, 140);

        g.setClip(null);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(255, 255, 255, 34));
        g.draw(plate);
        g.dispose();
        ImageIO.write(out, "PNG", new File("src/main/resources/logo.png"));
        System.out.println("Logo written.");
    }

    // ------------------------------------------------------------------ the object's textures

    private interface Texture {
        /** @param face 0 top, 1 left (+z), 2 right (+x); u, v in texture pixels from the face's corner */
        Color at(int face, int u, int v);
    }

    private static Color obsidian(int face, int u, int v) {
        int base = ((u * 7 + v * 13) % 5 == 0) ? 0x2E2347 : ((u + v) % 3 == 0 ? 0x251C3B : 0x2A2041);
        if (face != 0 && v == 1 && (u == 3 || u == 5 || u == 8 || u == 10)) {
            return new Color(0xF2F2F2); // the four tier pips
        }
        if (v == 3 && face != 0) {
            base = 0x3A2E52; // the lit rim under the band
        }
        return new Color(base);
    }

    /** The item's band: its top is seen through the glass, so it is dark, as on the item. */
    private static Color metal(int face, int v) {
        return new Color(face == 0 ? 0x2A2041 : v == 1 ? 0x7A6A74 : 0x4E4249);
    }

    private static Color core(int face, int u, int v) {
        boolean centre = u >= 2 && u <= 3 && v >= 2 && v <= 3;
        boolean ring = u >= 1 && u <= 4 && v >= 1 && v <= 4;
        return new Color(centre ? 0xC8FAFF : ring ? 0x8EE5EF : 0x55D0E0);
    }

    // ------------------------------------------------------------------ drawing

    /** A box drawn by its three visible faces, each in texture pixels, lit like a block. */
    private static void box(Graphics2D g, double x0, double y0, double z0, double x1, double y1, double z1,
                            Texture texture) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        // Left face (z = z1): u along x, v along y.
        for (double x = x0; x < x1; x++) {
            for (double y = y0; y < y1; y++) {
                g.setColor(shade(texture.at(1, (int) (x - x0), (int) (y - y0)), 0.82));
                g.fill(quad(x, y, z1, x + 1, y, z1, x + 1, y + 1, z1, x, y + 1, z1));
            }
        }
        // Right face (x = x1): u along z, v along y.
        for (double z = z0; z < z1; z++) {
            for (double y = y0; y < y1; y++) {
                g.setColor(shade(texture.at(2, (int) (z1 - z - 1), (int) (y - y0)), 0.6));
                g.fill(quad(x1, y, z, x1, y, z + 1, x1, y + 1, z + 1, x1, y + 1, z));
            }
        }
        // Top (y = y1).
        for (double x = x0; x < x1; x++) {
            for (double z = z0; z < z1; z++) {
                g.setColor(texture.at(0, (int) (x - x0), (int) (z - z0)));
                g.fill(quad(x, y1, z, x + 1, y1, z, x + 1, y1, z + 1, x, y1, z + 1));
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** The dome: tinted glass faces, a pixel frame along every visible edge, and a highlight. */
    private static void glass(Graphics2D g, double x0, double y0, double z0, double x1, double y1, double z1) {
        Color tint = new Color(168, 244, 250, 46);
        g.setColor(tint);
        g.fill(quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1));
        g.setColor(new Color(120, 200, 215, 60));
        g.fill(quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0));
        g.setColor(new Color(200, 250, 255, 70));
        g.fill(quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1));

        // The frame: one texture pixel wide, as glass is drawn in the game.
        Color frame = new Color(214, 250, 255, 230);
        Color frameDark = new Color(150, 210, 222, 230);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(frame);
        g.fill(quad(x0, y1 - 1, z1, x1, y1 - 1, z1, x1, y1, z1, x0, y1, z1));
        g.fill(quad(x0, y0, z1, x0 + 1, y0, z1, x0 + 1, y1, z1, x0, y1, z1));
        g.fill(quad(x1 - 1, y0, z1, x1, y0, z1, x1, y1, z1, x1 - 1, y1, z1));
        g.setColor(frameDark);
        g.fill(quad(x1, y1 - 1, z0, x1, y1 - 1, z1, x1, y1, z1, x1, y1, z0));
        g.fill(quad(x1, y0, z0, x1, y0, z0 + 1, x1, y1, z0 + 1, x1, y1, z0));
        g.setColor(new Color(235, 255, 255, 235));
        g.fill(quad(x0, y1, z0, x1, y1, z0, x1, y1, z0 + 1, x0, y1, z0 + 1));
        g.fill(quad(x0, y1, z0, x0 + 1, y1, z0, x0 + 1, y1, z1, x0, y1, z1));
        // A highlight across the left pane, as on glass in the game.
        g.setColor(new Color(255, 255, 255, 120));
        g.fill(quad(x0 + 2, y1 - 3, z1, x0 + 3, y1 - 3, z1, x0 + 3, y1 - 2, z1, x0 + 2, y1 - 2, z1));
        g.fill(quad(x0 + 3, y1 - 4, z1, x0 + 4, y1 - 4, z1, x0 + 4, y1 - 3, z1, x0 + 3, y1 - 3, z1));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    /** The dome's far vertical edge, faint: without it the glass reads as an open frame. */
    private static void backEdge(Graphics2D g, double x, double y0, double z, double y1) {
        g.setColor(new Color(214, 250, 255, 70));
        g.fill(quad(x, y0, z, x + 1, y0, z, x + 1, y1, z, x, y1, z));
    }

    private static double[] project(double x, double y, double z) {
        return new double[]{cx + (x - z) * K * COS, cy + (x + z) * K * SIN - y * K};
    }

    private static Polygon quad(double... xyz) {
        Polygon p = new Polygon();
        for (int i = 0; i < xyz.length; i += 3) {
            double[] s = project(xyz[i], xyz[i + 1], xyz[i + 2]);
            p.addPoint((int) Math.round(s[0]), (int) Math.round(s[1]));
        }
        return p;
    }

    private static void shadow(Graphics2D g) {
        double[] c = project(0, 0, 0);
        float r = (float) (12 * K);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(c[0], c[1] + K * 2), r, new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 150), new Color(0, 0, 0, 0)}));
        g.fill(new Ellipse2D.Double(c[0] - r, c[1] + K * 2 - r * 0.5, r * 2, r));
    }

    private static void glow(Graphics2D g, double x, double y, float radius, int alpha) {
        g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), radius, new float[]{0f, 1f},
                new Color[]{new Color(LIGHT.getRed(), LIGHT.getGreen(), LIGHT.getBlue(), alpha),
                        new Color(LIGHT.getRed(), LIGHT.getGreen(), LIGHT.getBlue(), 0)}));
        g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    private static void grid(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 12));
        for (int i = 16; i < SIZE; i += 32) {
            g.drawLine(i, 0, i, SIZE);
            g.drawLine(0, i, SIZE, i);
        }
    }

    private static Color shade(Color c, double f) {
        return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f));
    }

    private GenerateLogo() {}
}
