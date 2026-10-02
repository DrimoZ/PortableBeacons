package dev.drimoz.portablebeacons.client.gui;

/**
 * The palette of every screen in the mod - FactoryIO's, so the two read as one family.
 *
 * <p>Two backgrounds, two rules:
 * <ul>
 *   <li><b>on the window</b> (vanilla light grey): dark text, no shadow;</li>
 *   <li><b>inside a tab</b> (a saturated tint): light text, shadowed.</li>
 * </ul>
 * A colour means one thing. Green says "this works", red says "this will not work without you":
 * never red for decoration - a locked effect is progress, not a problem.
 *
 * <p>Every value carries its alpha. Text colours are strict ARGB in 26.1: a bare {@code 0xRRGGBB}
 * is alpha 0, and draws nothing at all.
 */
public final class GuiTheme {

    // On the window

    public static final int TEXT = 0xFF404040;
    public static final int TEXT_MUTED = 0xFF6E6E6E;
    public static final int TEXT_PROBLEM = 0xFFA8322A;

    // Inside a tab

    public static final int TAB_LABEL = 0xFFD8D8D8;
    public static final int TAB_VALUE = 0xFFFFFFFF;
    public static final int TAB_MUTED = 0xFF9A9A9A;
    /** Line height inside tabs. */
    public static final int LINE = 10;

    // Tab colour is function: on the left what informs, on the right what is set.

    public static final int TAB_INFO = 0xFFC89420;
    public static final int TAB_AUGMENTS = 0xFF3C6FC8;

    // Washes

    /** The hairline between a table's rows: lighter than its outline, so the list reads as one. */
    public static final int TABLE_LINE = 0xFFA8A8A8;

    /**
     * An effect's share of the drain, as a bar under its name. Amber like the fuel it burns; green
     * when a free slot covers it - green means "this works", and free is that.
     */
    public static final int DRAIN = 0xFFE08A1E;
    public static final int DRAIN_FREE = 0xFF3FBF3F;
    public static final int DRAIN_EMPTY = 0xFFA8A8A8;

    /** Under the mouse: what a click would land on. FactoryIO's. */
    public static final int HOVER_WASH = 0x60FFFFFF;
    /** Over a "ghost" item: what goes in a slot, not what is in it. FactoryIO's. */
    public static final int GHOST_WASH = 0xA0C6C6C6;
    /** Over something switched off: still there, not running. */
    public static final int OFF_WASH = 0x90303030;
    /** Under an augment that raises a ceiling the beacon is already at: amber, a caution, not an error. */
    public static final int IN_VAIN_WASH = 0x80E0A020;
    /** Over a locked socket or slot. */
    public static final int LOCKED_WASH = 0xB0202020;

    /** The sharing marker on an effect socket: FactoryIO's blue, the same as the augments tab. */
    public static final int SHARED = 0xFF3B78D8;
    public static final int SHARED_EDGE = 0xFF1F4A91;

    /**
     * A beacon's state, in FactoryIO's five shared tints: the band's light and the info tab say the
     * same thing in the same colour.
     */
    public enum Status {
        /** Running and paying for it. */
        WORKING(0xFF90FF90, "led_working"),
        /** On, but nothing enabled to project. */
        WAITING(0xFFFFFFA0, "led_waiting"),
        /** Running, but about to run dry. */
        BLOCKED(0xFFFFC060, "led_blocked"),
        /** Off for want of fuel: it needs you. */
        PROBLEM(0xFFFF8A80, "led_problem"),
        /** Off on purpose. */
        OFF(0xFFB8B8B8, "led_off");

        /** Text tint, on a tab background. */
        public final int tabColour;
        final String sprite;

        Status(int tabColour, String sprite) {
            this.tabColour = tabColour;
            this.sprite = sprite;
        }
    }

    private GuiTheme() {}
}
