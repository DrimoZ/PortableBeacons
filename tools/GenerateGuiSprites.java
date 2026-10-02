import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates the screen's GUI sprites.
 *
 * <p>The pieces are FactoryIO's - same bevels, same palette, same slot and LED drawings, ported
 * from its {@code tools/gui-sprites.js} - so the two mods' screens read as one family. What differs
 * is the container: 26.1 has GUI sprites, so each piece is its own file with its own mcmeta, and
 * the game does the nine-slicing. FactoryIO's sheet needed every u/v copied into a Java class, and
 * a sheet nobody can edit without moving a coordinate is exactly what a sprite atlas removes.
 *
 * <p>Kept as code rather than painted files for FactoryIO's reason: a nine-slice frame is only
 * correct if its borders are exactly regular, and a 12x12 icon is twelve lines of text.
 *
 * <pre>java tools/GenerateGuiSprites.java</pre>
 */
public final class GenerateGuiSprites {

    private static final String OUT = "src/main/resources/assets/portablebeacons/textures/gui/sprites";

    private static final Map<Character, Integer> PALETTE = new HashMap<>();

    static {
        PALETTE.put('k', 0x000000);
        PALETTE.put('K', 0x373737);
        PALETTE.put('d', 0x555555);
        PALETTE.put('m', 0x8b8b8b);
        PALETTE.put('g', 0xc6c6c6);
        PALETTE.put('w', 0xffffff);
        PALETTE.put('r', 0xd83a2e);
        PALETTE.put('R', 0x8e1f18);
        PALETTE.put('o', 0xf29b1d);
        PALETTE.put('y', 0xf7d44a);
        PALETTE.put('G', 0x3fbf3f);
        PALETTE.put('E', 0x1e7a1e);
        PALETTE.put('b', 0x3b78d8);
        PALETTE.put('B', 0x1f4a91);
    }

