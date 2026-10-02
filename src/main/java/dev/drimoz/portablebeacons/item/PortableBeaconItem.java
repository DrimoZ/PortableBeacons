package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.EffectSlotConfig;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.menu.BeaconMenuOpener;
import dev.drimoz.portablebeacons.registry.BPLookups;
import dev.drimoz.portablebeacons.registry.BPComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.Optional;

/** The portable beacon itself. One class, four registered instances, one per tier. */
public class PortableBeaconItem extends Item {

    /**
     * Slot 0 holds fuel; augments follow it. Slots above the tier's count stay locked.
     *
     * <p>Fuel comes first so that raising {@link #AUGMENT_SLOTS} only ever appends. It used to sit
     * after the augments, which meant adding a slot shifted its index and turned the fuel in every
     * saved beacon into an augment. The layout a saved item is written against has to be stable
     * under exactly the change this mod is most likely to make again.
     *
     * <p>The tier's {@code augment_slots} is a separate thing entirely — it says how many of these
     * are <em>usable</em>, and a datapack may move it freely without touching anyone's contents.
     */
    public static final int FUEL_SLOT = 0;
    public static final int AUGMENT_SLOTS = 4;
    public static final int CONTAINER_SIZE = AUGMENT_SLOTS + 1;

    /** Container index of the nth augment slot, counting from zero. */
    public static int augmentSlot(int n) {
        return n + 1;
    }

    private final ResourceKey<BeaconTierDef> tier;

    public PortableBeaconItem(Properties properties, ResourceKey<BeaconTierDef> tier) {
        super(properties);
        this.tier = tier;
    }

    public ResourceKey<BeaconTierDef> tier() {
        return tier;
    }

    public static BeaconState stateOf(ItemStack stack) {
        return stack.getOrDefault(BPComponents.BEACON.get(), BeaconState.EMPTY);
    }

    public static void setState(ItemStack stack, BeaconState state) {
        stack.set(BPComponents.BEACON.get(), state);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            BeaconMenuOpener.open(serverPlayer, player.getInventory().getSelectedSlot());
        }
        // The offhand has no drawn slot to freeze, so it opens nothing rather than opening a menu
        // whose source stack the player could still move. It keeps working while carried.
        return InteractionResult.SUCCESS;
    }

    void appendTooltip(ItemStack stack, TooltipContext context, Consumer<Component> tooltip) {
        BeaconState state = stateOf(stack);
        // The screen's light, in words and the same colours: green running, red waiting for fuel -
        // the one state that needs the player - grey off.
        if (state.active() && state.starved()) {
            tooltip.accept(Component.translatable("portablebeacons.msg.out_of_fuel").withStyle(ChatFormatting.RED));
        } else {
            tooltip.accept(Component.translatable(state.active()
                            ? "portablebeacons.gui.active" : "portablebeacons.gui.inactive")
                    .withStyle(state.active() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }

        if (!TooltipDetail.expanded()) {
            tooltip.accept(TooltipDetail.HINT);
            return;
        }
        if (context.registries() == null) {
            return;
        }
        context.registries().lookup(BPRegistryKeys.TIER)
                .flatMap(lookup -> lookup.get(tier))
                .ifPresent(holder -> tooltip.accept(Component.translatable("portablebeacons.tip.beacon_tier",
                                holder.value().level(),
                                holder.value().effectSlots(),
                                holder.value().augmentSlots())
                        .withStyle(ChatFormatting.DARK_GRAY)));

        // The configured effects, so a beacon in a chest can be identified without opening it.
        context.registries().lookup(BPRegistryKeys.EFFECT).ifPresent(lookup -> {
            for (EffectSlotConfig slot : state.effects()) {
                lookup.get(slot.effect()).ifPresent(holder -> tooltip.accept(Component.empty()
                        .append(holder.value().effect().value().getDisplayName())
                        .append(" ")
                        .append(String.valueOf(slot.amplifier() + 1))
                        .append(" - ")
                        .append(Component.translatable(
                                "portablebeacons.aura." + slot.aura().getSerializedName()))
                        .withStyle(slot.enabled() ? ChatFormatting.GRAY
                                : ChatFormatting.DARK_GRAY)));
            }
        });
        // Installed augments, which were missing entirely: two beacons of the same tier can behave
        // completely differently and looked identical in a chest.
        List<AugmentInstance> augments = BPLookups.installedAugments(stack);
        for (AugmentInstance augment : augments) {
            tooltip.accept(Component.translatable("portablebeacons.tip.augment_line",
                            Component.translatable("augment." + augment.type().identifier().getNamespace()
                                    + "." + augment.type().identifier().getPath(),
                                    Component.translatable("portablebeacons.tier." + augment.tier())))
                    // Grey like every other detail line: one palette for tooltips, FactoryIO's rule.
                    .withStyle(ChatFormatting.GRAY));
        }
        appendRuntime(stack, context, state, tooltip);
    }

    /**
     * Remaining runtime rather than a fuel count: the stored number is an implementation detail of
     * the datapack format, and only the time it buys is actionable.
     */
    private void appendRuntime(ItemStack stack, TooltipContext context, BeaconState state,
                               Consumer<Component> tooltip) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null || !BPConfig.fuelEnabled()) {
            return;
        }
        // The buffer and the slot together, as the screen counts it: the slot is burned before the
        // beacon runs dry, so leaving it out promised a fraction of the real runtime.
        int units = state.fuel() + BPLookups.reserveUnits(stack, registries);
        if (units <= 0) {
            return;
        }
        BeaconTierDef tierDef = lookup(registries, BPRegistryKeys.TIER, tier);
        if (tierDef == null) {
            return;
        }
        BeaconStats stats = BeaconResolver.resolve(tierDef, BPLookups.installedAugments(stack),
                key -> Optional.ofNullable(lookup(registries, BPRegistryKeys.AUGMENT, key)));
        double perSecond = BeaconResolver.fuelPerSecond(state, stats,
                key -> Optional.ofNullable(lookup(registries, BPRegistryKeys.EFFECT, key)));
        if (perSecond <= 0.0) {
            return;
        }
        tooltip.accept(Component.translatable("portablebeacons.gui.runtime",
                        Durations.format((int) (units / perSecond)))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Nullable
    private static <T> T lookup(HolderLookup.Provider registries,
                                ResourceKey<Registry<T>> registry, ResourceKey<T> key) {
        return registries.lookup(registry)
                .flatMap(lookup -> lookup.get(key))
                .map(Holder::value)
                .orElse(null);
    }


    /** A running beacon glints. Cheapest possible "this is on" signal, visible from the hotbar. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stateOf(stack).active();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stateOf(stack).capacity() > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(stateOf(stack).fillRatio() * 13.0);
    }

    /** Green to red as it empties, so a nearly dry beacon is obvious without reading the tooltip. */
    @Override
    public int getBarColor(ItemStack stack) {
        float ratio = (float) stateOf(stack).fillRatio();
        return Mth.hsvToRgb(ratio / 3.0F, 1.0F, 1.0F);
    }
}
