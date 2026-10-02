package dev.drimoz.portablebeacons;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/** Server-side knobs. Kept deliberately short: content belongs in datapacks, not in a config file. */
public final class BPConfig {

    public static final ModConfigSpec SPEC;
    public static final BPConfig INSTANCE;

    static {
        Pair<BPConfig, ModConfigSpec> pair =
                new ModConfigSpec.Builder().configure(BPConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    /**
     * Whether fuel exists at all, from either side.
     *
     * <p>Server configs are synced, but the menu is also built during login before that lands, so
     * an unloaded spec answers "yes" rather than throwing. The GUI hides every trace of fuel when
     * this is off - a gauge that never moves and a slot that accepts nothing are worse than absent.
     */
    public static boolean fuelEnabled() {
        return !SPEC.isLoaded() || INSTANCE.requireFuel.get();
    }

    public final ModConfigSpec.BooleanValue requireFuel;
    public final ModConfigSpec.BooleanValue auraAffectsNonTeamPlayers;
    public final ModConfigSpec.BooleanValue freeWhileNearBeacon;
    public final ModConfigSpec.BooleanValue requireBeaconToConfigure;
    public final ModConfigSpec.IntValue beaconRechargePerSecond;
    public final ModConfigSpec.IntValue energyPerFuelUnit;
    public final ModConfigSpec.DoubleValue fuelCostMultiplier;
    public final ModConfigSpec.DoubleValue maxAuraRange;
    public final ModConfigSpec.ConfigValue<java.util.List<? extends String>> disabledDimensions;

    /** 1.0 until the spec loads, so a menu built at login before the sync costs what the datapack says. */
    public static double fuelCostMultiplier() {
        return SPEC.isLoaded() ? INSTANCE.fuelCostMultiplier.get() : 1.0;
    }

    /** 0 - no cap - until the spec loads. */
    public static double maxAuraRange() {
        return SPEC.isLoaded() ? INSTANCE.maxAuraRange.get() : 0.0;
    }

    public static boolean disabledIn(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
        return SPEC.isLoaded() && INSTANCE.disabledDimensions.get().contains(dimension.identifier().toString());
    }

    /** Forge energy per fuel unit, or 0 when energy charging is off - including before the spec loads. */
    public static int energyPerFuelUnit() {
        return SPEC.isLoaded() && fuelEnabled() ? INSTANCE.energyPerFuelUnit.get() : 0;
    }

    private BPConfig(ModConfigSpec.Builder builder) {
        builder.push("gameplay");

        requireFuel = builder
                .comment("Whether beacons consume fuel. Disable for a purely craft-gated mod.")
                .define("require_fuel", true);

        auraAffectsNonTeamPlayers = builder
                .comment("Allow aura effects to reach players who are not on your scoreboard team.",
                        "Servers with PvP usually want this off.")
                .define("aura_affects_non_team_players", true);

        freeWhileNearBeacon = builder
                .comment("Stop charging for an effect a real beacon is already providing.")
                .define("free_while_near_beacon", true);

        requireBeaconToConfigure = builder
                .comment("Require a lit beacon within 16 blocks to change a beacon's effects.",
                        "Off by default: the beacon block is already on the crafting path of every",
                        "tier. Turn it on for a beacon that wants the stricter progression.")
                .define("require_beacon_to_configure", false);

        beaconRechargePerSecond = builder
                .comment("Fuel units per second a carried beacon gains while inside a lit beacon's range,",
                        "whether it is switched on or not. A base with a beacon becomes a charging station,",
                        "which keeps the block worth building. 0 turns recharging off.")
                .defineInRange("beacon_recharge_per_second", 20, 0, 1_000_000);

        energyPerFuelUnit = builder
                .comment("Forge energy (FE) one fuel unit is worth when a beacon is charged in another mod's",
                        "charger. 40 makes an iron ingot's 300 units 12,000 FE. 0 turns energy charging off.")
                .defineInRange("energy_per_fuel_unit", 40, 0, 1_000_000);

        fuelCostMultiplier = builder
                .comment("Multiplies every beacon's fuel cost, after augments. 0.5 halves it, 2.0 doubles it.",
                        "A server-wide balance knob that leaves the datapack's relative costs alone.")
                .defineInRange("fuel_cost_multiplier", 1.0, 0.0, 100.0);

        maxAuraRange = builder
                .comment("Caps how far a shared effect reaches, in blocks, whatever the tier and augments add.",
                        "0 for no cap.")
                .defineInRange("max_aura_range", 0.0, 0.0, 1024.0);

        disabledDimensions = builder
                .comment("Dimensions where portable beacons do nothing and spend nothing, by id -",
                        "for example [\"minecraft:the_end\"] to keep a boss fight unassisted.")
                .defineListAllowEmpty("disabled_dimensions", java.util.List.of(), () -> "minecraft:the_end",
                        entry -> entry instanceof String text && net.minecraft.resources.Identifier.tryParse(text) != null);

        builder.pop();
    }
}
