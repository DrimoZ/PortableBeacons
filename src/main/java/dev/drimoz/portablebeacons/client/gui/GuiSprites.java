package dev.drimoz.portablebeacons.client.gui;

import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.gui.GuiMetrics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * How each piece of the screen is drawn - FactoryIO's pieces, as GUI sprites.
 *
 * <p>The sprites are written by {@code tools/GenerateGuiSprites.java}, each with its own scaling
 * in its mcmeta, so no class here knows a texture coordinate: a frame is asked for at a size and the
 * game slices it. That is the part FactoryIO's sheet could not do, and the reason this file is a
 * list of names rather than of u/v pairs.
 */
public final class GuiSprites {

    public static final int SLOT_SIZE = GuiMetrics.SLOT;
    public static final int SOCKET_SIZE = GuiMetrics.SOCKET;
    public static final int ICON_SIZE = 12;

    private static final ResourceLocation PANEL = id("panel");
    private static final ResourceLocation TAB = id("tab");
    private static final ResourceLocation INSET = id("inset");
    private static final ResourceLocation FIELD = id("field");
    private static final ResourceLocation SLOT = id("slot");
    private static final ResourceLocation SLOT_DISABLED = id("slot_disabled");
    private static final ResourceLocation SLOT_SELECTED = id("slot_selected");
    private static final ResourceLocation SOCKET = id("socket");
    private static final ResourceLocation SOCKET_SELECTED = id("socket_selected");
    private static final ResourceLocation GAUGE_FUEL = id("gauge_fuel");
    private static final ResourceLocation GAUGE_EMPTY = id("gauge_empty");
    private static final ResourceLocation LOCK_SMALL = id("lock_small");
    private static final ResourceLocation TABLE = id("table");

    // Frames

    /** The main window, in the tone of vanilla containers. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, PANEL, x, y, width, height);
    }

    /** A side tab, tinted the colour of its function. */
    public static void tab(GuiGraphics graphics, int x, int y, int width, int height, int argb) {
        // 1.21.1 sprites take no tint of their own: the shader colour carries it, and is put back.
        graphics.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F,
                ((argb >>> 24) & 0xFF) / 255.0F);
        graphics.blitSprite(TAB, x, y, width, height);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public enum ButtonState {
        NORMAL("button"), HOVERED("button_hover"), PRESSED("button_pressed"), DISABLED("button_disabled");

        final ResourceLocation sprite;

        ButtonState(String name) {
            this.sprite = id(name);
        }
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height,
                              ButtonState state) {
        blit(graphics, state.sprite, x, y, width, height);
    }

