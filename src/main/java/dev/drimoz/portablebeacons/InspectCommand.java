package dev.drimoz.portablebeacons;

import com.mojang.brigadier.context.CommandContext;
import dev.drimoz.portablebeacons.core.AuraMode;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /portablebeacons inspect}: the held beacon's stats after augments and server rules.
 *
 * <p>For datapack authors. The screen shows what a player needs - time left, reach, levels - and
 * hides the factors that produce them, so a pack whose augment "does nothing" could only be
 * debugged by guessing. Untranslated on purpose: these are field names, like {@code /data get}.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class InspectCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(PortableBeacons.MOD_ID)
                .then(Commands.literal("inspect")
                        .requires(source -> source.hasPermission(2))
                        .executes(InspectCommand::inspect)));
    }

    private static int inspect(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BeaconStats stats = null;
        ItemStack beacon = ItemStack.EMPTY;
        if (source.getEntity() instanceof ServerPlayer player
                && player.getMainHandItem().getItem() instanceof PortableBeaconItem) {
            beacon = player.getMainHandItem();
            stats = BPLookups.stats(beacon, player.level().registryAccess());
        }
        if (stats == null) {
            source.sendFailure(Component.translatable("portablebeacons.msg.inspect_hold"));
            return 0;
        }
        var access = source.registryAccess();
        BeaconState state = PortableBeaconItem.stateOf(beacon);
        double perSecond = BeaconResolver.fuelPerSecond(state, stats, BPLookups.effects(access));
        int seconds = PortableBeaconItem.runtimeSeconds(beacon, state, access);

        line(source, "effect_slots %d, augment_slots %d, max_amplifier %d",
                stats.effectSlots(), stats.augmentSlots(), stats.maxAmplifier());
        line(source, "range %.1f, fuel_capacity %d, fuel %d", stats.range(), stats.fuelCapacity(), state.fuel());
        line(source, "fuel x%.3f, aura cost x%.3f, moving x%.3f, still x%.3f, free slots %d",
                stats.fuelMultiplier(), stats.auraCostMultiplier(), stats.movingCostMultiplier(),
                stats.stillCostMultiplier(), stats.freeEffectSlots());
        line(source, "aura modes %s", stats.allowedAuraModes().stream()
                .map(AuraMode::getSerializedName).collect(Collectors.joining(", ")));
        if (!stats.effectAmplifierBonus().isEmpty() || !stats.effectCostMultipliers().isEmpty()) {
            line(source, "per effect: levels %s, cost %s",
                    stats.effectAmplifierBonus(), stats.effectCostMultipliers());
        }
        line(source, "solar %.2f/s, linger %d s, last stand %d, mark hostiles %d", stats.solarChargePerSecond(),
                stats.lingerSeconds(), stats.lastStandLevel(), stats.markHostiles());
        line(source, "drawing %.3f/s standing, runtime %s", perSecond,
                seconds < 0 ? "idle" : Durations.format(seconds));
        return 1;
    }

    private static void line(CommandSourceStack source, String format, Object... args) {
        // ROOT so a decimal comma never turns "1.5" into "1,5" in a line meant to be copied.
        String text = String.format(Locale.ROOT, format, args);
        source.sendSuccess(() -> Component.literal(text), false);
    }

    private InspectCommand() {}
}
