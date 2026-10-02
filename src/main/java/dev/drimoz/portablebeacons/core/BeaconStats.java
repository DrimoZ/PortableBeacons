package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;

import java.util.Map;
import java.util.Set;

/**
 * A beacon tier's stats after its augments have been applied. Produced by {@link BeaconResolver} and
 * consumed by the ticking logic, the GUI and the tooltip alike, so the number the player reads is
 * always the number the server charges.
 *
 * @param effectSlots      configurable effect slots
 * @param augmentSlots     unlocked augment slots
 * @param range            aura radius in blocks
 * @param fuelCapacity     buffer size in fuel units
 * @param maxAmplifier     highest amplifier any effect may reach
 * @param fuelMultiplier   global factor on consumption (Efficiency lowers it)
 * @param allowedAuraModes aura modes the player may pick
 * @param effectAmplifierBonus per effect, levels added to {@code maxAmplifier} by targeted augments
 * @param effectCostMultipliers per effect, factors on its cost from targeted augments
 */
public record BeaconStats(
        int effectSlots,
        int augmentSlots,
        double range,
        int fuelCapacity,
        int maxAmplifier,
        double fuelMultiplier,
        double auraCostMultiplier,
        int freeEffectSlots,
        double movingCostMultiplier,
        double stillCostMultiplier,
        Set<AuraMode> allowedAuraModes,
        boolean hideParticles,
        boolean hideIcon,
        Map<ResourceKey<BeaconEffectDef>, Integer> effectAmplifierBonus,
        Map<ResourceKey<BeaconEffectDef>, Double> effectCostMultipliers
) {
    public BeaconStats {
        effectAmplifierBonus = Map.copyOf(effectAmplifierBonus);
        effectCostMultipliers = Map.copyOf(effectCostMultipliers);
    }

    /** Stats with no per-effect adjustment, which is what a beacon without targeted augments has. */
    public BeaconStats(int effectSlots, int augmentSlots, double range, int fuelCapacity, int maxAmplifier,
                       double fuelMultiplier, double auraCostMultiplier, int freeEffectSlots,
                       double movingCostMultiplier, double stillCostMultiplier, Set<AuraMode> allowedAuraModes,
                       boolean hideParticles, boolean hideIcon) {
        this(effectSlots, augmentSlots, range, fuelCapacity, maxAmplifier, fuelMultiplier, auraCostMultiplier,
                freeEffectSlots, movingCostMultiplier, stillCostMultiplier, allowedAuraModes, hideParticles,
                hideIcon, Map.of(), Map.of());
    }

    /** The level ceiling for one effect: the beacon's, plus whatever a targeted augment adds. */
    public int maxAmplifierFor(ResourceKey<BeaconEffectDef> effect) {
        return Math.clamp(maxAmplifier + effectAmplifierBonus.getOrDefault(effect, 0), 0, MAX_AMPLIFIER);
    }

    public double costMultiplierFor(ResourceKey<BeaconEffectDef> effect) {
        return effectCostMultipliers.getOrDefault(effect, 1.0);
    }
    /**
     * The most effect slots a beacon can ever have, however generous the tier and the augments.
     *
     * <p>Lives here rather than in the screen because it is not only a drawing limit: an effect
     * beyond what the screen lays out would still be resolved, charged and kept by sanitize — paid
     * for and invisible. The cap belongs where the number is decided, so the two cannot disagree.
     */
    public static final int MAX_EFFECT_SLOTS = 8;

    /**
     * The highest amplifier anything may reach - level X. One ceiling for the codecs, the resolver
     * and the menu, so a datapack that raises an effect's cap is not silently clamped somewhere else.
     */
    public static final int MAX_AMPLIFIER = 9;

    /**
     * Augment slots a beacon carries; a tier unlocks up to this many. The container layout puts fuel
     * first precisely so this can grow without moving anyone's saved augments.
     */
    public static final int MAX_AUGMENT_SLOTS = 8;

    public boolean allows(AuraMode mode) {
        return allowedAuraModes.contains(mode);
    }
}
