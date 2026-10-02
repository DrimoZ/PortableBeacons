package dev.drimoz.portablebeacons.client;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.BeaconTicker;
import dev.drimoz.portablebeacons.client.gui.GuiSprites;
import dev.drimoz.portablebeacons.client.gui.GuiTheme;
import dev.drimoz.portablebeacons.client.gui.SideTab;
import dev.drimoz.portablebeacons.client.gui.SideTabs;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.gui.GuiMetrics;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import dev.drimoz.portablebeacons.net.BeaconActionPayload;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The beacon's screen, laid out on FactoryIO's charter so the two mods' screens read as a set.
 *
 * <pre>
 *  ┌────────────────────────────────────────┐
 *  │ Portable Beacon IV                  ⏻ ● │  band: title, power, status light
 *  │ ▌ [⚔] Strength              II  👤  ━● │  one row per effect, everything set in place
 *  │ ▌ [🛡] Resistance             I  👥  ━● │
 *  │ ▌ [ + ] Click to pick an effect          │
 *  │ ▌ [🔒] Unlocked by a higher tier…       │
 *  │ ▣                                        │  fuel gauge and slot in column 0
 *  │ Inventory                                │
 *  └────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>One row per effect, and nothing hidden.</b> The previous screens had a row of sockets and a
 * separate panel for "the focused one", so every change was two steps and the controls on screen
 * belonged to whichever socket had last been clicked - state a player had to keep in their head.
 * Here each row carries its own effect, level, audience and switch; what was a row of buttons is
 * now the row itself, and the per-effect figures moved into the row's tooltip.
 *
 * <p>Tabs: the beacon's figures on the left in yellow, because they inform; augments on the right
 * in blue, because they are set. The effect picker is FactoryIO's recipe picker - a modal grid over
 * the whole window. Every colour comes from {@link GuiTheme} and every piece from {@link GuiSprites}.
 */
public class PortableBeaconScreen extends AbstractContainerScreen<PortableBeaconMenu> {

    static final int IMAGE_W = GuiMetrics.WIDTH;
    static final int IMAGE_H = GuiMetrics.height(PortableBeaconMenu.CONTENT_BOTTOM);

    // Band

    private static final int ICON = GuiSprites.ICON_SIZE;
    /** The master switch, beside the light: what the player chose, next to what is happening. */
    private static final int POWER_X = GuiMetrics.LED_X - 5 - GuiSprites.BIG_SWITCH_W;
    private static final int POWER_Y = GuiMetrics.LED_Y + GuiMetrics.LED_SIZE / 2 - GuiSprites.BIG_SWITCH_H / 2;
    private static final int POWER_W = GuiSprites.BIG_SWITCH_W;
    private static final int POWER_H = GuiSprites.BIG_SWITCH_H;

    // Fuel column

    private static final int GAUGE_Y = GuiMetrics.CONTENT_TOP;
    /** Down to the fuel slot's frame, with a two-pixel gap. */
    private static final int GAUGE_H = PortableBeaconMenu.FUEL_SLOT_Y - 1 - 2 - GAUGE_Y;

    // Effect rows: one inventory slot tall, from column 1 to the content's right edge

    private static final int ROW_X = GuiMetrics.column(1);
    private static final int ROW_RIGHT = GuiMetrics.CONTENT_RIGHT;
    private static final int ROW_H = GuiMetrics.SLOT;
    private static final int NAME_X = ROW_X + 21;
    /** The row's controls, right to left: switch, audience, level badge. */
    private static final int SWITCH_X = ROW_RIGHT - 3 - GuiSprites.SWITCH_W;
    private static final int AURA_X = SWITCH_X - 5 - ICON;
    /** Wide enough for VIII, the widest numeral a level reaches. */
    private static final int LEVEL_W = 20;
    private static final int LEVEL_H = 12;
    private static final int LEVEL_X = AURA_X - 5 - LEVEL_W;
    private static final int NAME_W = LEVEL_X - 4 - NAME_X;

    /** What a click in a row lands on. */
    private enum Part { ICON, NAME, LEVEL, AURA, SWITCH, NONE }

    // The picker: FactoryIO's recipe picker, with effects for items

    private static final int FIELD_X = GuiMetrics.column(0);
    private static final int FIELD_W = GuiMetrics.CONTENT_RIGHT - FIELD_X;
    private static final int FIELD_Y = GuiMetrics.CONTENT_TOP - 2;
    private static final int FIELD_H = 14;
    private static final int GRID_Y = FIELD_Y + FIELD_H + 4;
    /** Eight columns of the inventory grid; the ninth carries the scrollbar. */
    private static final int GRID_COLUMNS = 8;
    private static final int FOOTER_Y = IMAGE_H - 12;
    private static final int GRID_ROWS = (FOOTER_Y - 2 - GRID_Y) / GuiMetrics.SLOT;
    private static final int SCROLLBAR_X = GuiMetrics.column(GRID_COLUMNS) + 3;
    private static final int SCROLLBAR_W = 12;

    private final SideTabs tabs;

    /** First effect row shown, when a beacon has more than fit. */
    private int rowScroll;
    private boolean selectorOpen;
    /** The effect row the picker will fill. */
    private int selectorSlot;
    /** In rows of the grid. */
    private int scroll;
    /** Index into the filtered effects; driven by both the mouse and the arrow keys. */
    private int highlighted;
    private String search = "";
    private List<ResourceKey<BeaconEffectDef>> allKeysCache;
    private List<ResourceKey<BeaconEffectDef>> rowsCache;
    private String rowsCacheKey;
    private ItemStack fuelGhost;
    /** Cleared at the top of every frame; see {@link #stats()}. */
    private BeaconStats frameStats;

    public PortableBeaconScreen(PortableBeaconMenu menu, Inventory inventory, Component title) {
        // The size goes through the constructor: both fields are final now.
        super(menu, inventory, title, IMAGE_W, IMAGE_H);
        this.titleLabelX = GuiMetrics.MARGIN;
        this.titleLabelY = GuiMetrics.TITLE_Y;
        this.inventoryLabelX = GuiMetrics.MARGIN;
        this.inventoryLabelY = GuiMetrics.inventoryLabelY(PortableBeaconMenu.CONTENT_BOTTOM);
        this.tabs = new SideTabs(List.of(new InfoTab(), new AugmentTab()), IMAGE_W, AugmentTab.ID);
    }

    /**
     * The beacon's resolved stats, computed at most once per frame.
     *
     * <p>Each call walks the augment slots through a capability lookup and re-applies every
     * operation. Rendering asks for it many times a frame, which is far more often than it can change.
     */
    private BeaconStats stats() {
        if (frameStats == null) {
            frameStats = menu.stats();
        }
        return frameStats;
    }