    public static void main(String[] args) throws IOException {
        new File(OUT).mkdirs();

        // Frames, nine-sliced.
        nineSlice("panel", bevel(16, 16, 0x000000, 0xffffff, 0x555555, 0xc6c6c6, 2), 4);
        // Grey on purpose: it is tinted at draw time, one colour per function.
        nineSlice("tab", bevel(16, 16, 0x1a1a1a, 0xffffff, 0x9a9a9a, 0xd6d6d6, 1), 4);
        nineSlice("button", bevel(16, 16, 0x000000, 0xe8e8e8, 0x6b6b6b, 0xa8a8a8, 1), 3);
        nineSlice("button_hover", bevel(16, 16, 0x000000, 0xc9dcff, 0x4a5f8f, 0x7f9ad1, 1), 3);
        nineSlice("button_pressed", bevel(16, 16, 0x000000, 0x4a4a4a, 0xb8b8b8, 0x6f6f6f, 1), 3);
        nineSlice("button_disabled", bevel(16, 16, 0x2b2b2b, 0x9a9a9a, 0x707070, 0x868686, 1), 3);
        nineSlice("inset", inset(16, 16, 0x373737, 0xffffff, 0x8b8b8b), 1);
        nineSlice("field", inset(16, 16, 0x1a1a1a, 0xffffff, 0x2b2b2b), 1);

        // Slots and sockets.
        write("slot", inset(18, 18, 0x373737, 0xffffff, 0x8b8b8b));
        write("socket", inset(26, 26, 0x373737, 0xffffff, 0x8b8b8b));
        write("socket_selected", selected(inset(26, 26, 0x1e7a1e, 0x6fd46f, 0x8b8b8b)));
        write("slot_selected", selected(inset(18, 18, 0x1e7a1e, 0x6fd46f, 0x8b8b8b)));
        write("slot_disabled", hatched(inset(18, 18, 0x262626, 0x9a9a9a, 0x6a6a6a)));

        // The fuel gauge: lit and unlit segments, tiled. Amber rather than FactoryIO's energy red -
        // this column holds something that burns, and red is kept for "will not work without you".
        tile("gauge_fuel", gauge(0xf2a23a, 0xb8661a), 12, 4);
        tile("gauge_empty", gauge(0x4e4e4e, 0x3a3a3a), 12, 4);

        // Status LEDs, FactoryIO's five: working, waiting, blocked, problem, off.
        String[] led = {".kkkkk.", "kcccccK", "kcwcccK", "kccccCK", "kccccCK", "kcCCCCK", ".KKKKK."};
        int[][] leds = {{0x3fbf3f, 0x1e7a1e}, {0xf7d44a, 0xb08a10}, {0xf29b1d, 0xa0600a},
                {0xd83a2e, 0x8e1f18}, {0x8b8b8b, 0x555555}};
        String[] ledNames = {"led_working", "led_waiting", "led_blocked", "led_problem", "led_off"};
        for (int i = 0; i < leds.length; i++) {
            write(ledNames[i], ascii(led, with('c', leds[i][0], 'C', leds[i][1])));
        }

        write("lock_small", ascii(new String[]{
                "..kkk..",
                ".k...k.",
                ".k...k.",
                "kkkkkkk",
                "kyyyyyk",
                "kyykyyk",
                "kyyyyyk",
                "kkkkkkk"}, PALETTE));

        // Icons, 12x12.
        // Slide switches, one shape at two sizes: the band's governs the whole beacon, a row's one
        // effect. A switch rather than the power glyph, which read as neither a button nor a state.
        // Lit green with the knob right when on; grey with the knob left when off.
        write("switch_on", ascii(new String[]{
                "..kkkkkkkkkk..",
                ".kGGGGGGkwwwk.",
                "kGGGGGGGkwwwwk",
                "kGGGGGGGkwwwwk",
                "kGGGGGGGkwwwwk",
                "kGGGGGGGkwwwwk",
                ".kGGGGGGkwwwk.",
                "..kkkkkkkkkk.."}, PALETTE));
        write("switch_off", ascii(new String[]{
                "..kkkkkkkkkk..",
                ".kwwwkmmmmmmk.",
                "kwwwwkmmmmmmmk",
                "kwwwwkmmmmmmmk",
                "kwwwwkmmmmmmmk",
                "kwwwwkmmmmmmmk",
                ".kwwwkmmmmmmk.",
                "..kkkkkkkkkk.."}, PALETTE));
        write("switch_big_on", ascii(new String[]{
                "...kkkkkkkkkkkkkk...",
                ".kkGGGGGGGGGkkkkkkk.",
                ".kGGGGGGGGGkwwwwwwk.",
                "kGGGGGGGGGGkwwwwwwwk",
                "kGGGGGGGGGGkwwwwwwwk",
                "kGGGGGGGGGGkwwwwwwwk",
                "kGGGGGGGGGGkwwwwwwwk",
                ".kGGGGGGGGGkwwwwwwk.",
                ".kkGGGGGGGGGkkkkkkk.",
                "...kkkkkkkkkkkkkk..."}, PALETTE));
        write("switch_big_off", ascii(new String[]{
                "...kkkkkkkkkkkkkk...",
                ".kkkkkkkmmmmmmmmmkk.",
                ".kwwwwwwkmmmmmmmmmk.",
                "kwwwwwwwkmmmmmmmmmmk",
                "kwwwwwwwkmmmmmmmmmmk",
                "kwwwwwwwkmmmmmmmmmmk",
                "kwwwwwwwkmmmmmmmmmmk",
                ".kwwwwwwkmmmmmmmmmk.",
                ".kkkkkkkmmmmmmmmmkk.",
                "...kkkkkkkkkkkkkk..."}, PALETTE));
        // The effect table: a sunken outline around the window's own colour, so its rows read as one
        // list and not as five loose slots.
        nineSlice("table", inset(16, 16, 0x373737, 0xffffff, 0xc6c6c6), 1);
        write("icon_info", ascii(new String[]{
                "...bbbbbb...",
                "..bbbbbbbb..",
                ".bbbbwwbbbb.",
                "bbbbbwwbbbbb",
                "bbbbbbbbbbbb",
                "bbbbwwwbbbbb",
                "bbbbbwwbbbbb",
                "bbbbbwwbbbbb",
                ".bbbbwwbbbb.",
                "..bbwwwwbb..",
                "...bbbbbb...",
                "............"}, PALETTE));
        // An augment's cut gem, in the augment tab's own blue family.
        write("icon_augments", ascii(new String[]{
                "............",
                "...BBBBBB...",
                "..BbbwbbbB..",
                ".BbbwbbbbbB.",
                "BBBBBBBBBBBB",
                ".BbbbbbbbbB.",
                "..BbbbbbbB..",
                "...BbbbbB...",
                "....BbbB....",
                ".....BB.....",
                "............",
                "............"}, PALETTE));

        // Control icons, for the buttons that replaced the text rows. Dark on the button face, the way
        // FactoryIO draws its own; the sharing modes in its blue, the same blue as the socket marker.
        write("icon_plus", ascii(new String[]{
                "............", "............", ".....kk.....", ".....kk.....", ".....kk.....",
                "..kkkkkkkk..", "..kkkkkkkk..",
                ".....kk.....", ".....kk.....", ".....kk.....", "............", "............"}, PALETTE));
        // A cross, for the picker's "remove" cell.
        write("icon_clear", ascii(new String[]{
                "............", "............", "..rr....rr..", "...rr..rr...", "....rrrr....",
                ".....rr.....", "....rrrr....", "...rr..rr...", "..rr....rr..", "............",
                "............", "............"}, PALETTE));
        write("icon_aura_self", ascii(new String[]{
                "............", ".....kk.....", "....kkkk....", "....kkkk....", ".....kk.....",
                "............", "...kkkkkk...", "..kkkkkkkk..", "..kkkkkkkk..", "..kkkkkkkk..",
                "............", "............"}, PALETTE));
        write("icon_aura_team", ascii(new String[]{
                "............", "..bb....bb..", ".bbbb..bbbb.", ".bbbb..bbbb.", "..bb....bb..",
                "............", "bbbbb..bbbbb", "bbbbb..bbbbb", "bbbbb..bbbbb", "............",
                "............", "............"}, PALETTE));
        write("icon_aura_allies", ascii(new String[]{
                "............", ".b...bb...b.", "bbb.bbbb.bbb", ".b..bbbb..b.", ".....bb.....",
                "bbb......bbb", "bbb.bbbb.bbb", "bbb.bbbb.bbb", "....bbbb....", "............",
                "............", "............"}, PALETTE));
        write("icon_aura_pets", ascii(new String[]{
                "............", "...bb..bb...", "...bb..bb...", ".bb......bb.", ".bb......bb.",
                "....bbbb....", "...bbbbbb...", "..bbbbbbbb..", "..bbbbbbbb..", "...bbbbbb...",
                "............", "............"}, PALETTE));

        System.out.println("GUI sprites written to " + OUT);
    }

