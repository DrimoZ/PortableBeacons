import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * CurseForge and wiki art in Minecraft's font, on the GUI preview's captures - FactoryIO's
 * {@code docs/showcase/Banners.java}, so the mods' pages share one look.
 *
 * <pre>
 *   BEACON_GUI_PREVIEW=1 ./gradlew runClient    (1920x1080, GUI scale 4)
 *   java tools/Banners.java run/screenshots/preview run/store-art
 * </pre>
 *
 * The font comes from the game jar ModDevGradle already built: it is not committed. CurseForge's
 * description editor refuses any image wider than 850 px.
 */
public class Banners {

    static final int W = 850;
    static final Color INK = new Color(12, 13, 20);
    static final Color ACCENT = new Color(85, 208, 224);
    static final Color SUB = new Color(168, 244, 250);
    static final String ITEMS = "src/main/resources/assets/portablebeacons/textures/item/";

    static BufferedImage font;
    static final int[] widths = new int[256];

    public static void main(String[] args) throws Exception {
        File shots = new File(args[0]), out = new File(args[1]);
        out.mkdirs();
        loadFont();

        banner(shots, out);
        header(shots, out, "header_beacons", "beacon_main", 0.45, "The beacons", "Four tiers, three themed, one for creative");
        header(shots, out, "header_screen", "beacon_main", 0.3, "One screen", "Every setting in its row, nothing hidden");
        header(shots, out, "header_augments", "beacon_info", 0.5, "Augments", "Fourteen of them, four slots at most");
        header(shots, out, "header_fuel", "beacon_fuel_tooltip", 0.4, "Fuel", "Ingots, a real beacon, or Forge Energy");
        header(shots, out, "header_data", "beacon_selector", 0.5, "Data-driven", "Effects, tiers, augments and fuels from JSON");

        itemSheet(out, "items_beacons", "Beacons", 4,
                "beacon_i", "beacon_ii", "beacon_iii", "beacon_iv",
                "cinder_beacon", "void_beacon", "tidal_beacon", "creative_beacon");
        itemSheet(out, "items_augments", "Augments", 5,
                "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion",
                "communion", "wellspring", "wayfarer", "sentinel", "vanguard", "prism", "recluse");

        // The two screens worth showing, cropped to the window and its tabs. Captured at GUI scale 4,
        // so 3/4 maps every 4-pixel block onto exactly 3: no pixel blurs.
        gui(shots, out, "screen", "beacon_main", 840, 268, 1972, 1104);
        gui(shots, out, "picker", "beacon_selector", 928, 272, 1632, 1100);
    }

    /** The top banner: the title large, the Beacon IV beside it, on the main screen. */
    static void banner(File shots, File out) throws Exception {
        int h = 280;
        BufferedImage b = blur(cover(ImageIO.read(new File(shots, "beacon_main.png")), W, h, 0.4));
        Graphics2D g = b.createGraphics();
        g.setColor(alpha(INK, 170));
        g.fillRect(0, 0, W, h);
        g.setPaint(new GradientPaint(0, 0, alpha(INK, 235), W * 0.8f, 0, alpha(INK, 60)));
        g.fillRect(0, 0, W, h);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(ImageIO.read(new File(ITEMS + "beacon_iv.png")), W - 34 - 192, 44, 192, 192, null);
        int x = 34;
        text(g, "Portable Beacons", x, 52, 6, Color.WHITE);
        text(g, "A beacon you carry.", x + 2, 128, 3, new Color(236, 236, 240));
        text(g, "Your effects, your audience, your fuel.", x + 2, 172, 2, SUB);
        text(g, "NeoForge 1.21.1 - 26.1 - 26.2", x + 2, 226, 2, alpha(new Color(220, 220, 220), 220));
        g.dispose();
        ImageIO.write(b, "png", new File(out, "banner.png"));
    }

