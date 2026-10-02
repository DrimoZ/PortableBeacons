import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the item textures. The logo is {@code GenerateLogo.java}'s, built from these.
 *
 * <p>The augments are drawn in FactoryIO's module family on purpose - a dark casing, a coloured
 * screen carrying a white glyph, and tier pips along the foot - so an augment and a module read as
 * the same kind of thing: a part you fit into something. Three layers rather than one:
 * <ol>
 *   <li>the casing, with the tier's pips, untinted - three variants, one per tier;</li>
 *   <li>the screen, greyscale, tinted from the registry entry - which is what lets a datapack
 *       augment have its own colour without a texture;</li>
 *   <li>the glyph, white, untinted - shape carries the meaning, so two augments of similar hue are
 *       still told apart.</li>
 * </ol>
 * The old augments were one tinted tag, so the tier could not be seen at all: Range I and Range III
 * were the same icon.
 *
 * <p>The beacons share one silhouette - glass dome over a beam core, a metal band, an obsidian
 * foot - and the band is the tier's material, so the ladder reads iron, gold, diamond, netherite.
 * Pips repeat it for anyone who cannot tell the colours apart.
 *
 * <pre>java tools/GenerateTextures.java</pre>
 */
public final class GenerateTextures {

    private static final String ITEM_DIR = "src/main/resources/assets/portablebeacons/textures/item";

    public static void main(String[] args) throws IOException {
        new File(ITEM_DIR).mkdirs();
        writeAugments();
        writeBeacons();
        System.out.println("Textures written.");
    }

    // ------------------------------------------------------------------ augments

    private static final String[] CASING = {
            "................",
            "..kkkkkkkkkkkk..",
            ".kLLLLLLLLLLLLk.",
            ".kLKKKKKKKKKKDk.",
            ".kLK........KDk.",
            ".kLK........KDk.",
            ".kLK........KDk.",
            ".kLK........KDk.",
            ".kLK........KDk.",
            ".kLK........KDk.",
            ".kLKKKKKKKKKKDk.",
            ".kDDDDDDDDDDDDk.",
            ".kDD11D22D33DDk.",
            ".kSSSSSSSSSSSSk.",
            "..kkkkkkkkkkkk..",
            "................"};

    /** The screen's interior: x 4..11, y 4..9. */
    private static final int SCREEN_X = 4;
    private static final int SCREEN_Y = 4;

