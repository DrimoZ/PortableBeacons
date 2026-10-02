package dev.drimoz.portablebeacons.client.gui;

import dev.drimoz.portablebeacons.gui.GuiMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * A side tab, the way Thermal does them - FactoryIO's, ported.
 *
 * <p>Shut, it is a coloured square carrying an icon; open, it slides out to its size and shows its
 * content. The colour says the function and the side says the kind: on the left what informs, on
 * the right what is set.
 *
 * <p>Content is drawn and clickable only once the tab is <b>fully</b> open: a control sliding under
 * the cursor mid-animation would catch clicks meant for something else.
 */
public abstract class SideTab {

    public enum Side { LEFT, RIGHT }

    private static final float OPENING_MS = 160.0F;
    /** Where the title starts in the header, after the icon. */
    private static final int TITLE_X = 24;

    private final String id;
    private final Side side;
    private final int colour;

    private boolean open;
    private float openness;

    // Absolute position this frame, set by SideTabs.
    int x;
    int y;

    protected SideTab(String id, Side side, int colour) {
        this.id = id;
        this.side = side;
        this.colour = colour;
    }

    // Content, supplied by each tab

    protected abstract Component title();

    /** The header icon, 12x12, centred in the header square. */
    protected abstract GuiSprites.Icon icon();

    protected abstract int contentWidth();

    protected abstract int contentHeight();

    /** Under the items: slot frames and anything else the slots sit on. */
    protected void renderContentBackground(GuiGraphicsExtractor graphics, int x, int y) {}

    /** {@code x, y}: origin of the content, below the header. */
    protected abstract void renderContent(GuiGraphicsExtractor graphics, Font font, int x, int y,
                                          int mouseX, int mouseY);

    protected List<Component> contentTooltip(int x, int y, int mouseX, int mouseY) {
        return List.of();
    }

    /** Called every frame: the augment tab shows or hides its slots from here. */
    protected void onOpennessChanged(boolean fullyOpen) {}

    // Interface

    public String id() {
        return id;
    }

    public Side side() {
        return side;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isFullyOpen() {
        return open && openness >= 1.0F;
    }

    void setOpen(boolean open) {
        this.open = open;
    }

    /** Opens without animating: a remembered tab is already open when the screen appears. */
    void snapOpen() {
        open = true;
        openness = 1.0F;
    }

    void animate(float elapsedMs) {
        float target = open ? 1.0F : 0.0F;
        float step = elapsedMs / OPENING_MS;
        openness = openness < target
                ? Math.min(target, openness + step)
                : Math.max(target, openness - step);
        onOpennessChanged(isFullyOpen());
    }

    public int width() {
        return Math.round(Mth.lerp(eased(), GuiMetrics.TAB_HEADER, fullWidth()));
    }

    public int height() {
        return Math.round(Mth.lerp(eased(), GuiMetrics.TAB_HEADER, fullHeight()));
    }

    /** Wide enough for the content and for the title: it never runs out of its own tab. */
    private int fullWidth() {
        int titled = TITLE_X + Minecraft.getInstance().font.width(title()) + GuiMetrics.TAB_PADDING;
        return Math.max(titled, contentWidth() + 2 * GuiMetrics.TAB_PADDING);
    }

    private int fullHeight() {
        return GuiMetrics.TAB_HEADER + contentHeight() + GuiMetrics.TAB_PADDING;
    }

    /** Slows at the end of its travel: the tab settles instead of stopping dead. */
    private float eased() {
        return 1.0F - (1.0F - openness) * (1.0F - openness);
    }

    // Absolute geometry

    /**
     * The drawn rectangle runs four pixels under the main window, which is drawn over it: the tab
     * looks like it comes out of the window rather than being parked against it.
     */
    int drawX() {
        return side == Side.RIGHT ? x - 4 : x - width();
    }

    int drawWidth() {
        return width() + 4;
    }

    public int left() {
        return side == Side.RIGHT ? x : x - width();
    }

    public int top() {
        return y;
    }

    int contentX() {
        return left() + GuiMetrics.TAB_PADDING;
    }

    int contentY() {
        return y + GuiMetrics.TAB_HEADER;
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= left() && mouseX < left() + width() && mouseY >= y && mouseY < y + height();
    }

    boolean headerContains(double mouseX, double mouseY) {
        return contains(mouseX, mouseY) && mouseY < y + GuiMetrics.TAB_HEADER;
    }

    void renderBackground(GuiGraphicsExtractor graphics) {
        GuiSprites.tab(graphics, drawX(), y, drawWidth(), height(), colour);
        if (isFullyOpen()) {
            renderContentBackground(graphics, contentX(), contentY());
        }
    }

    void renderForeground(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        int iconX = left() + (GuiMetrics.TAB_HEADER - GuiSprites.ICON_SIZE) / 2;
        int iconY = y + (GuiMetrics.TAB_HEADER - GuiSprites.ICON_SIZE) / 2;
        GuiSprites.icon(graphics, icon(), iconX, iconY);
        if (!isFullyOpen()) {
            return;
        }
        graphics.text(font, title(), left() + TITLE_X, y + 7, GuiTheme.TAB_VALUE, true);
        renderContent(graphics, font, contentX(), contentY(), mouseX, mouseY);
    }
}