    /** A section header: a blurred slice of a capture behind a title. */
    static void header(File shots, File out, String name, String shot, double fy, String title, String subtitle) throws Exception {
        int h = 100;
        BufferedImage img = blur(cover(ImageIO.read(new File(shots, shot + ".png")), W, h, fy));
        Graphics2D g = img.createGraphics();
        g.setColor(alpha(INK, 150));
        g.fillRect(0, 0, W, h);
        g.setPaint(new GradientPaint(0, 0, alpha(INK, 210), W * 0.75f, 0, alpha(INK, 30)));
        g.fillRect(0, 0, W, h);
        g.setColor(ACCENT);
        g.fillRect(0, 0, 6, h);
        text(g, title, 28, 20, 4, Color.WHITE);
        text(g, subtitle, 30, 64, 2, SUB);
        g.dispose();
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    /**
     * The repository's item textures, enlarged without smoothing, with their English name. An
     * augment is composed as the game composes it: casing, screen tinted by its data file's colour,
     * glyph - at its highest tier.
     */
    static void itemSheet(File out, String name, String title, int cols, String... ids) throws Exception {
        String lang = Files.readString(Path.of("src/main/resources/assets/portablebeacons/lang/en_us.json"));
        int cell = W / cols, icon = 80, rowH = icon + 44, top = 64;
        int rows = (ids.length + cols - 1) / cols;
        BufferedImage img = new BufferedImage(W, top + rows * rowH + 12, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(34, 36, 52), 0, img.getHeight(), new Color(18, 19, 30)));
        g.fillRect(0, 0, W, img.getHeight());
        g.setColor(ACCENT);
        g.fillRect(0, 0, 6, img.getHeight());
        text(g, title, 28, 18, 3, Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int i = 0; i < ids.length; i++) {
            int cx = (i % cols) * cell, cy = top + (i / cols) * rowH;
            boolean augment = new File("src/main/resources/data/portablebeacons/portablebeacons/augment/" + ids[i] + ".json").exists();
            BufferedImage tex = augment ? augment(ids[i], maxTier(ids[i])) : ImageIO.read(new File(ITEMS + ids[i] + ".png"));
            g.setColor(new Color(255, 255, 255, 14));
            g.fillRect(cx + (cell - icon) / 2 - 8, cy - 4, icon + 16, icon + 8);
            g.drawImage(tex, cx + (cell - icon) / 2, cy, icon, icon, null);
            String label = label(lang, ids[i], augment);
            int scale = width(label) * 2 > cell - 12 ? 1 : 2;
            text(g, label, cx + (cell - width(label) * scale) / 2, cy + icon + 12, scale, SUB);
        }
        g.dispose();
        ImageIO.write(img, "png", new File(out, name + ".png"));
    }

    /** An augment shows its highest tier: three lights on Focus would claim tiers it does not have. */
    static int maxTier(String id) throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/portablebeacons/portablebeacons/augment/" + id + ".json"));
        var m = java.util.regex.Pattern.compile("\"max_tier\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 3;
    }