    private static void writeAugments() throws IOException {
        for (int tier = 1; tier <= 3; tier++) {
            Map<Character, Integer> palette = new LinkedHashMap<>();
            palette.put('k', 0x1B1B1B);
            palette.put('L', 0x6A6A6A);
            palette.put('D', 0x3E3E3E);
            palette.put('S', 0x2A2A2A);
            palette.put('K', 0x101010);
            for (int pip = 1; pip <= 3; pip++) {
                // Lit pips in FactoryIO's yellow; unlit ones stay as dark sockets, so how many tiers
                // the augment could have is visible too.
                palette.put((char) ('0' + pip), pip <= tier ? 0xF7D44A : 0x262626);
            }
            write("augment_casing_" + tier, ascii(CASING, palette));
        }

        // Greyscale, lit from the top: multiplied by the registry colour it gives a mid-tone screen
        // that white reads on. A full-white screen made pale colours - yellow, cyan - swallow the
        // glyph entirely.
        BufferedImage screen = blank();
        int[] rows = {0xB4B4B4, 0xA0A0A0, 0x969696, 0x8C8C8C, 0x828282, 0x787878};
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < 8; x++) {
                set(screen, SCREEN_X + x, SCREEN_Y + y, rows[y]);
            }
        }
        write("augment_screen", screen);

        for (Map.Entry<String, String[]> glyph : GLYPHS.entrySet()) {
            BufferedImage image = blank();
            String[] art = glyph.getValue();
            for (int y = 0; y < art.length; y++) {
                for (int x = 0; x < art[y].length(); x++) {
                    if (art[y].charAt(x) == 'w') {
                        set(image, SCREEN_X + x, SCREEN_Y + y, 0xFFFFFF);
                    }
                }
            }
            write("augment_glyph_" + glyph.getKey(), image);
        }
    }

    /** 8x6 white glyphs, one per built-in augment. */
    private static final Map<String, String[]> GLYPHS = new LinkedHashMap<>();

    static {
        // Double chevron: reaches further.
        GLYPHS.put("range", new String[]{
                "ww..ww..", ".ww..ww.", "..ww..ww", ".ww..ww.", "ww..ww..", "........"});
        // Plus: one more effect slot.
        GLYPHS.put("focus", new String[]{
                "...ww...", "...ww...", "wwwwwwww", "wwwwwwww", "...ww...", "...ww..."});
        // Arrow up: stronger.
        GLYPHS.put("amplification", new String[]{
                "...ww...", "..wwww..", ".wwwwww.", "...ww...", "...ww...", "...ww..."});
        // Arrow down: draws less.
        GLYPHS.put("efficiency", new String[]{
                "...ww...", "...ww...", "...ww...", ".wwwwww.", "..wwww..", "...ww..."});
        // Battery: a bigger buffer.
        GLYPHS.put("capacity", new String[]{
                "........", "wwwwwww.", "w.w.w.ww", "w.w.w.ww", "wwwwwww.", "........"});
        // Linked rings: who the aura reaches.
        GLYPHS.put("attunement", new String[]{
                ".ww..ww.", "w..ww..w", "w..ww..w", "w..ww..w", ".ww..ww.", "........"});
        // A closed eye: the effects stop announcing themselves.
        GLYPHS.put("discretion", new String[]{
                "........", "w......w", ".w....w.", "..wwww..", ".w.ww.w.", "........"});
        // Three people: sharing, made cheap.
        GLYPHS.put("communion", new String[]{
                "...ww...", "...ww...", "........", "ww....ww", "ww.ww.ww", "...ww..."});
        // A drop: one effect from nowhere.
        GLYPHS.put("wellspring", new String[]{
                "...ww...", "..wwww..", ".wwwwww.", ".wwwwww.", "..wwww..", "........"});
        // Speed lines: cheap while travelling.
        GLYPHS.put("wayfarer", new String[]{
                "....w...", "ww...w..", "......w.", "ww...w..", "....w...", "........"});
        // A tower: cheap while holding a position.
        GLYPHS.put("sentinel", new String[]{
                "w.w..w.w", "wwwwwwww", ".wwwwww.", ".ww..ww.", ".wwwwww.", ".wwwwww."});
        // A banner: reach and audience, carried forward.
        GLYPHS.put("vanguard", new String[]{
                "wwwww...", "wwwwwww.", "wwwww...", "w.......", "w.......", "w......."});
        // A prism.
        GLYPHS.put("prism", new String[]{
                "...ww...", "..w..w..", "..w..w..", ".w....w.", ".w....w.", "wwwwwwww"});
        // A padlock: everything kept in.
        GLYPHS.put("recluse", new String[]{
                "..wwww..", ".w....w.", ".w....w.", "wwwwwwww", "www..www", "wwwwwwww"});

        // Generic shapes, owned by no augment: for datapack augments to borrow with "glyph".
        GLYPHS.put("star", new String[]{
                "...ww...", "...ww...", "wwwwwwww", ".wwwwww.", ".ww..ww.", "ww....ww"});
        GLYPHS.put("bolt", new String[]{
                "....www.", "...www..", "..wwwww.", ".wwwww..", "...ww...", "..ww...."});
        GLYPHS.put("heart", new String[]{
                ".ww..ww.", "wwwwwwww", "wwwwwwww", ".wwwwww.", "..wwww..", "...ww..."});
        GLYPHS.put("gem", new String[]{
                "..wwww..", ".ww..ww.", "wwwwwwww", ".ww..ww.", "..w..w..", "...ww..."});
        GLYPHS.put("shield", new String[]{
                "wwwwwwww", "w..ww..w", "w..ww..w", ".w.ww.w.", "..wwww..", "...ww..."});
        GLYPHS.put("leaf", new String[]{
                ".....www", "...wwww.", "..www.w.", ".ww.ww..", ".www....", "w......."});
    }

    // ------------------------------------------------------------------ beacons

    /*
     * The beacons are 3D item models - portable_beacon.json: an obsidian foot, the tier's band, a
     * glass dome with the beam's core inside - drawn by the game the way it draws a block, so the
     * inventory icon is the logo's object rather than a flat picture of it. These are the faces.
     *
     * One sheet per beacon, read by UV region:
     *   core       (0,0)  6x6
     *   band side  (0,6)  12x2
     *   foot side  (0,8)  14x4   - the tier's pips, or a themed beacon's strip
     *   band top   (12,0) 1x1    - seen through the glass, so dark: a bright one drowned the core
     *   foot top   (13,0) 1x1
     * The glass is shared: glass_side 10x9 and glass_top 10x10, each from the sheet's corner.
     */

    private static final int OBSIDIAN_TOP = 0x3A2E52;
    private static final int OBSIDIAN = 0x2A2041;
    private static final int OBSIDIAN_DARK = 0x1E1730;
    private static final int PIP_OFF = 0x1C1529;

    /**
     * @param band   the tier's material, light then dark
     * @param core   the beam colour, then its bright centre
     * @param pips   how many tier pips to light; 0 for a themed beacon, which takes a strip instead
     * @param accent the themed beacon's strip colour, or 0
     */
    private static BufferedImage beacon(int[] band, int[] core, int pips, int accent) {
        BufferedImage image = blank();
        fill(image, 0, 0, 6, 6, 0xFF000000 | core[0]);
        fill(image, 1, 1, 4, 4, 0xFF000000 | mix(core[0], core[1]));
        fill(image, 2, 2, 2, 2, 0xFF000000 | core[1]);
        fill(image, 0, 6, 12, 1, 0xFF000000 | band[0]);
        fill(image, 0, 7, 12, 1, 0xFF000000 | band[1]);
        fill(image, 0, 8, 14, 1, 0xFF000000 | OBSIDIAN_TOP);
        fill(image, 0, 9, 14, 2, 0xFF000000 | OBSIDIAN);
        fill(image, 0, 11, 14, 1, 0xFF000000 | OBSIDIAN_DARK);
        if (pips > 0) {
            int[] at = {3, 5, 8, 10};
            for (int pip = 0; pip < 4; pip++) {
                fill(image, at[pip], 10, 1, 1, 0xFF000000 | (pip < pips ? 0xF2F2F2 : PIP_OFF));
            }
        } else {
            // A continuous strip rather than four dots: themed beacons sit beside the ladder, not on it.
            fill(image, 2, 10, 10, 1, 0xFF000000 | accent);
        }
        set(image, 12, 0, OBSIDIAN);
        set(image, 13, 0, OBSIDIAN_TOP);
        return image;
    }

    private static int mix(int a, int b) {
        int r = (((a >> 16) & 255) + ((b >> 16) & 255)) / 2;
        int g = (((a >> 8) & 255) + ((b >> 8) & 255)) / 2;
        return (r << 16) | (g << 8) | (((a & 255) + (b & 255)) / 2);
    }

    /** Glass as the game draws it: a light frame round a pane you can see through. */
    private static BufferedImage glass(int width, int height, boolean top) {
        BufferedImage image = blank();
        fill(image, 0, 0, width, height, top ? 0x50E8FAFF : 0x38CFEFF5);
        int frame = 0xFFE6FCFF;
        fill(image, 0, 0, width, 1, frame);
        fill(image, 0, 0, 1, height, frame);
        fill(image, width - 1, 0, 1, height, top ? frame : 0xFFAAD8E2);
        if (top) {
            fill(image, 0, height - 1, width, 1, frame);
        } else {
            // The game's glass highlight, two pixels across the pane.
            fill(image, 2, 3, 1, 1, 0xB0FFFFFF);
            fill(image, 3, 2, 1, 1, 0xB0FFFFFF);
        }
        return image;
    }

    private static void writeBeacons() throws IOException {
        new File(ITEM_DIR + "/beacon").mkdirs();
        int[] beam = {0x55D0E0, 0xC8FAFF};
        write("beacon/beacon_i", beacon(new int[]{0xD8D8D8, 0x9A9A9A}, beam, 1, 0));
        write("beacon/beacon_ii", beacon(new int[]{0xF7D44A, 0xC08A1E}, beam, 2, 0));
        write("beacon/beacon_iii", beacon(new int[]{0x7FE8E0, 0x2FA8A0}, beam, 3, 0));
        write("beacon/beacon_iv", beacon(new int[]{0x7A6A74, 0x4E4249}, beam, 4, 0));

        write("beacon/cinder_beacon", beacon(new int[]{0xE0603A, 0x8E2A18}, new int[]{0xF29B1D, 0xFFE08A}, 0, 0xF29B1D));
        write("beacon/void_beacon", beacon(new int[]{0xC48CE0, 0x7A4A9A}, new int[]{0xB07CD8, 0xEAD2FA}, 0, 0xC48CE0));
        write("beacon/tidal_beacon", beacon(new int[]{0x5AB8A8, 0x2E7A70}, new int[]{0x3FB6D8, 0xB4ECF8}, 0, 0x5AB8A8));
        write("beacon/creative_beacon", beacon(new int[]{0xE070D0, 0x9A3A90}, new int[]{0xF6B8F0, 0xFFEFFC}, 0, 0xFFD54A));

        write("beacon/glass_side", glass(10, 9, false));
        write("beacon/glass_top", glass(10, 10, true));
    }

    // ------------------------------------------------------------------ plumbing

    private static BufferedImage ascii(String[] rows, Map<Character, Integer> palette) {
        BufferedImage image = blank();
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != 16) {
                throw new IllegalArgumentException("row " + y + " is " + rows[y].length() + " wide");
            }
            for (int x = 0; x < 16; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') {
                    continue;
                }
                Integer colour = palette.get(ch);
                if (colour == null) {
                    throw new IllegalArgumentException("unknown colour '" + ch + "' in row " + y);
                }
                set(image, x, y, colour);
            }
        }
        return image;
    }

    private static BufferedImage blank() {
        return new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
    }

    private static void fill(BufferedImage image, int x, int y, int w, int h, int argb) {
        for (int px = x; px < x + w; px++) {
            for (int py = y; py < y + h; py++) {
                if (px >= 0 && py >= 0 && px < image.getWidth() && py < image.getHeight()) {
                    image.setRGB(px, py, argb);
                }
            }
        }
    }

    private static void write(String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "PNG", new File(ITEM_DIR + "/" + name + ".png"));
    }

    private GenerateTextures() {}
}