    /** A one-pixel recess: gauges, text areas. */
    public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, INSET, x, y, width, height);
    }

    /** A text field: a dark recess where white text reads. */
    public static void field(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, FIELD, x, y, width, height);
    }

    // Slots

    /** The frame of a slot whose item sits at {@code (itemX, itemY)}. */
    public static void slot(GuiGraphics graphics, int itemX, int itemY) {
        blit(graphics, SLOT, itemX - 1, itemY - 1, SLOT_SIZE, SLOT_SIZE);
    }

    /** Darker and hatched: a slot that will take nothing in the current configuration. */
    public static void disabledSlot(GuiGraphics graphics, int itemX, int itemY) {
        blit(graphics, SLOT_DISABLED, itemX - 1, itemY - 1, SLOT_SIZE, SLOT_SIZE);
    }

    /** FactoryIO's green rim: the current choice of a list. */
    public static void selectedSlot(GuiGraphics graphics, int itemX, int itemY) {
        blit(graphics, SLOT_SELECTED, itemX - 1, itemY - 1, SLOT_SIZE, SLOT_SIZE);
    }

    public static void socket(GuiGraphics graphics, int x, int y, boolean selected) {
        blit(graphics, selected ? SOCKET_SELECTED : SOCKET, x, y, SOCKET_SIZE, SOCKET_SIZE);
    }

    /** The small padlock, in the bottom-right corner of a 16-pixel square. */
    public static void smallLock(GuiGraphics graphics, int itemX, int itemY) {
        blit(graphics, LOCK_SMALL, itemX + 9, itemY + 8, 7, 8);
    }

    /**
     * A "ghost" item: what goes in this slot, washed out. Not contents - it cannot be taken, and the
     * wash is what tells it from a real item.
     *
     * <p>The wash is lifted above the item's depth: drawn at the same depth it would land under the
     * item it is meant to cover.
     */
    public static void ghostItem(GuiGraphics graphics, ItemStack stack, int itemX, int itemY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderFakeItem(stack, itemX, itemY);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.fill(itemX, itemY, itemX + 16, itemY + 16, GuiTheme.GHOST_WASH);
        graphics.pose().popPose();
    }

    public static final int SWITCH_W = 14;
    public static final int SWITCH_H = 8;
    public static final int BIG_SWITCH_W = 20;
    public static final int BIG_SWITCH_H = 10;

    /** A slide switch: green, knob right, when on. {@code big} is the band's, which governs the beacon. */
    public static void toggle(GuiGraphics graphics, int x, int y, boolean on, boolean big) {
        String name = (big ? "switch_big_" : "switch_") + (on ? "on" : "off");
        blit(graphics, id(name), x, y, big ? BIG_SWITCH_W : SWITCH_W, big ? BIG_SWITCH_H : SWITCH_H);
    }

    /** A sunken outline around the window's own colour: a list that reads as one thing. */
    public static void table(GuiGraphics graphics, int x, int y, int width, int height) {
        blit(graphics, TABLE, x, y, width, height);
    }

    // Gauge and lights

    /**
     * A vertical gauge filled from the bottom, over the whole height given.
     *
     * <p>Both layers are tiled from the gauge's own top and the lit one is clipped, so its segments
     * stay put while the level moves instead of sliding with it.
     */
    public static void fuelGauge(GuiGraphics graphics, int x, int y, int width, int height,
                                 float fill) {
        inset(graphics, x, y, width, height);
        int innerX = x + 1;
        int innerY = y + 1;
        int innerW = width - 2;
        int innerH = height - 2;
        blit(graphics, GAUGE_EMPTY, innerX, innerY, innerW, innerH);

        int filled = Math.round(innerH * Math.clamp(fill, 0.0F, 1.0F));
        if (filled <= 0) {
            return;
        }
        graphics.enableScissor(innerX, innerY + innerH - filled, innerX + innerW, innerY + innerH);
        blit(graphics, GAUGE_FUEL, innerX, innerY, innerW, innerH);
        graphics.disableScissor();
    }

    /** The band's status light, in the tint of {@link GuiTheme.Status}. */
    public static void statusLight(GuiGraphics graphics, GuiTheme.Status status, int x, int y) {
        blit(graphics, id(status.sprite), x, y, GuiMetrics.LED_SIZE, GuiMetrics.LED_SIZE);
    }

    // Icons, 12x12

    public enum Icon {
        INFO("icon_info"), CLEAR("icon_clear"),
        AUGMENTS("icon_augments"), PLUS("icon_plus"),
        AURA_SELF("icon_aura_self"), AURA_TEAM("icon_aura_team"),
        AURA_ALLIES("icon_aura_allies"), AURA_PETS("icon_aura_pets");

        final ResourceLocation sprite;

        Icon(String name) {
            this.sprite = id(name);
        }
    }

    public static void icon(GuiGraphics graphics, Icon icon, int x, int y) {
        blit(graphics, icon.sprite, x, y, ICON_SIZE, ICON_SIZE);
    }

    // Inner work

    private static void blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int w, int h) {
        graphics.blitSprite(sprite, x, y, w, h);
    }

    private static ResourceLocation id(String name) {
        return BPRegistryKeys.id(name);
    }

    private GuiSprites() {}
}
