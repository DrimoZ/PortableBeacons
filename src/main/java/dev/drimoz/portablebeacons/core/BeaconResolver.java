package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * All of the mod's arithmetic, with no dependency on data components, packets or rendering.
 *
 * <p>Keeping this layer pure is what makes the numbers testable in plain JUnit, and what keeps a
 * 1.20.1 backport down to rewriting the serialization rather than the behaviour.
 */
public final class BeaconResolver {

    /** How definitions are looked up, so this class never touches a registry directly. */
    @FunctionalInterface
    public interface Lookup<T> {
        Optional<T> get(ResourceKey<T> key);

        static <T> Lookup<T> of(Function<ResourceKey<T>, Optional<T>> fn) {
            return fn::apply;
        }
    }

    /** Applies every augment to a tier's base stats. Unknown augment keys are ignored. */
    public static BeaconStats resolve(BeaconTierDef tier,
                                    List<AugmentInstance> augments,
                                    Lookup<AugmentDef> augmentLookup) {
        int effectSlots = tier.effectSlots();
        int augmentSlots = tier.augmentSlots();
        double range = tier.baseRange();
        double capacityMultiplier = 1.0;
        int maxAmplifier = tier.maxAmplifier();
        double fuelMultiplier = 1.0;
        double auraCostMultiplier = 1.0;
        int freeEffectSlots = 0;
        double movingCostMultiplier = 1.0;
        double stillCostMultiplier = 1.0;
        int auraTierBonus = 0;
        int concealment = 0;
        Map<ResourceKey<BeaconEffectDef>, Integer> effectBonus = new HashMap<>();
        Map<ResourceKey<BeaconEffectDef>, Double> effectCost = new HashMap<>();

        for (AugmentInstance instance : dedupeByType(augments)) {
            Optional<AugmentDef> maybeDef = augmentLookup.get(instance.type());
            if (maybeDef.isEmpty()) {
                continue;
            }
            AugmentDef def = maybeDef.get();
            int tierLevel = Math.clamp(instance.tier(), 1, def.maxTier());

            for (AugmentDef.Operation op : def.operations()) {
                double value = op.valueFor(tierLevel);
                switch (op.type()) {
                    case ADD_RANGE -> range += value;
                    case ADD_EFFECT_SLOT -> effectSlots += (int) value;
                    case ADD_AMPLIFIER -> maxAmplifier += (int) value;
                    case MUL_FUEL -> fuelMultiplier *= value;
                    case MUL_CAPACITY -> capacityMultiplier *= value;
                    case UNLOCK_AURA -> auraTierBonus += (int) value;
                    case MUL_AURA_COST -> auraCostMultiplier *= value;
                    case FREE_EFFECT_SLOT -> freeEffectSlots += (int) value;
                    case MUL_COST_MOVING -> movingCostMultiplier *= value;
                    case MUL_COST_STILL -> stillCostMultiplier *= value;
                    // Highest wins rather than summing: this value names a behaviour, so adding
                    // two of them would be meaningless.
                    case HIDE_EFFECTS -> concealment = Math.max(concealment, (int) value);
                    // Targeted: the codec guarantees an effect is named, so these never act on nothing.
                    case ADD_EFFECT_AMPLIFIER -> op.effect().ifPresent(key ->
                            effectBonus.merge(key, (int) value, Integer::sum));
                    case MUL_EFFECT_COST -> op.effect().ifPresent(key ->
                            effectCost.merge(key, Math.max(0.0, value), (a, b) -> a * b));
                }
            }
        }

        int auraRank = tier.auraRank() + auraTierBonus;
        EnumSet<AuraMode> auraModes = EnumSet.noneOf(AuraMode.class);
        for (AuraMode mode : AuraMode.values()) {
            if (mode.rank() <= auraRank) {
                auraModes.add(mode);
            }
        }

        return new BeaconStats(
                Math.clamp(effectSlots, 0, BeaconStats.MAX_EFFECT_SLOTS),
                augmentSlots,
                Math.max(0.0, range),
                (int) Math.round(tier.fuelCapacity() * capacityMultiplier),
                Math.clamp(maxAmplifier, 0, BeaconStats.MAX_AMPLIFIER),
                Math.max(0.0, fuelMultiplier),
                Math.max(0.0, auraCostMultiplier),
                Math.max(0, freeEffectSlots),
                Math.max(0.0, movingCostMultiplier),
                Math.max(0.0, stillCostMultiplier),
                auraModes,
                concealment >= 1,
                concealment >= 2,
                effectBonus,
                effectCost);
    }

    /**
     * Enforces "one augment per type" in the resolver too, not only in the slot's placement rule.
     * The GUI rejects duplicates, but a stack built by a command or a broken datapack must not be
     * able to stack two Range augments.
     */
    private static List<AugmentInstance> dedupeByType(List<AugmentInstance> augments) {
        List<AugmentInstance> kept = new ArrayList<>(augments.size());
        for (AugmentInstance instance : augments) {
            if (kept.stream().noneMatch(other -> other.type().equals(instance.type()))) {
                kept.add(instance);
            }
        }
        return kept;
    }

    /**
     * Total fuel units per second, summed per enabled effect.
     *
     * <p>Range only enters the cost of effects that actually project ({@link AuraMode#isAura()}),
     * which is why the info panel can show a believable per-line cost instead of one opaque total.
     */
    public static double fuelPerSecond(BeaconState state,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup) {
        return fuelPerSecond(state, stats, effectLookup, false);
    }