    /** For JEI, which would otherwise lay its item list over the tabs. */
    public List<Rect2i> extraAreas() {
        return selectorOpen ? List.of() : tabs.areas();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        frameStats = null;
        // Tabs first: the window is drawn over their inner edge. The picker covers everything, so
        // nothing of the beacon - tabs included - shows through it or takes a click.
        if (!selectorOpen) {
            tabs.renderBackgrounds(graphics, leftPos, topPos);
        }
        GuiSprites.panel(graphics, leftPos, topPos, imageWidth, imageHeight);

        for (Slot slot : menu.slots) {
            if (!(slot instanceof ResourceHandlerSlot)) {
                GuiSprites.slot(graphics, leftPos + slot.x, topPos + slot.y);
            }
        }
        if (BPConfig.fuelEnabled()) {
            GuiSprites.fuelGauge(graphics, leftPos + GuiMetrics.GAUGE_X, topPos + GAUGE_Y,
                    GuiMetrics.GAUGE_WIDTH, GAUGE_H, (float) fuelFill());
            GuiSprites.slot(graphics, leftPos + PortableBeaconMenu.FUEL_SLOT_X,
                    topPos + PortableBeaconMenu.FUEL_SLOT_Y);
        }
        drawGhosts(graphics);
    }

    /**
     * FactoryIO's ghost items: what goes in an empty slot. The fuel slot shows the cheapest fuel,
     * an open augment slot a bare augment. Last in the background, because each ghost lifts what
     * follows it onto a new stratum.
     */
    private void drawGhosts(GuiGraphicsExtractor graphics) {
        if (selectorOpen) {
            return;
        }
        for (Slot slot : menu.slots) {
            if (!(slot instanceof ResourceHandlerSlot handler) || !slot.isActive() || slot.hasItem()) {
                continue;
            }
            int index = handler.getSlotIndex();
            ItemStack ghost;
            if (index == PortableBeaconItem.FUEL_SLOT) {
                ghost = fuelGhost();
            } else if (index - 1 < stats().augmentSlots()) {
                ghost = new ItemStack(BPItems.AUGMENT.get());
            } else {
                continue;
            }
            GuiSprites.ghostItem(graphics, ghost, leftPos + slot.x, topPos + slot.y);
        }
    }