    static BufferedImage augment(String id, int tier) throws Exception {
        String json = Files.readString(Path.of("src/main/resources/data/portablebeacons/portablebeacons/augment/" + id + ".json"));
        var m = java.util.regex.Pattern.compile("\"color\"\\s*:\\s*(\\d+)").matcher(json);
        int rgb = m.find() ? Integer.parseInt(m.group(1)) : 0xFFFFFF;
        BufferedImage screen = ImageIO.read(new File(ITEMS + "augment_screen.png"));
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) {
                int p = screen.getRGB(x, y);
                int r = ((p >> 16) & 255) * ((rgb >> 16) & 255) / 255;
                int gr = ((p >> 8) & 255) * ((rgb >> 8) & 255) / 255;
                int b = (p & 255) * (rgb & 255) / 255;
                screen.setRGB(x, y, (p & 0xFF000000) | (r << 16) | (gr << 8) | b);
            }
        BufferedImage o = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.drawImage(ImageIO.read(new File(ITEMS + "augment_casing_" + tier + ".png")), 0, 0, null);
        g.drawImage(screen, 0, 0, null);
        g.drawImage(ImageIO.read(new File(ITEMS + "augment_glyph_" + id + ".png")), 0, 0, null);
        g.dispose();
        return o;
    }

    static String label(String lang, String id, boolean augment) {
        String key = augment ? "augment\\.portablebeacons\\." + id : "item\\.portablebeacons\\." + id;
        var m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(lang);
        if (!m.find()) return id;
        // "Range Augment %s" -> "Range"; "Portable Beacon IV" -> "Beacon IV".
        return m.group(1).replace(" Augment %s", "").replace("Portable ", "");
    }

    /** One capture, cut to the window and its tabs, at 3/4: every 4-pixel block becomes 3. */
    static void gui(File shots, File out, String name, String shot, int x0, int y0, int x1, int y1) throws Exception {
        BufferedImage cut = ImageIO.read(new File(shots, shot + ".png")).getSubimage(x0, y0, x1 - x0, y1 - y0);
        int w = cut.getWidth() * 3 / 4, h = cut.getHeight() * 3 / 4;
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) o.setRGB(x, y, cut.getRGB(x * 4 / 3, y * 4 / 3));
        ImageIO.write(o, "png", new File(out, name + ".png"));
    }

    static int width(String s) {
        int w = 0;
        for (char ch : s.toCharArray()) w += widths[ch] + 1;
        return w;
    }

    // Minecraft's font: 8-pixel cells, one pixel between letters, a shadow at a quarter of the colour.

    static void loadFont() throws Exception {
        File jar = null;
        File[] jars = new File("build/moddev/artifacts").listFiles((d, n) -> n.startsWith("minecraft-patched-") && n.endsWith(".jar") && !n.contains("sources"));
        if (jars != null) for (File f : jars) jar = f;
        if (jar == null) throw new IllegalStateException("no game jar under build/moddev/artifacts: build once first");
        try (ZipFile zip = new ZipFile(jar)) {
            font = ImageIO.read(zip.getInputStream(zip.getEntry("assets/minecraft/textures/font/ascii.png")));
        }
        for (int c = 0; c < 256; c++) {
            int gx = (c % 16) * 8, gy = (c / 16) * 8, w = 0;
            for (int x = 7; x >= 0 && w == 0; x--)
                for (int y = 0; y < 8; y++) if ((font.getRGB(gx + x, gy + y) >>> 24) > 0) { w = x + 1; break; }
            widths[c] = c == ' ' ? 4 : w;
        }
    }

    static void text(Graphics2D g, String s, int x, int y, int scale, Color c) {
        glyphs(g, s, x + scale, y + scale, scale, new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, c.getAlpha()));
        glyphs(g, s, x, y, scale, c);
    }

    static void glyphs(Graphics2D g, String s, int x, int y, int scale, Color c) {
        g.setColor(c);
        for (char ch : s.toCharArray()) {
            int gx = (ch % 16) * 8, gy = (ch / 16) * 8;
            for (int yy = 0; yy < 8; yy++)
                for (int xx = 0; xx < 8; xx++)
                    if ((font.getRGB(gx + xx, gy + yy) >>> 24) > 0) g.fillRect(x + xx * scale, y + yy * scale, scale, scale);
            x += (widths[ch] + 1) * scale;
        }
    }

    // Images

    static BufferedImage cover(BufferedImage src, int w, int h, double fy) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double s = Math.max(w / (double) src.getWidth(), h / (double) src.getHeight());
        int sw = (int) (src.getWidth() * s), sh = (int) (src.getHeight() * s);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, (w - sw) / 2, (int) (-(sh - h) * fy), sw, sh, null);
        g.dispose();
        return o;
    }

    static BufferedImage blur(BufferedImage src) {
        int r = 5, n = (2 * r + 1) * (2 * r + 1);
        float[] k = new float[n];
        java.util.Arrays.fill(k, 1f / n);
        BufferedImage padded = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        new ConvolveOp(new Kernel(2 * r + 1, 2 * r + 1, k), ConvolveOp.EDGE_NO_OP, null).filter(src, padded);
        return padded;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
