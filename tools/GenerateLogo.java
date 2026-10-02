import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * The logo: the Beacon IV item in relief, on Immaterial Drawers' plate, so the mods' icons read as
 * a set in a launcher.
 *
 * <p>The icon is the shipped item texture, built up as cubes - one per pixel, two deep, the way the
 * game draws an item lying on the ground - so the logo cannot drift from the item art: change the
 * texture, rerun this. A flat 16-pixel icon enlarged on a grey plate was what this replaced, and at
 * the 64 px a launcher shows it, it read as a smudge.
 *
 * <p>512 x 512: the mod list, CurseForge's project avatar and the GitHub social preview all scale it
 * down from there.
 *
 * <pre>java tools/GenerateTextures.java &amp;&amp; java tools/GenerateLogo.java</pre>
 */
public final class GenerateLogo {

    private static final int SIZE = 512;
    private static final int DEPTH = 2;
    /** Pixels per texture pixel. */
    private static final int S = 22;
    /** How far one step of depth moves on screen: up and to the right, a three-quarter view. */
    private static final double DX = S * 0.55;
    private static final double DY = -S * 0.40;

    private static final Color PLATE_TOP = new Color(0x2C2346);
    private static final Color PLATE_BOTTOM = new Color(0x110E1E);
    private static final Color BEAM = new Color(0x55D0E0);

    public static void main(String[] args) throws IOException {
        BufferedImage icon = ImageIO.read(new File(
                "src/main/resources/assets/portablebeacons/textures/item/beacon_iv.png"));
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D plate = new RoundRectangle2D.Double(16, 16, SIZE - 32, SIZE - 32, 104, 104);
        g.setPaint(new GradientPaint(0, 16, PLATE_TOP, 0, SIZE - 16, PLATE_BOTTOM));
        g.fill(plate);
        g.setClip(plate);
        grid(g);

        // Where the icon lands: centred, with room for the depth offset.
        int width = (int) (16 * S + DEPTH * DX);
        int ox = (SIZE - width) / 2;
        int oy = (SIZE - 16 * S) / 2 + 30;

        int[] crystal = crystalCentre(icon);
        double cx = ox + crystal[0] * S + S / 2.0 + DX;
        double cy = oy + crystal[1] * S + S / 2.0 + DY;
        glow(g, cx, cy, 230, 150);
        shadow(g, ox, oy);

        voxels(g, icon, ox, oy);
        // Over the dome, from its top: drawn behind, the dome hid all but the tip of it.
        beam(g, cx, oy + topRow(icon, crystal[0]) * S + (DEPTH - 1) * DY + 2);
        glow(g, cx, cy, 90, 70);

        g.setClip(null);
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(255, 255, 255, 34));
        g.draw(plate);
        g.dispose();
        ImageIO.write(out, "PNG", new File("src/main/resources/logo.png"));
        System.out.println("Logo written.");
    }

    /** One cube per opaque pixel, back to front, left to right, bottom to top. */
    private static void voxels(Graphics2D g, BufferedImage icon, int ox, int oy) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (int z = DEPTH - 1; z >= 0; z--) {
            for (int y = 15; y >= 0; y--) {
                for (int x = 0; x < 16; x++) {
                    int argb = icon.getRGB(x, y);
                    if ((argb >>> 24) < 128) {
                        continue;
                    }
                    Color c = new Color(argb);
                    double bx = ox + x * S + z * DX;
                    double by = oy + y * S + z * DY;
                    // Top and right faces, then the front over them.
                    g.setColor(shade(c, 1.22));
                    g.fill(quad(bx, by, bx + S, by, bx + S + DX, by + DY, bx + DX, by + DY));
                    g.setColor(shade(c, 0.62));
                    g.fill(quad(bx + S, by, bx + S + DX, by + DY, bx + S + DX, by + S + DY, bx + S, by + S));
                    g.setColor(c);
                    g.fill(quad(bx, by, bx + S, by, bx + S, by + S, bx, by + S));
                }
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static int topRow(BufferedImage icon, int x) {
        for (int y = 0; y < 16; y++) {
            if ((icon.getRGB(x, y) >>> 24) >= 128) {
                return y;
            }
        }
        return 0;
    }

    /** A soft contact shadow, so the beacon stands on something rather than floating. */
    private static void shadow(Graphics2D g, int ox, int oy) {
        double cx = ox + 8 * S + DX;
        double cy = oy + 16 * S + 4;
        float r = 8 * S;
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), r, new float[]{0f, 1f},
                new Color[]{new Color(0, 0, 0, 120), new Color(0, 0, 0, 0)}));
        g.fill(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r * 0.22, r * 2, r * 0.44));
    }

    /** The brightest pixel of the dome: where the beam starts and the light comes from. */
    private static int[] crystalCentre(BufferedImage icon) {
        int best = -1;
        int[] at = {8, 3};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int argb = icon.getRGB(x, y);
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int b = ((argb >> 16) & 255) + ((argb >> 8) & 255) + (argb & 255);
                if (b > best) {
                    best = b;
                    at = new int[]{x, y};
                }
            }
        }
        return at;
    }

    private static void beam(Graphics2D g, double cx, double cy) {
        int w = 60;
        g.setPaint(new GradientPaint(0, 16, new Color(BEAM.getRed(), BEAM.getGreen(), BEAM.getBlue(), 0),
                0, (float) cy, new Color(BEAM.getRed(), BEAM.getGreen(), BEAM.getBlue(), 230)));
        g.fillRect((int) (cx - w / 2.0), 0, w, (int) cy);
        g.setPaint(new GradientPaint(0, 16, new Color(255, 255, 255, 0), 0, (float) cy, new Color(235, 255, 255, 255)));
        g.fillRect((int) (cx - 9), 0, 18, (int) cy);
    }

    private static void glow(Graphics2D g, double cx, double cy, float radius, int alpha) {
        g.setComposite(AlphaComposite.SrcOver);
        g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), radius, new float[]{0f, 1f},
                new Color[]{new Color(BEAM.getRed(), BEAM.getGreen(), BEAM.getBlue(), alpha),
                        new Color(BEAM.getRed(), BEAM.getGreen(), BEAM.getBlue(), 0)}));
        g.fillOval((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
    }

    private static void grid(Graphics2D g) {
        g.setColor(new Color(255, 255, 255, 13));
        for (int i = 16; i < SIZE; i += 32) {
            g.drawLine(i, 0, i, SIZE);
            g.drawLine(0, i, SIZE, i);
        }
    }

    private static Polygon quad(double... xy) {
        Polygon p = new Polygon();
        for (int i = 0; i < xy.length; i += 2) {
            p.addPoint((int) Math.round(xy[i]), (int) Math.round(xy[i + 1]));
        }
        return p;
    }

    private static Color shade(Color c, double f) {
        return new Color(Math.min(255, (int) (c.getRed() * f)), Math.min(255, (int) (c.getGreen() * f)),
                Math.min(255, (int) (c.getBlue() * f)));
    }

    private GenerateLogo() {}
}