    /** The cheapest fuel named by item: the one a player is most likely to have on them. */
    private ItemStack fuelGhost() {
        if (fuelGhost == null) {
            fuelGhost = Minecraft.getInstance().level.registryAccess().lookupOrThrow(BPRegistryKeys.FUEL)
                    .stream()
                    .filter(def -> def.item().isPresent())
                    .min(Comparator.comparingInt(FuelDef::units))
                    .map(def -> new ItemStack(def.item().get()))
                    .orElse(ItemStack.EMPTY);
        }
        return fuelGhost;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (selectorOpen) {
            // Labels are drawn before the slots in 26.1, so a picker drawn with them would sit under
            // the inventory's items. A new stratum puts it over everything already recorded.
            graphics.nextStratum();
            graphics.pose().pushMatrix();
            graphics.pose().translate(leftPos, topPos);
            drawSelector(graphics, mouseX - leftPos, mouseY - topPos);
            graphics.pose().popMatrix();
        } else {
            tabs.renderForegrounds(graphics, font, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        List<Component> tooltip = selectorOpen ? selectorTooltip(mouseX, mouseY) : tooltipAt(mouseX, mouseY);
        if (!tooltip.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Vanilla's slot tooltips, except over the picker or where this screen has its own.
        if (selectorOpen || !tooltipAt(mouseX, mouseY).isEmpty()) {
            return;
        }
        if (!renderFuelSlotTooltip(graphics, mouseX, mouseY)
                && !renderAugmentSlotTooltip(graphics, mouseX, mouseY)) {
            super.extractTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        drawBand(graphics, x, y);
        if (!selectorOpen) {
            drawRows(graphics, x, y);
        }
    }

    /**
     * The band: title, the master switch, the light. A slide switch for the beacon itself - the
     * power glyph that stood here read as neither a button nor a state - and the light beside it,
     * FactoryIO's, because the two say different things: what the player chose, and what is
     * actually happening (running, low, dry).
     */
    private void drawBand(GuiGraphicsExtractor graphics, int x, int y) {
        GuiSprites.toggle(graphics, POWER_X, POWER_Y, menu.state().active(), true);
        if (within(x, y, POWER_X, POWER_Y, POWER_W, POWER_H)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        GuiSprites.statusLight(graphics, status(), GuiMetrics.LED_X, GuiMetrics.LED_Y);
    }

    /**
     * The effects as one table: a sunken outline holding a row per slot, the rows split by a hairline.
     * Five loose slot frames down the left read as five unrelated boxes; a table reads as a list,
     * which is what it is.
     */
    private void drawRows(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BeaconStats stats = stats();
        List<EffectSlotConfig> effects = menu.state().effects();
        int top = GuiMetrics.CONTENT_TOP;
        int bottom = PortableBeaconMenu.CONTENT_BOTTOM;
        GuiSprites.table(graphics, ROW_X, top, ROW_RIGHT - ROW_X, bottom - top);
        int total = visibleRows(stats);
        rowScroll = Mth.clamp(rowScroll, 0, Math.max(0, total - PortableBeaconMenu.VISIBLE_EFFECT_ROWS));
        for (int i = 1; i < PortableBeaconMenu.VISIBLE_EFFECT_ROWS; i++) {
            int line = top + i * ROW_H - 1;
            graphics.fill(ROW_X + 1, line, ROW_RIGHT - 1, line + 1, GuiTheme.TABLE_LINE);
        }
        drawRowScroll(graphics, total, top, bottom);
        for (int i = rowScroll; i < Math.min(total, rowScroll + PortableBeaconMenu.VISIBLE_EFFECT_ROWS); i++) {
            int y = rowY(i);
            boolean locked = i >= stats.effectSlots();
            boolean hovered = !locked && within(mouseX, mouseY, ROW_X, y, ROW_RIGHT - ROW_X, ROW_H);
            if (hovered) {
                graphics.fill(ROW_X + 1, y, ROW_RIGHT - 1, y + ROW_H - 1, GuiTheme.HOVER_WASH);
                graphics.requestCursor(CursorTypes.POINTING_HAND);
            }

            if (locked) {
                // Shown rather than hidden: the player should see what a higher tier would give. A
                // padlock and nothing else - the sentence saying how to unlock it is the tooltip's,
                // and spelled out in the row it ran off the window in most languages.
                // smallLock sits in a 16-pixel square's corner; offset so it lands in the icon's centre.
                GuiSprites.smallLock(graphics, ROW_X + 2 - 4, y + 1 - 4);
                continue;
            }
            if (i >= effects.size()) {
                GuiSprites.icon(graphics, GuiSprites.Icon.PLUS, ROW_X + 4, y + 3);
                drawRowText(graphics, Component.translatable("portablebeacons.gui.empty_slot"),
                        ROW_RIGHT - 4 - NAME_X, y + 5, GuiTheme.TEXT_MUTED);
                continue;
            }
            drawEffectRow(graphics, i, effects.get(i), stats, y);
        }
    }

    private void drawEffectRow(GuiGraphicsExtractor graphics, int row, EffectSlotConfig slot, BeaconStats stats,
                               int y) {
        Optional<BeaconEffectDef> maybeDef = effectLookup().get(slot.effect());
        if (maybeDef.isEmpty()) {
            return;
        }
        BeaconEffectDef def = maybeDef.get();
        drawEffectIcon(graphics, slot.effect(), ROW_X + 2, y + 1);
        if (!slot.enabled()) {
            graphics.fill(ROW_X + 2, y + 1, ROW_X + 18, y + 17, GuiTheme.OFF_WASH);
        }
        drawRowText(graphics, def.effect().value().getDisplayName(), NAME_W, y + 3,
                slot.enabled() ? GuiTheme.TEXT : GuiTheme.TEXT_MUTED);

        // What this effect takes of the beacon's drain, as a bar under its name - Factorio's power
        // statistics, in one line. Which effect is emptying the beacon is the question the old
        // screen answered only in a tooltip; here it is seen before anything is hovered.
        if (slot.enabled()) {
            int barY = y + 13;
            graphics.fill(NAME_X, barY, NAME_X + NAME_W, barY + 2, GuiTheme.DRAIN_EMPTY);
            int share = (int) Math.round(NAME_W * drainShare(row, slot, stats));
            graphics.fill(NAME_X, barY, NAME_X + share, barY + 2,
                    isFree(row, stats) ? GuiTheme.DRAIN_FREE : GuiTheme.DRAIN);
        }

        // The level as a badge - a dark chip with a white numeral, which reads as something to press
        // the way a bare numeral never did. Muted when it has nowhere to go.
        boolean amplifiable = canAmplify(def, stats, slot.effect());
        int chipY = y + (ROW_H - LEVEL_H) / 2 - 1;
        GuiSprites.field(graphics, LEVEL_X, chipY, LEVEL_W, LEVEL_H);
        String level = roman(slot.amplifier() + 1);
        graphics.text(font, level, LEVEL_X + (LEVEL_W - font.width(level)) / 2 + 1, chipY + 2,
                amplifiable ? GuiTheme.TAB_VALUE : GuiTheme.TAB_MUTED, false);

        // Who it reaches, as a figure: one person for self, blue groups when shared - the costly
        // setting is the visible one. Washed out when the beacon offers nothing else.
        int iconY = y + (ROW_H - ICON) / 2 - 1;
        GuiSprites.icon(graphics, auraIcon(slot.aura()), AURA_X, iconY);
        if (stats.allowedAuraModes().size() <= 1) {
            graphics.fill(AURA_X, iconY, AURA_X + ICON, iconY + ICON, GuiTheme.GHOST_WASH);
        }
        GuiSprites.toggle(graphics, SWITCH_X, y + (ROW_H - GuiSprites.SWITCH_H) / 2 - 1, slot.enabled(), false);
    }

    private void drawRowText(GuiGraphicsExtractor graphics, Component text, int width, int textY, int colour) {
        graphics.text(font, font.plainSubstrByWidth(text.getString(), width), NAME_X, textY, colour, false);
    }

    /** This effect's share of what the beacon draws, 0 to 1 - the same figure the bill uses. */
    private double drainShare(int row, EffectSlotConfig slot, BeaconStats stats) {
        if (isFree(row, stats)) {
            return 1.0;
        }
        return BeaconResolver.share(menu.state().effects(), row, stats, effectLookup());
    }

    /** Whether a free slot - Wellspring's - covers this row. */
    private boolean isFree(int row, BeaconStats stats) {
        boolean[] free = BeaconResolver.freeMask(menu.state().effects(), stats, effectLookup());
        return row < free.length && free[row];
    }

    private static GuiSprites.Icon auraIcon(AuraMode mode) {
        return switch (mode) {
            case SELF -> GuiSprites.Icon.AURA_SELF;
            case TEAM -> GuiSprites.Icon.AURA_TEAM;
            case ALLIES -> GuiSprites.Icon.AURA_ALLIES;
            case ALLIES_AND_PETS -> GuiSprites.Icon.AURA_PETS;
        };
    }

    /**
     * The unlocked rows plus a single locked preview. Five rows of padlocks under a two-slot beacon
     * read as a broken screen rather than as progression.
     */
    private static int visibleRows(BeaconStats stats) {
        return Math.min(BeaconStats.MAX_EFFECT_SLOTS, stats.effectSlots() + 1);
    }

    /** Where a row is drawn, after scrolling. */
    private int rowY(int index) {
        return GuiMetrics.CONTENT_TOP + (index - rowScroll) * ROW_H;
    }

    /**
     * A thin bar on the table's inner edge, only when there are more rows than fit - the cue that
     * the wheel does something here. Two pixels: anything wider would take room from the switches.
     */
    private void drawRowScroll(GuiGraphicsExtractor graphics, int total, int top, int bottom) {
        int visible = PortableBeaconMenu.VISIBLE_EFFECT_ROWS;
        if (total <= visible) {
            return;
        }
        int track = bottom - top - 2;
        int thumb = Math.max(6, track * visible / total);
        int thumbY = top + 1 + (track - thumb) * rowScroll / (total - visible);
        graphics.fill(ROW_RIGHT - 3, thumbY, ROW_RIGHT - 1, thumbY + thumb, GuiTheme.TABLE_LINE);
    }

    /** The row under the mouse, or -1. Window coordinates. */
    private int rowAt(int x, int y) {
        if (x < ROW_X || x >= ROW_RIGHT || y < GuiMetrics.CONTENT_TOP) {
            return -1;
        }
        int local = (y - GuiMetrics.CONTENT_TOP) / ROW_H;
        if (local >= PortableBeaconMenu.VISIBLE_EFFECT_ROWS) {
            return -1;
        }
        int index = rowScroll + local;
        return index < visibleRows(stats()) ? index : -1;
    }

    private static Part partAt(int x) {
        if (x < ROW_X + ROW_H) {
            return Part.ICON;
        }
        if (x >= SWITCH_X - 2) {
            return Part.SWITCH;
        }
        if (x >= AURA_X - 2) {
            return Part.AURA;
        }
        if (x >= LEVEL_X) {
            return Part.LEVEL;
        }
        return Part.NAME;
    }

    private void drawEffectIcon(GuiGraphicsExtractor graphics, ResourceKey<BeaconEffectDef> key, int x, int y) {
        // Straight from vanilla's effect sprites, so any registered effect - vanilla, another mod's,
        // or one added by a datapack - shows its own icon with no texture from us.
        effectLookup().get(key).ifPresent(def -> graphics.blitSprite(RenderPipelines.GUI_TEXTURED,
                Gui.getMobEffectSprite(def.effect()), x, y, 16, 16));
    }

    // ------------------------------------------------------------------ status

    /**
     * The band's light, in FactoryIO's five states. The info tab's first line says the same thing in
     * the same colour.
     */
    private GuiTheme.Status status() {
        BeaconState state = menu.state();
        boolean fuel = BPConfig.fuelEnabled();
        if (!state.active()) {
            return GuiTheme.Status.OFF;
        }
        if (state.starved()) {
            // On, but dry: the one state that needs the player. It resumes by itself once fed.
            return GuiTheme.Status.PROBLEM;
        }
        if (BPConfig.disabledIn(minecraft.player.level().dimension())) {
            return GuiTheme.Status.WAITING;
        }
        double perSecond = BeaconResolver.fuelPerSecond(state, stats(), effectLookup());
        if (perSecond <= 0.0) {
            // A beacon that burns nothing - the creative one - is working whenever it projects.
            return burnsNothing() && state.effects().stream().anyMatch(EffectSlotConfig::enabled)
                    ? GuiTheme.Status.WORKING
                    : GuiTheme.Status.WAITING;
        }
        if (fuel && (state.fuel() + reserveUnits()) / perSecond < BeaconTicker.LOW_FUEL_SECONDS) {
            return GuiTheme.Status.BLOCKED;
        }
        return GuiTheme.Status.WORKING;
    }

    private Component statusText(GuiTheme.Status status) {
        return switch (status) {
            case WORKING -> Component.translatable("portablebeacons.gui.active");
            case WAITING -> BPConfig.disabledIn(minecraft.player.level().dimension())
                    ? Component.translatable("portablebeacons.gui.disabled_here")
                    : Component.translatable("portablebeacons.gui.idle");
            case BLOCKED -> Component.translatable("portablebeacons.gui.low_fuel", totalRuntime());
            case PROBLEM -> Component.translatable("portablebeacons.msg.out_of_fuel");
            case OFF -> Component.translatable("portablebeacons.gui.inactive");
        };
    }

    // ------------------------------------------------------------------ tabs

    /** The beacon's figures. On the left and yellow: it informs, it sets nothing. */
    private final class InfoTab extends SideTab {

        InfoTab() {
            super("info", Side.LEFT, GuiTheme.TAB_INFO);
        }

        @Override
        protected Component title() {
            return Component.translatable("portablebeacons.gui.stats");
        }

        @Override
        protected GuiSprites.Icon icon() {
            return GuiSprites.Icon.INFO;
        }

        private List<Component> lines() {
            BeaconState state = menu.state();
            BeaconStats stats = stats();
            List<Component> lines = new ArrayList<>(4);
            lines.add(statusText(status()));
            lines.add(Component.translatable("portablebeacons.gui.range",
                    String.format(Locale.ROOT, "%.0f", stats.range())));
            if (BPConfig.fuelEnabled() && !burnsNothing()) {
                lines.add(Component.translatable("portablebeacons.gui.runtime", totalRuntime()));
                // Wayfarer and Sentinel price moving and standing differently, and one figure would
                // be wrong for whichever the player is not doing. Shown only when the two differ.
                if (stats.movingCostMultiplier() != stats.stillCostMultiplier()) {
                    int units = state.fuel() + reserveUnits();
                    lines.add(Component.translatable("portablebeacons.gui.runtime_motion",
                            atCurrentDraw(units, BeaconResolver.fuelPerSecond(state, stats, effectLookup(), true)),
                            atCurrentDraw(units, BeaconResolver.fuelPerSecond(state, stats, effectLookup(), false))));
                }
            }
            lines.add(Component.translatable("portablebeacons.gui.slots",
                    state.effects().size(), stats.effectSlots()));
            return lines;
        }

        @Override
        protected int contentWidth() {
            int widest = 0;
            for (Component line : lines()) {
                widest = Math.max(widest, font.width(line));
            }
            return widest;
        }

        @Override
        protected int contentHeight() {
            return lines().size() * GuiTheme.LINE;
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, int x, int y,
                                     int mouseX, int mouseY) {
            List<Component> lines = lines();
            for (int i = 0; i < lines.size(); i++) {
                int colour = i == 0 ? status().tabColour : GuiTheme.TAB_LABEL;
                graphics.text(font, lines.get(i), x, y + i * GuiTheme.LINE, colour, true);
            }
        }
    }

    /**
     * The augment slots. On the right and blue: it sets something. The slots themselves belong to
     * the menu and sit where this tab's content is once fully open.
     */
    private final class AugmentTab extends SideTab {

        static final String ID = "augments";

        AugmentTab() {
            super(ID, Side.RIGHT, GuiTheme.TAB_AUGMENTS);
        }

        @Override
        protected Component title() {
            return Component.translatable("portablebeacons.gui.augments");
        }

        @Override
        protected GuiSprites.Icon icon() {
            return GuiSprites.Icon.AUGMENTS;
        }

        @Override
        protected int contentWidth() {
            return PortableBeaconMenu.AUGMENTS_PER_ROW * GuiMetrics.SLOT;
        }

        @Override
        protected int contentHeight() {
            return menu.augmentRows() * GuiMetrics.SLOT;
        }

        private int shown() {
            return Math.min(PortableBeaconItem.AUGMENT_SLOTS, menu.augmentRows() * PortableBeaconMenu.AUGMENTS_PER_ROW);
        }

        private static int cellX(int x, int i) {
            return x + 1 + (i % PortableBeaconMenu.AUGMENTS_PER_ROW) * GuiMetrics.SLOT;
        }

        private static int cellY(int y, int i) {
            return y + 1 + (i / PortableBeaconMenu.AUGMENTS_PER_ROW) * GuiMetrics.SLOT;
        }

        @Override
        protected void renderContentBackground(GuiGraphicsExtractor graphics, int x, int y) {
            for (int i = 0; i < shown(); i++) {
                if (i < stats().augmentSlots()) {
                    GuiSprites.slot(graphics, cellX(x, i), cellY(y, i));
                    if (inVain(i)) {
                        graphics.fill(cellX(x, i) + 1, cellY(y, i) + 1, cellX(x, i) + 17, cellY(y, i) + 17,
                                GuiTheme.IN_VAIN_WASH);
                    }
                } else {
                    GuiSprites.disabledSlot(graphics, cellX(x, i), cellY(y, i));
                }
            }
        }

        @Override
        protected void renderContent(GuiGraphicsExtractor graphics, Font font, int x, int y,
                                     int mouseX, int mouseY) {
            for (int i = stats().augmentSlots(); i < shown(); i++) {
                if (slotStack(PortableBeaconItem.augmentSlot(i)).isEmpty()) {
                    GuiSprites.smallLock(graphics, cellX(x, i), cellY(y, i));
                }
            }
        }

        @Override
        protected List<Component> contentTooltip(int x, int y, int mouseX, int mouseY) {
            int index = (mouseY - y) / GuiMetrics.SLOT * PortableBeaconMenu.AUGMENTS_PER_ROW
                    + (mouseX - x) / GuiMetrics.SLOT;
            if (mouseX >= x && mouseY >= y && index < shown() && index >= stats().augmentSlots()
                    && slotStack(PortableBeaconItem.augmentSlot(index)).isEmpty()) {
                return List.of(Component.translatable("portablebeacons.tip.augment_locked"));
            }
            return List.of();
        }

        @Override
        protected void onOpennessChanged(boolean fullyOpen) {
            menu.setAugmentsVisible(fullyOpen && !selectorOpen);
        }
    }

    // ------------------------------------------------------------------ effect picker

    private void openSelector(int row) {
        selectorOpen = true;
        selectorSlot = row;
        scroll = 0;
        highlighted = 0;
        search = "";
        menu.setAugmentsVisible(false);
    }

    private void closeSelector() {
        selectorOpen = false;
    }

    /**
     * FactoryIO's picker: a modal over the whole window, so nothing of the beacon shows through or
     * takes a click. Title and count in the band, a dark search field, a grid aligned to the
     * inventory and drawn whole even when empty, the scrollbar in the last column, help at the
     * bottom. Escape closes it; typing goes to the search.
     */
    private void drawSelector(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiSprites.panel(graphics, 0, 0, imageWidth, imageHeight);
        List<ResourceKey<BeaconEffectDef>> rows = filteredEffects();
        graphics.text(font, Component.translatable("portablebeacons.gui.change_effect"),
                GuiMetrics.MARGIN, GuiMetrics.TITLE_Y, GuiTheme.TEXT, false);
        String count = Component.translatable("portablebeacons.gui.result_count", rows.size()).getString();
        graphics.text(font, count, GuiMetrics.CONTENT_RIGHT - font.width(count), GuiMetrics.TITLE_Y,
                GuiTheme.TEXT_MUTED, false);
        drawSearchField(graphics);

        int cells = cellCount();
        if (cells > 0) {
            highlighted = Mth.clamp(highlighted, 0, cells - 1);
        }
        int tierLevel = tierLevel();
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = (scroll + row) * GRID_COLUMNS + col;
                int itemX = GuiMetrics.column(col) + 1;
                int itemY = GRID_Y + row * GuiMetrics.SLOT + 1;
                if (index >= cells) {
                    GuiSprites.slot(graphics, itemX, itemY);
                    continue;
                }
                if (within(mouseX, mouseY, itemX - 1, itemY - 1, GuiMetrics.SLOT, GuiMetrics.SLOT)) {
                    // Hover drives the same highlight the arrow keys do, so mouse and keyboard
                    // never disagree about what Enter would pick.
                    highlighted = index;
                    graphics.requestCursor(CursorTypes.POINTING_HAND);
                }
                if (index == highlighted) {
                    GuiSprites.selectedSlot(graphics, itemX, itemY);
                } else {
                    GuiSprites.slot(graphics, itemX, itemY);
                }
                if (index < removeCells()) {
                    GuiSprites.icon(graphics, GuiSprites.Icon.CLEAR, itemX + 2, itemY + 2);
                    continue;
                }
                ResourceKey<BeaconEffectDef> key = rows.get(index - removeCells());
                drawEffectIcon(graphics, key, itemX, itemY);
                boolean locked = effectLookup().get(key).map(def -> def.minTier() > tierLevel).orElse(true);
                if (locked) {
                    // Washed and padlocked, not red: a locked effect is progress, not a problem.
                    graphics.fill(itemX, itemY, itemX + 16, itemY + 16, GuiTheme.LOCKED_WASH);
                    GuiSprites.smallLock(graphics, itemX, itemY);
                }
            }
        }
        if (rows.isEmpty()) {
            graphics.centeredText(font, Component.translatable("portablebeacons.gui.no_results"),
                    GuiMetrics.column(GRID_COLUMNS / 2), GRID_Y + 5, GuiTheme.TEXT_MUTED);
        }
        drawScrollbar(graphics, cells);
        graphics.text(font, Component.translatable("portablebeacons.gui.selector_help"),
                GuiMetrics.MARGIN, FOOTER_Y, GuiTheme.TEXT_MUTED, false);
    }

    private void drawSearchField(GuiGraphicsExtractor graphics) {
        GuiSprites.field(graphics, FIELD_X, FIELD_Y, FIELD_W, FIELD_H);
        boolean empty = search.isEmpty();
        String shown = empty ? Component.translatable("portablebeacons.gui.search").getString() : search;
        graphics.text(font, shown, FIELD_X + 4, FIELD_Y + 3,
                empty ? GuiTheme.TAB_MUTED : GuiTheme.TAB_VALUE, false);
        // No caret over the placeholder: it read as a stray character appended to the hint.
        if (!empty && (System.currentTimeMillis() / 500) % 2 == 0) {
            int caret = FIELD_X + 5 + font.width(search);
            graphics.fill(caret, FIELD_Y + 2, caret + 1, FIELD_Y + FIELD_H - 2, GuiTheme.TAB_VALUE);
        }
    }

    /** A sunken track in the ninth column, drawn even when there is nothing to scroll. */
    private void drawScrollbar(GuiGraphicsExtractor graphics, int total) {
        int trackH = GRID_ROWS * GuiMetrics.SLOT;
        GuiSprites.inset(graphics, SCROLLBAR_X, GRID_Y, SCROLLBAR_W, trackH);
        int totalRows = Mth.positiveCeilDiv(total, GRID_COLUMNS);
        if (totalRows <= GRID_ROWS) {
            return;
        }
        int thumbH = Math.max(15, (trackH - 2) * GRID_ROWS / totalRows);
        int thumbY = GRID_Y + 1 + (trackH - 2 - thumbH) * scroll / Math.max(1, totalRows - GRID_ROWS);
        GuiSprites.button(graphics, SCROLLBAR_X + 1, thumbY, SCROLLBAR_W - 2, thumbH, GuiSprites.ButtonState.NORMAL);
    }

    /**
     * One cell before the effects when the row already holds one: "remove", where anyone looking to
     * get rid of an effect would look - beside the effects it could be swapped for. Right-clicking
     * the row's icon does the same, but only the tooltip says so.
     */
    private int removeCells() {
        return selectorSlot < menu.state().effects().size() ? 1 : 0;
    }

    private int cellCount() {
        return removeCells() + filteredEffects().size();
    }

    /** The cell under the mouse, counting the remove cell if there is one, or -1. */
    private int cellAt(int x, int y) {
        if (y < GRID_Y || x < GuiMetrics.column(0)) {
            return -1;
        }
        int col = (x - GuiMetrics.column(0)) / GuiMetrics.SLOT;
        int row = (y - GRID_Y) / GuiMetrics.SLOT;
        if (col >= GRID_COLUMNS || row >= GRID_ROWS) {
            return -1;
        }
        int index = (scroll + row) * GRID_COLUMNS + col;
        return index < cellCount() ? index : -1;
    }

    // ------------------------------------------------------------------ tooltips

    private boolean renderFuelSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!(hoveredSlot instanceof ResourceHandlerSlot handler)
                || handler.getSlotIndex() != PortableBeaconItem.FUEL_SLOT
                || !hoveredSlot.hasItem()) {
            return false;
        }
        ItemStack stack = hoveredSlot.getItem();
        int perItem = BPLookups.fuelValue(Minecraft.getInstance().level.registryAccess(), stack.getItem());
        if (perItem <= 0) {
            return false;
        }
        List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
        BeaconStats stats = stats();
        double perSecond = BeaconResolver.fuelPerSecond(menu.state(), stats, effectLookup());
        if (perSecond > 0.0) {
            lines.add(Component.translatable("portablebeacons.tip.fuel_worth",
                            Durations.format((int) (perItem / perSecond)),
                            Durations.format((int) (perItem * stack.getCount() / perSecond)))
                    .withStyle(ChatFormatting.GRAY));
        }
        // A denser fuel than the buffer can hold is never consumed, and that would otherwise look
        // like the beacon ignoring it for no reason. Red: it will not work without the player.
        if (perItem > stats.fuelCapacity()) {
            lines.add(Component.translatable("portablebeacons.tip.fuel_too_dense")
                    .withStyle(ChatFormatting.RED));
        }
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    /** An augment's own tooltip, plus a warning when part of it does nothing on this beacon. */
    private boolean renderAugmentSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!(hoveredSlot instanceof ResourceHandlerSlot handler)
                || handler.getSlotIndex() == PortableBeaconItem.FUEL_SLOT
                || !hoveredSlot.hasItem()
                || !inVain(handler.getSlotIndex() - 1)) {
            return false;
        }
        List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(hoveredSlot.getItem()));
        lines.add(Component.translatable("portablebeacons.tip.augment_in_vain").withStyle(ChatFormatting.YELLOW));
        graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
        return true;
    }

    /**
     * Whether the augment in augment slot {@code n} raises a ceiling this beacon is already at.
     * Per frame, like everything else here: eight resolves of a handful of numbers.
     */
    private boolean inVain(int n) {
        BeaconTierDef tier = menu.tierDef();
        ItemStack stack = slotStack(PortableBeaconItem.augmentSlot(n));
        if (tier == null || AugmentItem.instanceOf(stack) == null) {
            return false;
        }
        List<AugmentInstance> installed = new ArrayList<>();
        int index = -1;
        for (int i = 0; i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
            AugmentInstance instance = AugmentItem.instanceOf(slotStack(PortableBeaconItem.augmentSlot(i)));
            if (instance != null) {
                if (i == n) {
                    index = installed.size();
                }
                installed.add(instance);
            }
        }
        var access = Minecraft.getInstance().level.registryAccess();
        Map<ResourceKey<BeaconEffectDef>, BeaconEffectDef> effects = new HashMap<>();
        access.lookupOrThrow(BPRegistryKeys.EFFECT).listElements()
                .forEach(holder -> effects.put(holder.key(), holder.value()));
        return BeaconResolver.raisesACeilingInVain(tier, installed, index, BPLookups.augments(access), effects);
    }

    /** The effect under the mouse: its name, and its relative cost or what unlocks it. */
    private List<Component> selectorTooltip(int mouseX, int mouseY) {
        int index = cellAt(mouseX - leftPos, mouseY - topPos);
        if (index < 0) {
            return List.of();
        }
        if (index < removeCells()) {
            return List.of(Component.translatable("portablebeacons.tip.clear_effect"));
        }
        return effectLookup().get(filteredEffects().get(index - removeCells())).<List<Component>>map(def -> List.of(
                def.effect().value().getDisplayName(),
                def.minTier() > tierLevel()
                        ? Component.translatable("portablebeacons.gui.locked_tier", roman(def.minTier()))
                                .withStyle(ChatFormatting.GRAY)
                        : Component.translatable("portablebeacons.tip.cost_meter",
                                        Component.translatable("portablebeacons.cost." + costBand(def)))
                                .withStyle(ChatFormatting.GRAY))).orElse(List.of());
    }

    private String costBand(BeaconEffectDef def) {
        double max = filteredEffects().stream()
                .map(key -> effectLookup().get(key).map(BeaconEffectDef::cost).orElse(0.0))
                .max(Double::compare).orElse(1.0);
        return switch (Mth.clamp((int) Math.ceil(def.cost() / max * 4), 1, 4)) {
            case 1 -> "very_low";
            case 2 -> "low";
            case 3 -> "moderate";
            default -> "high";
        };
    }

    /** Nothing on this screen is self-explanatory without these. Absolute mouse coordinates. */
    private List<Component> tooltipAt(int mouseX, int mouseY) {
        List<Component> tabTip = tabs.tooltipAt(mouseX, mouseY);
        if (!tabTip.isEmpty()) {
            return tabTip;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (within(x, y, POWER_X, POWER_Y, POWER_W, POWER_H)) {
            return List.of(Component.translatable(menu.state().active()
                            ? "portablebeacons.gui.active" : "portablebeacons.gui.inactive"),
                    Component.translatable("portablebeacons.tip.master").withStyle(ChatFormatting.GRAY));
        }
        if (within(x, y, GuiMetrics.LED_X - 1, GuiMetrics.LED_Y - 1, GuiMetrics.LED_SIZE + 2, GuiMetrics.LED_SIZE + 2)) {
            return List.of(statusText(status()));
        }
        if (BPConfig.fuelEnabled()
                && within(x, y, GuiMetrics.GAUGE_X, GAUGE_Y, GuiMetrics.GAUGE_WIDTH, GAUGE_H)) {
            double perSecond = BeaconResolver.fuelPerSecond(menu.state(), stats(), effectLookup());
            return List.of(Component.translatable("portablebeacons.gui.fuel"),
                    Component.translatable("portablebeacons.tip.fuel_stored",
                            atCurrentDraw(menu.state().fuel(), perSecond)).withStyle(ChatFormatting.GRAY),
                    Component.translatable("portablebeacons.tip.fuel_reserve",
                            atCurrentDraw(reserveUnits(), perSecond)).withStyle(ChatFormatting.GRAY));
        }
        int row = rowAt(x, y);
        return row < 0 ? List.of() : rowTooltip(row, partAt(x));
    }

    private List<Component> rowTooltip(int row, Part part) {
        BeaconStats stats = stats();
        if (row >= stats.effectSlots()) {
            return List.of(Component.translatable("portablebeacons.tip.case_locked"));
        }
        List<EffectSlotConfig> effects = menu.state().effects();
        if (row >= effects.size()) {
            return List.of(Component.translatable("portablebeacons.gui.empty_slot"));
        }
        EffectSlotConfig slot = effects.get(row);
        Optional<BeaconEffectDef> maybeDef = effectLookup().get(slot.effect());
        if (maybeDef.isEmpty()) {
            return List.of();
        }
        BeaconEffectDef def = maybeDef.get();
        Component name = Component.empty().append(def.effect().value().getDisplayName())
                .append(" " + roman(slot.amplifier() + 1));
        return switch (part) {
            case ICON -> List.of(name,
                    Component.translatable("portablebeacons.tip.case_clear").withStyle(ChatFormatting.GRAY));
            case LEVEL -> List.of(Component.translatable("portablebeacons.tip.level"),
                    Component.translatable("portablebeacons.tip.level_click").withStyle(ChatFormatting.GRAY));
            case AURA -> List.of(
                    Component.translatable("portablebeacons.aura." + slot.aura().getSerializedName()),
                    Component.translatable("portablebeacons.tip.aura").withStyle(ChatFormatting.GRAY));
            case SWITCH -> List.of(Component.translatable(slot.enabled()
                            ? "portablebeacons.gui.active" : "portablebeacons.gui.inactive"),
                    Component.translatable("portablebeacons.tip.effect_toggle").withStyle(ChatFormatting.GRAY));
            case NAME, NONE -> List.of(name, drainLine(row, slot, stats), reachLine(slot, stats));
        };
    }

    /**
     * This effect's share of the total drain, rather than a raw rate: it answers "which of my
     * effects is draining the beacon" without asking the player to compare two decimals. A free slot
     * covers this one: say so rather than printing a share of a total it is not in.
     */
    private Component drainLine(int row, EffectSlotConfig slot, BeaconStats stats) {
        List<EffectSlotConfig> effects = menu.state().effects();
        boolean[] free = BeaconResolver.freeMask(effects, stats, effectLookup());
        if (row < free.length && free[row]) {
            return Component.translatable("portablebeacons.gui.share_free").withStyle(ChatFormatting.GRAY);
        }
        int share = (int) Math.round(BeaconResolver.share(effects, row, stats, effectLookup()) * 100.0);
        return Component.translatable("portablebeacons.gui.share", share).withStyle(ChatFormatting.GRAY);
    }

    private static Component reachLine(EffectSlotConfig slot, BeaconStats stats) {
        Component reach = slot.aura().isAura()
                ? Component.literal(String.format(Locale.ROOT, "%.0f m", stats.range()))
                : Component.translatable("portablebeacons.aura.self");
        return Component.translatable("portablebeacons.gui.reach", reach).withStyle(ChatFormatting.GRAY);
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (selectorOpen) {
            confirm(cellAt(x, y));
            return true;
        }
        if (tabs.mouseClicked(event.x(), event.y())) {
            click();
            return true;
        }
        if (within(x, y, POWER_X, POWER_Y, POWER_W, POWER_H)) {
            send(PortableBeaconMenu.ACTION_TOGGLE_ACTIVE, 0, 0);
            return true;
        }
        int row = rowAt(x, y);
        if (row >= 0) {
            handleRowClick(row, partAt(x), event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /**
     * Every control lives in its row. Left click does the obvious thing; right click undoes it -
     * removes the effect from the icon, lowers the level - which is what the tooltips say.
     */
    private void handleRowClick(int row, Part part, boolean secondary) {
        if (row >= stats().effectSlots()) {
            return;
        }
        List<EffectSlotConfig> effects = menu.state().effects();
        if (row >= effects.size()) {
            // An empty row has one thing to do, so any click on it does it.
            click();
            openSelector(row);
            return;
        }
        EffectSlotConfig slot = effects.get(row);
        switch (part) {
            case ICON, NAME, NONE -> {
                if (secondary && part == Part.ICON) {
                    send(PortableBeaconMenu.ACTION_CLEAR_EFFECT, row, 0);
                } else if (!secondary) {
                    click();
                    openSelector(row);
                }
            }
            case LEVEL -> {
                // Round and round, I to the ceiling and back to I: one button that always does
                // something, the way a level selector in any game behaves. Right click steps back.
                int cap = effectLookup().get(slot.effect())
                        .map(def -> Math.min(def.maxAmplifier(), stats().maxAmplifierFor(slot.effect()))).orElse(0);
                if (cap > 0) {
                    int step = secondary ? cap : 1;
                    send(PortableBeaconMenu.ACTION_SET_AMPLIFIER, row, (slot.amplifier() + step) % (cap + 1));
                }
            }
            case AURA -> send(PortableBeaconMenu.ACTION_CYCLE_AURA, row, 0);
            case SWITCH -> send(PortableBeaconMenu.ACTION_TOGGLE_EFFECT, row, 0);
        }
    }

    /** A click on a tab is not a click outside the window, which would drop the carried stack. */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return !tabs.contains(mouseX, mouseY) && super.hasClickedOutside(mouseX, mouseY, left, top);
    }

    private void confirm(int index) {
        if (index < 0 || index >= cellCount()) {
            return;
        }
        if (index < removeCells()) {
            send(PortableBeaconMenu.ACTION_CLEAR_EFFECT, selectorSlot, 0);
            closeSelector();
            return;
        }
        ResourceKey<BeaconEffectDef> key = filteredEffects().get(index - removeCells());
        if (effectLookup().get(key).map(def -> def.minTier() > tierLevel()).orElse(true)) {
            return;
        }
        send(PortableBeaconMenu.ACTION_SET_EFFECT, selectorSlot, allKeys().indexOf(key));
        closeSelector();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (selectorOpen) {
            scroll = Mth.clamp(scroll - (int) Math.signum(deltaY), 0, maxScroll());
            return true;
        }
        if (rowAt((int) mouseX - leftPos, (int) mouseY - topPos) >= 0) {
            int total = visibleRows(stats());
            rowScroll = Mth.clamp(rowScroll - (int) Math.signum(deltaY), 0,
                    Math.max(0, total - PortableBeaconMenu.VISIBLE_EFFECT_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (selectorOpen && search.length() < 24) {
            search += event.codepointAsString();
            scroll = 0;
            highlighted = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!selectorOpen) {
            return super.keyPressed(event);
        }
        switch (event.key()) {
            case GLFW.GLFW_KEY_ESCAPE -> closeSelector();
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                    scroll = 0;
                    highlighted = 0;
                }
            }
            case GLFW.GLFW_KEY_RIGHT -> moveHighlight(1);
            case GLFW.GLFW_KEY_LEFT -> moveHighlight(-1);
            case GLFW.GLFW_KEY_DOWN -> moveHighlight(GRID_COLUMNS);
            case GLFW.GLFW_KEY_UP -> moveHighlight(-GRID_COLUMNS);
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> confirm(highlighted);
            default -> {
                // Everything else is swallowed so the inventory key - "E" - is typed into the
                // search instead of closing the whole screen.
            }
        }
        return true;
    }

    /** Keeps the highlighted cell's row on screen, which is what makes arrow keys usable at all. */
    private void moveHighlight(int delta) {
        int size = cellCount();
        if (size == 0) {
            return;
        }
        highlighted = Mth.clamp(highlighted + delta, 0, size - 1);
        int row = highlighted / GRID_COLUMNS;
        if (row < scroll) {
            scroll = row;
        } else if (row >= scroll + GRID_ROWS) {
            scroll = row - GRID_ROWS + 1;
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
    }

    private int maxScroll() {
        return Math.max(0, Mth.positiveCeilDiv(cellCount(), GRID_COLUMNS) - GRID_ROWS);
    }

    // ------------------------------------------------------------------ helpers

    private void send(int action, int slot, int value) {
        click();
        ClientPacketDistributor.sendToServer(new BeaconActionPayload(action, slot, value));
    }

    /**
     * The click every vanilla button makes. Hand-drawn controls get no audio for free, and a control
     * that changes but makes no sound reads as not having registered the press.
     */
    private static void click() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private BeaconResolver.Lookup<BeaconEffectDef> effectLookup() {
        return BPLookups.effects(Minecraft.getInstance().level.registryAccess());
    }

    /**
     * The whole registry, in the order both sides agree on.
     *
     * <p>This is what the wire index refers to, so it must never be filtered - the server resolves
     * the index against the same unfiltered list. Cached because the registry cannot change while
     * the screen is open, and building it means a stream and a sort.
     */
    private List<ResourceKey<BeaconEffectDef>> allKeys() {
        if (allKeysCache == null) {
            allKeysCache = BPLookups.sortedEffectKeys(Minecraft.getInstance().level.registryAccess());
        }
        return allKeysCache;
    }

    /**
     * The effects the picker shows: this beacon's pool, narrowed by the search box.
     *
     * <p>A themed beacon listing the standard effects it will never accept would be a grid of dead
     * ends, so the pool filters the picker rather than greying cells out. Locked entries are kept,
     * though - those are progress, not dead ends. Recomputed only when the search text changes.
     */
    private List<ResourceKey<BeaconEffectDef>> filteredEffects() {
        if (rowsCache != null && search.equals(rowsCacheKey)) {
            return rowsCache;
        }
        BeaconTierDef tier = menu.tierDef();
        String needle = search.toLowerCase(Locale.ROOT);
        rowsCache = allKeys().stream()
                .filter(key -> tier == null || tier.allows(key, effectLookup().get(key).orElse(null)))
                .filter(key -> needle.isEmpty() || effectLookup().get(key)
                        .map(def -> def.effect().value().getDisplayName().getString()
                                .toLowerCase(Locale.ROOT).contains(needle))
                        .orElse(false))
                .toList();
        rowsCacheKey = search;
        return rowsCache;
    }

    private double fuelFill() {
        return Math.min(1.0, menu.state().fuel() / (double) Math.max(1, stats().fuelCapacity()));
    }

    private boolean burnsNothing() {
        return stats().fuelMultiplier() <= 0.0;
    }

    private String totalRuntime() {
        double perSecond = BeaconResolver.fuelPerSecond(menu.state(), stats(), effectLookup());
        return atCurrentDraw(menu.state().fuel() + reserveUnits(), perSecond);
    }

    /**
     * "Idle" rather than a dash when nothing is drawing: a lone "-" reads as missing data, naming
     * the state says the beacon is fine and simply has nothing running.
     */
    private static String atCurrentDraw(int units, double perSecond) {
        return perSecond <= 0.0
                ? Component.translatable("portablebeacons.gui.idle").getString()
                : Durations.format((int) (units / perSecond));
    }

    /** Fuel units still sitting in the fuel slot, not yet drawn into the buffer. */
    private int reserveUnits() {
        return BPLookups.reserveUnits(menu.beacon(), Minecraft.getInstance().level.registryAccess());
    }

    private ItemStack slotStack(int handlerIndex) {
        for (Slot slot : menu.slots) {
            if (slot instanceof ResourceHandlerSlot handler && handler.getSlotIndex() == handlerIndex) {
                return slot.getItem();
            }
        }
        return ItemStack.EMPTY;
    }

    private int tierLevel() {
        BeaconTierDef tier = menu.tierDef();
        return tier == null ? 1 : tier.level();
    }

    private static boolean canAmplify(BeaconEffectDef def, BeaconStats stats, ResourceKey<BeaconEffectDef> key) {
        return Math.min(def.maxAmplifier(), stats.maxAmplifierFor(key)) > 0;
    }

    private static boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** Levels as the game writes them, I to X; past that, digits - nothing reaches it today. */
    static String roman(int value) {
        String[] numerals = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return value >= 1 && value <= numerals.length ? numerals[value - 1] : String.valueOf(value);
    }
}
