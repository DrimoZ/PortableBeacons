package dev.drimoz.portablebeacons.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A button that lives inside a tab - YetAnotherFarmingStation's, ported.
 *
 * <p>Not a vanilla widget: a tab's content is drawn and clicked in the tab's own coordinates, and
 * only once the tab is fully open, so a widget the screen owns would sit in the wrong place for most
 * of the animation. Everything it shows is a supplier read each frame, so the button never holds a
 * stale label or state; the sound is the screen's, played once for any click a tab consumed.
 */
public final class IconButton {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private Supplier<GuiSprites.Icon> icon = () -> null;
    private Supplier<Component> label = () -> null;
    private BooleanSupplier pressed = () -> false;
    private BooleanSupplier active = () -> true;
    private Supplier<List<Component>> tooltip = List::of;
    private Runnable action = () -> {};

    /** Position is relative to the tab's content origin. */
    public IconButton(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public IconButton icon(Supplier<GuiSprites.Icon> icon) {
        this.icon = icon;
        return this;
    }

    public IconButton label(Supplier<Component> label) {
        this.label = label;
        return this;
    }

    /** Drawn sunk: this is the current choice. */
    public IconButton pressed(BooleanSupplier pressed) {
        this.pressed = pressed;
        return this;
    }

    /** Greyed and deaf to clicks: there is nothing for it to do right now. */
    public IconButton active(BooleanSupplier active) {
        this.active = active;
        return this;
    }

    public IconButton tooltip(Supplier<List<Component>> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public IconButton onPress(Runnable action) {
        this.action = action;
        return this;
    }

    public void render(GuiGraphics graphics, Font font, int originX, int originY, int mouseX, int mouseY) {
        int left = originX + x;
        int top = originY + y;
        boolean enabled = active.getAsBoolean();

        GuiSprites.ButtonState state = !enabled ? GuiSprites.ButtonState.DISABLED
                : pressed.getAsBoolean() ? GuiSprites.ButtonState.PRESSED
                : isOver(originX, originY, mouseX, mouseY) ? GuiSprites.ButtonState.HOVERED
                : GuiSprites.ButtonState.NORMAL;
        GuiSprites.button(graphics, left, top, width, height, state);

        GuiSprites.Icon glyph = icon.get();
        Component text = label.get();
        int colour = enabled ? 0xFFFFFFFF : 0xFFA0A0A0;
        int iconY = top + (height - GuiSprites.ICON_SIZE) / 2;
        int textY = top + (height - font.lineHeight) / 2 + 1;

        if (glyph != null && text != null) {
            GuiSprites.icon(graphics, glyph, left + 3, iconY);
            graphics.drawString(font, text, left + 3 + GuiSprites.ICON_SIZE + 4, textY, colour, true);
        } else if (glyph != null) {
            GuiSprites.icon(graphics, glyph, left + (width - GuiSprites.ICON_SIZE) / 2, iconY);
        } else if (text != null) {
            graphics.drawString(font, text, left + (width - font.width(text)) / 2, textY, colour, true);
        }
    }

    public boolean isOver(int originX, int originY, double mouseX, double mouseY) {
        int left = originX + x;
        int top = originY + y;
        return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
    }

    /** @return whether it took the click: over it and active. */
    public boolean mouseClicked(int originX, int originY, double mouseX, double mouseY) {
        if (!isOver(originX, originY, mouseX, mouseY) || !active.getAsBoolean()) {
            return false;
        }
        action.run();
        return true;
    }

    public List<Component> tooltipAt(int originX, int originY, double mouseX, double mouseY) {
        return isOver(originX, originY, mouseX, mouseY) ? tooltip.get() : List.of();
    }
}
