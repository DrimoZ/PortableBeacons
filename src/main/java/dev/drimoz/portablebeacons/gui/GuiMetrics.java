package dev.drimoz.portablebeacons.gui;

/**
 * The grid every part of the screen is laid on - FactoryIO's, so the two mods' screens share it.
 *
 * <p>Outside {@code client/} because the menu needs it: a slot's position is fixed when the menu
 * is built, on both sides. Colours live in {@code client/gui/GuiTheme}.
 *
 * <p><b>Everything aligns to the player inventory's columns</b>: slot frames at {@code 7 + 18 n}.
 * The fuel gauge takes column 0, the beacon's own content starts at column 1. That alignment is
 * what makes the screens look like a set.
 */
public final class GuiMetrics {

    // Window

    public static final int WIDTH = 176;
    public static final int MARGIN = 8;
    /** The band: the title, and on the right the status light. */
    public static final int TITLE_Y = 7;
    public static final int CONTENT_TOP = 20;

    // Pieces

    public static final int SLOT = 18;
    /** The large socket for what the screen is about: here, an effect. */
    public static final int SOCKET = 26;

    /** Left edge of the slot frame in inventory column n. */
    public static int column(int n) {
        return MARGIN - 1 + n * SLOT;
    }

    /** Right edge of the last inventory column - where content stops. */
    public static final int CONTENT_RIGHT = WIDTH - MARGIN + 1;

    public static final int GAUGE_WIDTH = 14;
    public static final int GAUGE_X = column(0) + (SLOT - GAUGE_WIDTH) / 2;

    public static final int LED_SIZE = 7;
    public static final int LED_X = WIDTH - MARGIN - LED_SIZE;
    public static final int LED_Y = TITLE_Y;

    // Player inventory, below the content. Item positions, not frame positions.

    public static int inventoryY(int contentBottom) {
        return contentBottom + 15;
    }

    public static int inventoryLabelY(int contentBottom) {
        return inventoryY(contentBottom) - 11;
    }

    public static int hotbarY(int contentBottom) {
        return inventoryY(contentBottom) + 58;
    }

    public static int height(int contentBottom) {
        return hotbarY(contentBottom) + SLOT + 6;
    }

    // Side tabs

    /** Top of the first tab, on either side. */
    public static final int TAB_TOP = 4;
    /** Side of a closed tab, and height of an open one's header. */
    public static final int TAB_HEADER = 22;
    public static final int TAB_PADDING = 6;
    public static final int TAB_GAP = 2;

    /** Where the first right-hand tab's content starts once open - slots inside it sit here. */
    public static final int RIGHT_TAB_CONTENT_X = WIDTH + TAB_PADDING;
    public static final int FIRST_TAB_CONTENT_Y = TAB_TOP + TAB_HEADER;

    private GuiMetrics() {}
}
