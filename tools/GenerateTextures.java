import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the item textures and the mod list logo.
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
    /** The mod list logo lives at the jar root, not under assets/. */
    private static final String ROOT_DIR = "src/main/resources";

    public static void main(String[] args) throws IOException {
        new File(ITEM_DIR).mkdirs();
        writeAugments();
        writeBeacons();
        writeLogo();
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
    }

    // ------------------------------------------------------------------ beacons

    private static final String[] BEACON = {
            "......kkkk......",
            ".....kWWWWk.....",
            "....kGGccGGk....",
            "...kGGcCCcGGk...",
            "...kGcCWWCcGk...",
            "...kGGcCCcGGk...",
            "...kGGGccGGGk...",
            "..kkkkkkkkkkkk..",
            "..kMMMMMMMMMMk..",
            "..kmmmmmmmmmmk..",
            ".kOOOOOOOOOOOOk.",
            ".kOooooooooooOk.",
            ".kOoo1o2o3o4oOk.",
            ".kOooooooooooOk.",
            "..kkkkkkkkkkkk..",
            "................"};

    /**
     * @param band   the tier's material, light then dark
     * @param core   the beam colour, then its bright centre
     * @param pips   how many tier pips to light; 0 for a themed beacon, which takes a strip instead
     * @param accent the themed beacon's strip colour, or 0
     */
    private static BufferedImage beacon(int[] band, int[] core, int pips, int accent) {
        Map<Character, Integer> palette = new LinkedHashMap<>();
        palette.put('k', 0x15161C);
        palette.put('W', 0xFFFFFF);
        palette.put('G', 0xB6DCE6);
        palette.put('c', core[0]);
        palette.put('C', core[1]);
        palette.put('M', band[0]);
        palette.put('m', band[1]);
        palette.put('O', 0x3A2E52);
        palette.put('o', 0x231B33);
        for (int pip = 1; pip <= 4; pip++) {
            palette.put((char) ('0' + pip), pips == 0 ? accent : pip <= pips ? 0xF2F2F2 : 0x231B33);
        }
        BufferedImage image = ascii(BEACON, palette);
        if (pips == 0) {
            // A continuous strip rather than four dots: themed beacons sit beside the ladder, not on it.
            for (int x = 5; x <= 11; x++) {
                set(image, x, 12, accent);
            }
        }
        return image;
    }

    private static void writeBeacons() throws IOException {
        int[] beam = {0x55D0E0, 0xA8F4FA};
        write("beacon_i", beacon(new int[]{0xD8D8D8, 0x9A9A9A}, beam, 1, 0));
        write("beacon_ii", beacon(new int[]{0xF7D44A, 0xC08A1E}, beam, 2, 0));
        write("beacon_iii", beacon(new int[]{0x7FE8E0, 0x2FA8A0}, beam, 3, 0));
        write("beacon_iv", beacon(new int[]{0x6A5A64, 0x3E3238}, beam, 4, 0));

        write("cinder_beacon", beacon(new int[]{0xE0603A, 0x8E2A18}, new int[]{0xF29B1D, 0xFFE08A}, 0, 0xF29B1D));
        write("void_beacon", beacon(new int[]{0xC48CE0, 0x7A4A9A}, new int[]{0xB07CD8, 0xEAD2FA}, 0, 0xC48CE0));
        write("tidal_beacon", beacon(new int[]{0x5AB8A8, 0x2E7A70}, new int[]{0x3FB6D8, 0xB4ECF8}, 0, 0x5AB8A8));
    }

    /**
     * The mod list logo: the tier IV icon, scaled up whole pixels onto a dark plate.
     *
     * <p>Nearest-neighbour by construction rather than by a scaling hint, and paired with
     * {@code logoBlur = false} in the mods.toml, because the interpolated version of a 16px icon
     * is mush. The plate exists because the icon is drawn for a grey slot; the mod list background
     * is dark and the outline would disappear into it.
     */
    private static void writeLogo() throws IOException {
        int scale = 7;
        int size = 128;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        fill(image, 0, 0, size, size, 0xFF23252E);
        fill(image, 0, 0, size, 2, 0xFF34384A);
        fill(image, 0, size - 2, size, 2, 0xFF15161C);

        BufferedImage icon = beacon(new int[]{0x6A5A64, 0x3E3238}, new int[]{0x55D0E0, 0xA8F4FA}, 4, 0);
        int origin = (size - icon.getWidth() * scale) / 2;
        for (int x = 0; x < icon.getWidth(); x++) {
            for (int y = 0; y < icon.getHeight(); y++) {
                int argb = icon.getRGB(x, y);
                if ((argb >>> 24) != 0) {
                    fill(image, origin + x * scale, origin + y * scale, scale, scale, argb);
                }
            }
        }
        ImageIO.write(image, "PNG", new File(ROOT_DIR + "/logo.png"));
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
