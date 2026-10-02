package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;

import java.util.List;

/**
 * One entry of the {@code portablebeacons:effect} datapack registry: an effect a beacon is allowed to
 * project, and what it costs.
 *
 * <pre>{@code
 * {
 *   "effect": "minecraft:regeneration",
 *   "cost": 3.0,
 *   "max_amplifier": 1,
 *   "min_tier": 3
 * }
 * }</pre>
 *
 * @param effect                  the projected mob effect
 * @param cost                    base fuel units per second at amplifier 0, self-only
 * @param maxAmplifier            highest amplifier this effect may be raised to (0 = level I only)
 * @param minTier                 lowest beacon tier that may select it
 * @param amplifierCostMultiplier cost factor applied per amplifier level above 0
 * @param pools                   pools this effect belongs to; a tier listing {@code #name} in its
 *                                {@code effect_pool} offers every effect in that pool, so adding
 *                                an effect is one file rather than one plus every tier
 */
public record BeaconEffectDef(
        Holder<MobEffect> effect,
        double cost,
        int maxAmplifier,
        int minTier,
        double amplifierCostMultiplier,
        List<String> pools
) {
    public static final Codec<BeaconEffectDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec()
                    .fieldOf("effect").forGetter(BeaconEffectDef::effect),
            Codec.DOUBLE.optionalFieldOf("cost", 1.0)
                    .forGetter(BeaconEffectDef::cost),
            Codec.intRange(0, BeaconStats.MAX_AMPLIFIER).optionalFieldOf("max_amplifier", 0)
                    .forGetter(BeaconEffectDef::maxAmplifier),
            Codec.intRange(1, 4).optionalFieldOf("min_tier", 1)
                    .forGetter(BeaconEffectDef::minTier),
            Codec.DOUBLE.optionalFieldOf("amplifier_cost_multiplier", 2.0)
                    .forGetter(BeaconEffectDef::amplifierCostMultiplier),
            Codec.STRING.listOf().optionalFieldOf("pools", List.of()).forGetter(BeaconEffectDef::pools)
    ).apply(i, BeaconEffectDef::new));

    public BeaconEffectDef {
        pools = List.copyOf(pools);
    }

    /** An effect in no pool, reachable only by tiers that name it. */
    public BeaconEffectDef(Holder<MobEffect> effect, double cost, int maxAmplifier, int minTier,
                           double amplifierCostMultiplier) {
        this(effect, cost, maxAmplifier, minTier, amplifierCostMultiplier, List.of());
    }

    /** Fuel units per second for this effect at the given amplifier and aura mode. */
    public double costPerSecond(int amplifier, AuraMode aura) {
        double amplified = cost * Math.pow(amplifierCostMultiplier, Math.max(0, amplifier));
        return amplified * aura.costMultiplier();
    }
}
