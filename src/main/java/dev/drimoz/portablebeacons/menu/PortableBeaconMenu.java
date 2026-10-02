package dev.drimoz.portablebeacons.menu;

import dev.drimoz.portablebeacons.ActionBar;
import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.BeaconProximity;
import dev.drimoz.portablebeacons.compat.CuriosCompat;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.gui.GuiMetrics;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.registry.BPLookups;
import dev.drimoz.portablebeacons.registry.BPMenus;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The beacon's container menu.
 *
 * <p>Effect slots are deliberately not {@link Slot}s: they hold data, not stacks, so they are drawn
 * by the screen and mutated through {@link #applyAction}. Only augments and fuel are real slots.
 */
public class PortableBeaconMenu extends AbstractContainerMenu {

    public static final int ACTION_TOGGLE_ACTIVE = 0;
    public static final int ACTION_SET_EFFECT = 1;
    public static final int ACTION_CLEAR_EFFECT = 2;
    // 3 was ACTION_CYCLE_AMPLIFIER, replaced by ACTION_SET_AMPLIFIER; left unused rather than reused,
    // so a mismatched client sending it is refused instead of doing something else.
    public static final int ACTION_TOGGLE_EFFECT = 4;
    public static final int ACTION_CYCLE_AURA = 5;
    /** The level outright, so the screen can step down as well as up; sanitize caps it. */
    public static final int ACTION_SET_AMPLIFIER = 6;

    /**
     * Bottom of the beacon's own content - sockets, details and the fuel column - and so where the
     * player inventory starts. Shared with the screen, which draws to the same line.
     */
    /**
     * Effect rows shown at once. A beacon may have more - up to {@link BeaconStats#MAX_EFFECT_SLOTS}
     * - and the table scrolls; five keeps the window the size it was.
     */
    public static final int VISIBLE_EFFECT_ROWS = 5;

    /** The effect rows, each one inventory slot tall, under the band. */
    public static final int CONTENT_BOTTOM = GuiMetrics.CONTENT_TOP + VISIBLE_EFFECT_ROWS * GuiMetrics.SLOT;

    /**
     * The fuel slot sits in column 0 under the gauge, FactoryIO's burner layout: the fuel is what
     * keeps the beacon alive, so it is on the window rather than in a drawer.
     */
    public static final int FUEL_SLOT_X = GuiMetrics.column(0) + 1;
    public static final int FUEL_SLOT_Y = CONTENT_BOTTOM - GuiMetrics.SLOT + 1;

    /**
     * Augments live in the first right-hand tab. Their positions are where that tab's content is
     * once it is fully open: {@code Slot.x} and {@code y} are final, so a shut tab hides its slots
     * from rendering and hit-testing rather than moving them.
     */
    public static final int AUGMENT_SLOT_Y = GuiMetrics.FIRST_TAB_CONTENT_Y + 1;

    /** Augment slots per row of the tab; past four the slots wrap onto a second row. */
    public static final int AUGMENTS_PER_ROW = 4;

    public static int augmentSlotX(int n) {
        return GuiMetrics.RIGHT_TAB_CONTENT_X + 1 + (n % AUGMENTS_PER_ROW) * GuiMetrics.SLOT;
    }

    public static int augmentSlotY(int n) {
        return AUGMENT_SLOT_Y + (n / AUGMENTS_PER_ROW) * GuiMetrics.SLOT;
    }

    /**
     * Rows of augment slots worth showing: enough for what the beacon unlocks, and for any augment
     * still sitting in a slot it no longer does. The shipped tiers fit one row; a datapack tier with
     * more slots gets a second, and nobody else pays for it with a row of hatched boxes.
     */
    public int augmentRows() {
        int needed = Math.max(1, stats().augmentSlots());
        IItemHandler handler = BPLookups.handlerOf(beacon());
        if (handler != null) {
            for (int i = 0; i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
                int index = PortableBeaconItem.augmentSlot(i);
                if (index < handler.getSlots() && !handler.getStackInSlot(index).isEmpty()) {
                    needed = Math.max(needed, i + 1);
                }
            }
        }
        return Math.min(Mth.positiveCeilDiv(PortableBeaconItem.AUGMENT_SLOTS, AUGMENTS_PER_ROW),
                Mth.positiveCeilDiv(needed, AUGMENTS_PER_ROW));
    }

    /**
     * Stands in for "the beacon worn as a curio" where an inventory slot index is expected.
     *
     * <p>Negative so it can never collide with a real index. The beacon's own inventory slot is
     * frozen while its menu is open, to stop the stack being moved out from under it; a worn beacon
     * needs no such treatment, because it is not one of the slots this menu draws.
     */
    public static final int CURIO_SLOT = -1;

    private final Player player;
    private final int beaconSlotIndex;
    /**
     * Whether the augment tab is fully open on the client. Always true on the server, where nothing is
     * hidden.
     *
     * <p>Vanilla skips both rendering and hover for a slot whose {@link Slot#isActive()} is false,
     * which is the only supported way to hide one - the screen's {@code isHovering(Slot, ...)}
     * overload is private.
     */
    private boolean augmentsVisible = true;
    /** Only used to back the augment and fuel slots; never read for state - see {@link #beacon()}. */
    private final ItemStack slotBackingStack;
    /** Varies with the fuel config, so shift-clicking cannot assume a fixed boundary. */
    private final int beaconSlotCount;

    public PortableBeaconMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, buf.readVarInt());
    }

    public PortableBeaconMenu(int containerId, Inventory playerInventory, int beaconSlotIndex) {
        super(BPMenus.BEACON.get(), containerId);
        this.player = playerInventory.player;
        this.beaconSlotIndex = beaconSlotIndex;
        this.slotBackingStack = beaconSlotIndex == CURIO_SLOT
                ? CuriosCompat.findBeacon(playerInventory.player)
                : playerInventory.getItem(beaconSlotIndex);

        IItemHandler handler = new LiveBeaconHandler();
        for (int i = 0; i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
            addSlot(new AugmentSlot(handler, PortableBeaconItem.augmentSlot(i), augmentSlotX(i), augmentSlotY(i)));
        }
        // No fuel slot at all when fuel is switched off, rather than a slot that refuses
        // everything. Both sides read the same synced config, so the slot counts agree.
        if (BPConfig.fuelEnabled()) {
            addSlot(new FuelSlot(handler, PortableBeaconItem.FUEL_SLOT, FUEL_SLOT_X, FUEL_SLOT_Y));
        }
        this.beaconSlotCount = slots.size();

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                addSlot(playerSlot(playerInventory, index,
                        GuiMetrics.column(col) + 1, GuiMetrics.inventoryY(CONTENT_BOTTOM) + row * GuiMetrics.SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(playerSlot(playerInventory, col, GuiMetrics.column(col) + 1,
                    GuiMetrics.hotbarY(CONTENT_BOTTOM)));
        }
    }

    /**
     * The beacon's own slot is present but frozen. Without this the player could move or drop the
     * stack the open menu is writing into, which duplicates its contents.
     */
    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        if (index == beaconSlotIndex) {
            return new Slot(inventory, index, x, y) {
                @Override
                public boolean mayPickup(Player who) {
                    return false;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            };
        }
        return new Slot(inventory, index, x, y);
    }

    /**
     * Resolved from the inventory on every call rather than cached.
     *
     * <p>Syncing a slot replaces the client's {@link ItemStack} instance, so a cached reference goes
     * stale the moment the server answers - which shows up as a screen that only refreshes when it
     * is closed and reopened.
     */
    public ItemStack beacon() {
        return beaconSlotIndex == CURIO_SLOT
                ? CuriosCompat.findBeacon(player)
                : player.getInventory().getItem(beaconSlotIndex);
    }

    public BeaconState state() {
        return PortableBeaconItem.stateOf(beacon());
    }

    /**
     * Resolved stats. Both sides compute this from the same synced registries and the same synced
     * stack, so the figure the GUI prints is the figure the ticker charges.
     */
    public BeaconStats stats() {
        BeaconStats stats = BPLookups.stats(beacon(), player.level().registryAccess());
        return stats != null ? stats : new BeaconStats(0, 0, 0.0, 0, 0, 1.0, 1.0, 0, 1.0, 1.0,
                java.util.EnumSet.of(AuraMode.SELF), false, false);
    }

    public BeaconTierDef tierDef() {
        if (!(beacon().getItem() instanceof PortableBeaconItem item)) {
            return null;
        }
        return BPLookups.tier(player.level().registryAccess(), item);
    }

    @Override
    public boolean stillValid(Player who) {
        // Compared against the instance captured at open time: a same-looking beacon swapped into the
        // slot is not this beacon. The server never replaces the instance, it mutates it in place.
        if (who != player) {
            return false;
        }
        if (beaconSlotIndex == CURIO_SLOT) {
            // "Still wearing a beacon", not "still wearing this exact instance". The inventory case
            // compares instances to stop the stack being moved out from under an open menu, but a
            // worn beacon is not one of the slots this menu draws, so there is no such move to guard
            // against - and Curios is free to hand back a different instance for the same slot.
            return !CuriosCompat.findBeacon(who).isEmpty();
        }
        return beaconSlotIndex < who.getInventory().getContainerSize()
                && who.getInventory().getItem(beaconSlotIndex) == slotBackingStack;
    }

    /**
     * Applies one configuration change. Called from {@code BeaconActionPayload} rather than
     * {@code clickMenuButton}, whose id is a single byte on the wire.
     */
    public boolean applyAction(int action, int slotIndex, int value) {
        Player who = player;
        // Both arrive from the client as var-ints, which carry negatives perfectly well. Every
        // guard below this point is an upper bound - `slotIndex < effects.size()` and friends - so
        // a negative index sailed past all of them and reached List.set/remove, throwing on the
        // server thread from a packet any modified client can send at will.
        if (slotIndex < 0 || value < 0) {
            return false;
        }
        BeaconStats stats = stats();
        BeaconTierDef tier = tierDef();
        if (tier == null) {
            return false;
        }
        if (isReconfiguration(action) && !canReconfigure()) {
            who.displayClientMessage(
                    Component.translatable("portablebeacons.msg.needs_beacon"), true);
            return false;
        }
        BeaconResolver.Lookup<BeaconEffectDef> lookup =
                BPLookups.effects(who.level().registryAccess());

        List<EffectSlotConfig> effects = new ArrayList<>(state().effects());
        BeaconState updated = switch (action) {
            case ACTION_TOGGLE_ACTIVE -> state().withActive(!state().active());
            case ACTION_SET_EFFECT -> state().withEffects(
                    setEffect(effects, slotIndex, value, stats, tier, lookup, who.level()
                            .registryAccess()));
            case ACTION_CLEAR_EFFECT -> {
                if (slotIndex < effects.size()) {
                    effects.remove(slotIndex);
                }
                yield state().withEffects(effects);
            }
            case ACTION_TOGGLE_EFFECT -> state().withEffects(
                    mutate(effects, slotIndex, slot -> slot.withEnabled(!slot.enabled())));
            case ACTION_CYCLE_AURA -> state().withEffects(
                    mutate(effects, slotIndex, slot -> slot.withAura(nextAura(slot.aura(), stats))));
            case ACTION_SET_AMPLIFIER -> state().withEffects(
                    mutate(effects, slotIndex, slot -> slot.withAmplifier(Math.min(value, BeaconStats.MAX_AMPLIFIER))));
            default -> null;
        };

        if (updated == null) {
            return false;
        }
        PortableBeaconItem.setState(beacon(), BeaconResolver.sanitize(updated, stats, lookup, tier));
        if (action == ACTION_TOGGLE_ACTIVE) {
            // Audible confirmation, because the only other signal is a small label changing colour.
            who.level().playSound(null, who.blockPosition(),
                    state().active() ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.PLAYERS, 0.4F, 1.0F);
            if (state().active() && switchOffOtherBeacons()) {
                ActionBar.send(who, Component.translatable("portablebeacons.msg.others_off"));
            }
        }
        broadcastChanges();
        return true;
    }

    /**
     * Only one beacon runs at a time, so switching one on switches the others off - the way radio
     * buttons behave. Two used to stay "on", glinting, while only the first did anything, and
     * nothing on screen said which.
     *
     * @return whether any other beacon was on
     */
    private boolean switchOffOtherBeacons() {
        ItemStack mine = beacon();
        boolean any = false;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            any |= switchOff(inventory.getItem(i), mine);
        }
        // Once, not until none is left: were Curios to hand back a copy, the copy would be switched
        // off, the original would stay on, and a loop would never end.
        any |= switchOff(CuriosCompat.findActiveBeacon(player), mine);
        return any;
    }

    private static boolean switchOff(ItemStack stack, ItemStack mine) {
        if (stack == mine || !(stack.getItem() instanceof PortableBeaconItem)
                || !PortableBeaconItem.stateOf(stack).active()) {
            return false;
        }
        PortableBeaconItem.setState(stack, PortableBeaconItem.stateOf(stack).withActive(false));
        return true;
    }

    /**
     * Turning the beacon or a single effect off must always work - a player caught out in the field
     * has to be able to stop the drain. Only changes to what the beacon projects are gated.
     */
    private static boolean isReconfiguration(int action) {
        return action == ACTION_SET_EFFECT
                || action == ACTION_CLEAR_EFFECT
                || action == ACTION_SET_AMPLIFIER
                || action == ACTION_CYCLE_AURA;
    }

    private boolean canReconfigure() {
        return !BPConfig.INSTANCE.requireBeaconToConfigure.get()
                || BeaconProximity.activeBeaconNear(player);
    }

    private List<EffectSlotConfig> setEffect(List<EffectSlotConfig> effects, int slotIndex,
                                             int effectIndex, BeaconStats stats, BeaconTierDef tier,
                                             BeaconResolver.Lookup<BeaconEffectDef> lookup,
                                             RegistryAccess access) {
        if (slotIndex >= stats.effectSlots()) {
            return effects;
        }
        List<ResourceKey<BeaconEffectDef>> keys = BPLookups.sortedEffectKeys(access);
        if (effectIndex >= keys.size()) {
            return effects;
        }
        ResourceKey<BeaconEffectDef> key = keys.get(effectIndex);

        Optional<BeaconEffectDef> def = lookup.get(key);
        // Every one of these is re-checked here even though the GUI greys them out: the button id
        // arrives from the client and cannot be trusted on its own.
        if (def.isEmpty() || def.get().minTier() > tier.level() || !tier.allows(key, def.get())) {
            return effects;
        }
        if (effects.stream().anyMatch(slot -> slot.effect().equals(key))) {
            return effects;
        }

        EffectSlotConfig created = new EffectSlotConfig(key, 0, true, AuraMode.SELF);
        if (slotIndex < effects.size()) {
            effects.set(slotIndex, created);
        } else {
            effects.add(created);
        }
        return effects;
    }

    private static List<EffectSlotConfig> mutate(List<EffectSlotConfig> effects, int slotIndex,
                                                 java.util.function.UnaryOperator<EffectSlotConfig> op) {
        if (slotIndex < effects.size()) {
            effects.set(slotIndex, op.apply(effects.get(slotIndex)));
        }
        return effects;
    }

    private static AuraMode nextAura(AuraMode current, BeaconStats stats) {
        AuraMode[] modes = AuraMode.values();
        for (int step = 1; step <= modes.length; step++) {
            AuraMode candidate = modes[(current.ordinal() + step) % modes.length];
            if (stats.allows(candidate)) {
                return candidate;
            }
        }
        return AuraMode.SELF;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int packSlots = beaconSlotCount;

        if (index < packSlots) {
            if (!moveItemStackTo(stack, packSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, packSlots, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    /**
     * Resolves the beacon's inventory on every call instead of holding a handler bound to one
     * {@link ItemStack}.
     *
     * <p>Any change to the beacon makes the server resend its slot, which replaces the client's stack
     * instance - leaving a bound handler writing into an orphaned copy. The augment and fuel slots
     * would then show contents that no longer exist.
     */
    private class LiveBeaconHandler implements IItemHandlerModifiable {

        /**
         * Modifiable, not just {@link IItemHandler}: {@code SlotItemHandler#set} casts to
         * {@link IItemHandlerModifiable} without checking, which is how the client applies the
         * server's container contents.
         */
        private IItemHandlerModifiable delegate() {
            IItemHandler handler = beacon().getCapability(Capabilities.ItemHandler.ITEM);
            return handler instanceof IItemHandlerModifiable modifiable ? modifiable : null;
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = delegate();
            if (handler != null) {
                handler.setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? PortableBeaconItem.CONTAINER_SIZE : handler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? stack : handler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? ItemStack.EMPTY : handler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandlerModifiable handler = delegate();
            return handler == null ? 64 : handler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandlerModifiable handler = delegate();
            return handler == null || handler.isItemValid(slot, stack);
        }
    }

    public void setAugmentsVisible(boolean visible) {
        this.augmentsVisible = visible;
    }

    /** Rejects a second augment of a type already installed, so the rule is visible, not hidden. */
    private class AugmentSlot extends SlotItemHandler {
        AugmentSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            // A slot in a row the tab is not showing is hidden like a shut tab's: no render, no hover.
            return augmentsVisible && (getSlotIndex() - 1) / AUGMENTS_PER_ROW < augmentRows();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            var instance = AugmentItem.instanceOf(stack);
            if (instance == null) {
                return false;
            }
            // getSlotIndex() is a container index; augmentSlots() is a count. Fuel occupies index 0.
            if (getSlotIndex() - 1 >= stats().augmentSlots()) {
                return false;
            }
            // Against the other slots only. Counting this one refused a Range III dropped onto the
            // Range II it was meant to replace, so upgrading an augment took three clicks instead
            // of the swap every other slot in the game does.
            IItemHandler handler = BPLookups.handlerOf(beacon());
            if (handler == null) {
                return false;
            }
            for (int i = 0; i < PortableBeaconItem.AUGMENT_SLOTS; i++) {
                int index = PortableBeaconItem.augmentSlot(i);
                if (index == getSlotIndex() || index >= handler.getSlots()) {
                    continue;
                }
                AugmentInstance other = AugmentItem.instanceOf(handler.getStackInSlot(index));
                if (other != null && other.type().equals(instance.type())) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Fitting or pulling an augment is the one action in this screen with no feedback at all -
         * every button clicks, but a slot is silent, and slotting an augment changes what the beacon
         * does more than any button does.
         *
         * <p>Only on a real change of occupancy: {@code set} also runs while the menu syncs, and a
         * chime on every sync would fire while nothing happened.
         */
        @Override
        public void set(ItemStack stack) {
            boolean wasEmpty = getItem().isEmpty();
            super.set(stack);
            if (!player.level().isClientSide() && wasEmpty != stack.isEmpty()) {
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        0.6F, stack.isEmpty() ? 0.8F : 1.2F);
            }
        }
    }

    private class FuelSlot extends SlotItemHandler {
        FuelSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return BPLookups.fuelValue(player.level().registryAccess(), stack.getItem()) > 0;
        }
    }
}
