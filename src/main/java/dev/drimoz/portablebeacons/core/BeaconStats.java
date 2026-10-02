package dev.drimoz.portablebeacons.core;

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
        boolean hideIcon
) {
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
