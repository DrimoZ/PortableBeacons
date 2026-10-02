package dev.drimoz.portablebeacons.client.gui;

import dev.drimoz.portablebeacons.gui.GuiMetrics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A screen's tabs, on both sides - FactoryIO's, ported.
 *
 * <p>One rule, from Thermal: <b>one open tab per side</b>. Opening one shuts the last, and the ones
 * below slide to make room. The last one opened is remembered for the session and reopened: a
 * player going through their beacons should not have to reopen the augments every time.
 */
public final class SideTabs {

    /** The open tab on each side, across screens. {@code null}: shut on purpose. */
    private static final Map<SideTab.Side, String> REMEMBERED = new EnumMap<>(SideTab.Side.class);

    private final List<SideTab> tabs = new ArrayList<>();
    private final int guiWidth;
    private long lastFrame = Util.getMillis();

    /**
     * @param guiWidth    width of the window, which the right-hand tabs sit against
     * @param defaultOpen tabs to open, on their side, when nothing is remembered there yet
     */
    public SideTabs(List<SideTab> tabs, int guiWidth, String... defaultOpen) {
        this.guiWidth = guiWidth;
        this.tabs.addAll(tabs);
        for (String id : defaultOpen) {
            for (SideTab tab : this.tabs) {
                if (tab.id().equals(id) && !REMEMBERED.containsKey(tab.side())) {
                    REMEMBERED.put(tab.side(), id);
                }
            }
        }
        for (SideTab tab : this.tabs) {
            if (tab.id().equals(REMEMBERED.get(tab.side()))) {
                tab.snapOpen();
            }
        }
    }

    // Rendering

    /** Tab bodies, drawn <b>before</b> the main window: it is the window that covers their inner edge. */
    public void renderBackgrounds(GuiGraphicsExtractor graphics, int guiLeft, int guiTop) {
        long now = Util.getMillis();
        float elapsed = Math.min(100.0F, now - lastFrame);
        lastFrame = now;
        for (SideTab tab : tabs) {
            tab.animate(elapsed);
        }
        layout(guiLeft, guiTop);
        for (SideTab tab : tabs) {
            tab.renderBackground(graphics);
        }
    }

    public void renderForegrounds(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        for (SideTab tab : tabs) {
            tab.renderForeground(graphics, font, mouseX, mouseY);
        }
    }

    /** The tooltip under the mouse: a shut tab's title, or whatever its content offers. */
    public List<Component> tooltipAt(int mouseX, int mouseY) {
        for (SideTab tab : tabs) {
            if (!tab.contains(mouseX, mouseY)) {
                continue;
            }
            if (!tab.isFullyOpen()) {
                return List.of(tab.title());
            }
            return tab.contentTooltip(tab.contentX(), tab.contentY(), mouseX, mouseY);
        }
        return List.of();
    }

    // Interaction

    /**
     * @return {@code true} if a tab took the click. A click in the content that no control takes is
     *         left to the screen: it may be a slot, like the augments'.
     */
    public boolean mouseClicked(double mouseX, double mouseY) {
        for (SideTab tab : tabs) {
            if (!tab.contains(mouseX, mouseY)) {
                continue;
            }
            if (tab.headerContains(mouseX, mouseY) || !tab.isFullyOpen()) {
                toggle(tab);
                return true;
            }
            return false;
        }
        return false;
    }

    /** Whether the mouse is over a tab, so a click there is not taken for dropping an item. */
    public boolean contains(double mouseX, double mouseY) {
        for (SideTab tab : tabs) {
            if (tab.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /** The rectangles in use, so JEI does not lay its item list over them. */
    public List<Rect2i> areas() {
        List<Rect2i> areas = new ArrayList<>(tabs.size());
        for (SideTab tab : tabs) {
            areas.add(new Rect2i(tab.left(), tab.top(), tab.width(), tab.height()));
        }
        return areas;
    }

    // Inner work

    private void toggle(SideTab tab) {
        boolean opening = !tab.isOpen();
        for (SideTab other : tabs) {
            if (other.side() == tab.side()) {
                other.setOpen(false);
            }
        }
        tab.setOpen(opening);
        REMEMBERED.put(tab.side(), opening ? tab.id() : null);
    }

    /** Stacks each side's tabs, allowing for each one's current size. */
    private void layout(int guiLeft, int guiTop) {
        int left = guiTop + GuiMetrics.TAB_TOP;
        int right = guiTop + GuiMetrics.TAB_TOP;
        for (SideTab tab : tabs) {
            if (tab.side() == SideTab.Side.RIGHT) {
                tab.x = guiLeft + guiWidth;
                tab.y = right;
                right += tab.height() + GuiMetrics.TAB_GAP;
            } else {
                tab.x = guiLeft;
                tab.y = left;
                left += tab.height() + GuiMetrics.TAB_GAP;
            }
        }
    }
}