    /**
     * Total fuel units per second.
     *
     * <p>{@code moving} picks between the two context multipliers. Both default to 1.0, so a beacon
     * with no such augment costs the same either way and callers need not care.
     *
     * <p>Free slots are applied to the <em>most expensive</em> effects rather than the first ones.
     * Taking them in configured order would make the augment's worth depend on the order the player
     * happened to set the effects in, which is not a decision — just a thing to get right or wrong
     * without being told.
     */
    public static double fuelPerSecond(BeaconState state,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup,
                                       boolean moving) {
        return state.active()
                ? fuelPerSecond(state.effects(), stats, effectLookup, moving)
                : 0.0;
    }

    /**
     * The bill for a given set of effects.
     *
     * <p>Takes the list rather than the state so the ticker can pass the ones it is actually
     * applying — it drops any a real beacon already covers, and a free slot must be spent on
     * something being charged for.
     */
    public static double fuelPerSecond(List<EffectSlotConfig> slots,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup,
                                       boolean moving) {
        List<Double> costs = new ArrayList<>(slots.size());
        for (EffectSlotConfig slot : slots) {
            costs.add(fuelPerSecond(slot, stats, effectLookup));
        }
        costs.sort(Comparator.reverseOrder());

        double total = 0.0;
        for (int i = stats.freeEffectSlots(); i < costs.size(); i++) {
            total += costs.get(i);
        }
        double context = moving ? stats.movingCostMultiplier() : stats.stillCostMultiplier();
        return total * stats.fuelMultiplier() * context;
    }

    /**
     * Which of these effects the free slots cover, in the order given.
     *
     * <p>The screen needs the same answer the bill uses. Working it out twice, once here and once
     * there, is how a display comes to claim an effect is 375% of a total it was excluded from.
     */
    public static boolean[] freeMask(List<EffectSlotConfig> slots,
                                     BeaconStats stats,
                                     Lookup<BeaconEffectDef> effectLookup) {
        boolean[] free = new boolean[slots.size()];
        if (stats.freeEffectSlots() <= 0) {
            return free;
        }
        // The dearest first, ties broken by position so the answer is stable frame to frame.
        Integer[] byCost = new Integer[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            byCost[i] = i;
        }
        java.util.Arrays.sort(byCost, Comparator.comparingDouble(
                (Integer i) -> fuelPerSecond(slots.get(i), stats, effectLookup)).reversed());

        for (int n = 0; n < Math.min(stats.freeEffectSlots(), byCost.length); n++) {
            free[byCost[n]] = true;
        }
        return free;
    }

    /** Per-effect cost, excluding the beacon-wide {@link BeaconStats#fuelMultiplier()}. */
    public static double fuelPerSecond(EffectSlotConfig slot,
                                       BeaconStats stats,
                                       Lookup<BeaconEffectDef> effectLookup) {
        if (!slot.enabled()) {
            return 0.0;
        }
        Optional<BeaconEffectDef> maybeDef = effectLookup.get(slot.effect());
        if (maybeDef.isEmpty()) {
            return 0.0;
        }
        // The sharing surcharge is separated out so an augment can discount it alone: everything
        // else here scales what an effect costs you, this scales what it costs to give away.
        double shared = slot.aura().isAura()
                ? 1.0 + (slot.aura().costMultiplier() - 1.0) * stats.auraCostMultiplier()
                : 1.0;
        double base = maybeDef.get().costPerSecond(slot.amplifier(), AuraMode.SELF) * shared
                * stats.costMultiplierFor(slot.effect());
        return slot.aura().isAura() ? base * rangeFactor(stats.range()) : base;
    }

    /**
     * Range scales cost mildly rather than linearly: a linear factor made the largest Range augment
     * strictly worse than no augment at all, which is not a choice, just a trap.
     */
    private static double rangeFactor(double range) {
        return 1.0 + range / 64.0;
    }

    /**
     * Drops or clamps anything the current tier and augments no longer permit — trimming excess
     * effect slots, capping amplifiers, and falling back to {@link AuraMode#SELF} for modes that
     * are no longer unlocked. Called whenever an augment is removed, so a downgraded beacon can never
     * keep projecting what it can no longer pay for.
     */
    public static BeaconState sanitize(BeaconState state,
                                     BeaconStats stats,
                                     Lookup<BeaconEffectDef> effectLookup,
                                     BeaconTierDef tier) {
        List<EffectSlotConfig> kept = new ArrayList<>(stats.effectSlots());
        for (EffectSlotConfig slot : state.effects()) {
            if (kept.size() >= stats.effectSlots()) {
                break;
            }
            Optional<BeaconEffectDef> maybeDef = effectLookup.get(slot.effect());
            if (maybeDef.isEmpty()
                    || maybeDef.get().minTier() > tier.level()
                    || !tier.allows(slot.effect(), maybeDef.get())) {
                continue;
            }
            int amplifierCap = Math.min(maybeDef.get().maxAmplifier(), stats.maxAmplifierFor(slot.effect()));
            AuraMode aura = stats.allows(slot.aura()) ? slot.aura() : AuraMode.SELF;
            kept.add(slot.withAmplifier(Math.clamp(slot.amplifier(), 0, amplifierCap))
                    .withAura(aura));
        }
        // Fuel above the new capacity is kept, not clamped. Clamping meant pulling a Capacity
        // augment out of a full beacon destroyed three quarters of what was in it; kept, the
        // surplus simply burns down, and no refill is accepted until it has.
        return state.withEffects(kept)
                .withCapacity(stats.fuelCapacity());
    }

    private BeaconResolver() {}
}