    // ------------------------------------------------------------------ pieces

    /**
     * A raised frame with two-pixel rounded corners. Where the light and shadow edges cross, the
     * fill wins - which is what gives vanilla's frames their look.
     */
    private static BufferedImage bevel(int w, int h, int outline, int light, int shadow, int fill,
                                       int depth) {
        BufferedImage image = blank(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int left = x, top = y, right = w - 1 - x, bottom = h - 1 - y;
                boolean cut = left + top < 2 || right + top < 2 || left + bottom < 2 || right + bottom < 2;
                if (cut) {
                    continue;
                }
                boolean edge = Math.min(Math.min(left, top), Math.min(right, bottom)) == 0
                        || (left == 1 && top == 1) || (right == 1 && top == 1)
                        || (left == 1 && bottom == 1) || (right == 1 && bottom == 1);
                if (edge) {
                    set(image, x, y, outline);
                    continue;
                }
                boolean nearLight = left <= depth || top <= depth;
                boolean nearShadow = right <= depth || bottom <= depth;
                int colour = fill;
                if (nearLight && !nearShadow) {
                    colour = light;
                } else if (nearShadow && !nearLight) {
                    colour = shadow;
                }
                set(image, x, y, colour);
            }
        }
        return image;
    }

    /** A one-pixel recess with no outline: slots, gauges, text areas. */
    private static BufferedImage inset(int w, int h, int dark, int bright, int fill) {
        BufferedImage image = blank(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int left = x, top = y, right = w - 1 - x, bottom = h - 1 - y;
                int colour = fill;
                if ((left == 0 || top == 0) && !(right == 0 || bottom == 0)) {
                    colour = dark;
                } else if ((right == 0 || bottom == 0) && !(left == 0 || top == 0)) {
                    colour = bright;
                }
                set(image, x, y, colour);
            }
        }
        return image;
    }

    /** FactoryIO's green rim: the current choice of a list. */
    private static BufferedImage selected(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        for (int k = 0; k < w; k++) {
            set(image, k, 0, 0x3fbf3f);
            set(image, k, h - 1, 0x3fbf3f);
        }
        for (int k = 0; k < h; k++) {
            set(image, 0, k, 0x3fbf3f);
            set(image, w - 1, k, 0x3fbf3f);
        }
        return image;
    }

    /** Darker and hatched: a slot that will take nothing in the current configuration. */
    private static BufferedImage hatched(BufferedImage image) {
        for (int i = 2; i < 16; i += 3) {
            for (int j = 0; j < 16; j++) {
                int y = 1 + ((i + j) % 16);
                if (y < 17) {
                    set(image, 1 + j, y, 0x606060);
                }
            }
        }
        return image;
    }

    /** One gauge segment, lit at the centre, with a dark seam on its last row. */
    private static BufferedImage gauge(int bright, int dim) {
        BufferedImage image = blank(12, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 12; x++) {
                double centre = 1 - Math.abs(x - 5.5) / 6;
                int colour = mix(dim, bright, centre);
                if (y == 3) {
                    colour = mix(0x000000, colour, 0.72);
                }
                set(image, x, y, colour);
            }
        }
        return image;
    }

    private static BufferedImage ascii(String[] rows, Map<Character, Integer> palette) {
        BufferedImage image = blank(rows[0].length(), rows.length);
        for (int y = 0; y < rows.length; y++) {
            if (rows[y].length() != rows[0].length()) {
                throw new IllegalArgumentException("ragged row " + y);
            }
            for (int x = 0; x < rows[y].length(); x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') {
                    continue;
                }
                Integer colour = palette.get(ch);
                if (colour == null) {
                    throw new IllegalArgumentException("unknown colour '" + ch + "'");
                }
                set(image, x, y, colour);
            }
        }
        return image;
    }

    private static Map<Character, Integer> with(Object... pairs) {
        Map<Character, Integer> palette = new HashMap<>(PALETTE);
        for (int i = 0; i < pairs.length; i += 2) {
            palette.put((Character) pairs[i], (Integer) pairs[i + 1]);
        }
        return palette;
    }

    // ------------------------------------------------------------------ output

    private static void write(String name, BufferedImage image) throws IOException {
        ImageIO.write(image, "png", new File(OUT, name + ".png"));
    }

    private static void nineSlice(String name, BufferedImage image, int border) throws IOException {
        write(name, image);
        mcmeta(name, "{\"type\": \"nine_slice\", \"width\": " + image.getWidth() + ", \"height\": "
                + image.getHeight() + ", \"border\": " + border + "}");
    }

    private static void tile(String name, BufferedImage image, int width, int height) throws IOException {
        write(name, image);
        mcmeta(name, "{\"type\": \"tile\", \"width\": " + width + ", \"height\": " + height + "}");
    }

    private static void mcmeta(String name, String scaling) throws IOException {
        Files.writeString(new File(OUT, name + ".png.mcmeta").toPath(),
                "{\n  \"gui\": {\n    \"scaling\": " + scaling + "\n  }\n}\n", StandardCharsets.UTF_8);
    }

    private static BufferedImage blank(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private static void set(BufferedImage image, int x, int y, int rgb) {
        image.setRGB(x, y, 0xFF000000 | rgb);
    }

    private static int mix(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return r << 16 | g << 8 | b;
    }

    private GenerateGuiSprites() {}
}
