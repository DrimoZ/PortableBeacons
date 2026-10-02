package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The point of keeping {@code core} free of components, packets and rendering: the balance maths
 * can be exercised without launching Minecraft.
 */
class BeaconResolverTest {

    private static final BeaconTierDef TIER_4 = new BeaconTierDef(4, 2, 3, 16.0, 12000, 1, 1, List.of());

    private static final ResourceKey<AugmentDef> RANGE = augmentKey("range");
    private static final ResourceKey<AugmentDef> FOCUS = augmentKey("focus");

    private static final AugmentDef RANGE_DEF = new AugmentDef(3, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_RANGE, List.of(4.0, 8.0, 12.0))));
    private static final AugmentDef FOCUS_DEF = new AugmentDef(1, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.ADD_EFFECT_SLOT, List.of(1.0))));

    private static final ResourceKey<AugmentDef> ATTUNEMENT = augmentKey("attunement");
    private static final AugmentDef ATTUNEMENT_DEF = new AugmentDef(2, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.UNLOCK_AURA, List.of(1.0, 2.0))));

    private static final ResourceKey<AugmentDef> COMMUNION = augmentKey("communion");
    private static final AugmentDef COMMUNION_DEF = new AugmentDef(1, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.MUL_AURA_COST, List.of(0.5))));

    private static final ResourceKey<AugmentDef> WELLSPRING = augmentKey("wellspring");
    private static final AugmentDef WELLSPRING_DEF = new AugmentDef(1, 0, List.of(
            new AugmentDef.Operation(AugmentDef.Type.FREE_EFFECT_SLOT, List.of(1.0)),
            new AugmentDef.Operation(AugmentDef.Type.MUL_FUEL, List.of(1.6))));

    private static final BeaconResolver.Lookup<AugmentDef> AUGMENTS = lookup(Map.of(
            RANGE, RANGE_DEF,
            FOCUS, FOCUS_DEF,
            ATTUNEMENT, ATTUNEMENT_DEF,
            COMMUNION, COMMUNION_DEF,
            WELLSPRING, WELLSPRING_DEF));

    /** The ceiling is level X now, not IV: a datapack raising it is not clamped back by the resolver. */
    @Test
    void theLevelCeilingIsLevelTen() {
        ResourceKey<AugmentDef> big = augmentKey("big");
        AugmentDef bigDef = new AugmentDef(1, 0, List.of(
                new AugmentDef.Operation(AugmentDef.Type.ADD_AMPLIFIER, List.of(20.0))));

        BeaconStats stats = BeaconResolver.resolve(TIER_4, List.of(new AugmentInstance(big, 1)),
                lookup(Map.of(big, bigDef)));

        assertEquals(BeaconStats.MAX_AMPLIFIER, stats.maxAmplifier());
        assertEquals(9, stats.maxAmplifier());
    }

    @Test
    void augmentsStackAcrossTypes() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 2), new AugmentInstance(FOCUS, 1)),
                AUGMENTS);

        assertEquals(24.0, stats.range());
        assertEquals(3, stats.effectSlots());
    }

    @Test
    void duplicateAugmentTypeIsIgnored() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(RANGE, 3), new AugmentInstance(RANGE, 3)),
                AUGMENTS);

        assertEquals(28.0, stats.range(), "a second Range augment must not stack");
    }

    @Test
    void augmentTierIsClampedToItsDeclaredMaximum() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(FOCUS, 3)),
                AUGMENTS);

        assertEquals(3, stats.effectSlots(), "Focus only exists at tier 1");
    }

    @Test
    void tierOneCannotProject() {
        BeaconStats stats = BeaconResolver.resolve(
                new BeaconTierDef(1, 1, 0, 0.0, 600, 0, 0, List.of()), List.of(), AUGMENTS);

        assertTrue(stats.allows(AuraMode.SELF));
        assertFalse(stats.allows(AuraMode.ALLIES));
    }

    /**
     * The augment shipped doing nothing for two releases: every tier already cleared the threshold
     * it raised, so there was never a mode left for it to unlock. Nothing tested that it did.
     */
    @Test
    void attunementUnlocksModesTheTierDoesNotGrant() {
        BeaconStats unaided = BeaconResolver.resolve(TIER_4, List.of(), AUGMENTS);
        assertTrue(unaided.allows(AuraMode.TEAM), "tier 4 shares with a team on its own");
        assertFalse(unaided.allows(AuraMode.ALLIES), "and needs help to go wider");

        BeaconStats attuned = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(ATTUNEMENT, 1)), AUGMENTS);
        assertTrue(attuned.allows(AuraMode.ALLIES), "Attunement I reaches allies");
        assertFalse(attuned.allows(AuraMode.ALLIES_AND_PETS), "but not their pets yet");

        BeaconStats fully = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(ATTUNEMENT, 2)), AUGMENTS);
        assertTrue(fully.allows(AuraMode.ALLIES_AND_PETS), "Attunement II reaches pets too");
    }

    /**
     * The sharing surcharge was folded into the effect's own cost until Communion needed to discount
     * it alone. Splitting it out must leave every existing beacon charging exactly what it did.
     */
    @Test
    void separatingTheSharingSurchargeDidNotChangeAnyPrice() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4, List.of(), AUGMENTS);
        BeaconResolver.Lookup<BeaconEffectDef> effects =
                lookup(Map.of(effectKey("speed"), new BeaconEffectDef(null, 1.0, 2, 1, 2.0)));

        // 1.0 base, level II doubles it, allies multiplies by 2.0, range 16 adds 1 + 16/64.
        double allies = BeaconResolver.fuelPerSecond(
                new EffectSlotConfig(effectKey("speed"), 1, true, AuraMode.ALLIES), stats, effects);
        assertEquals(1.0 * 2.0 * 2.0 * 1.25, allies, 1e-9);

        // Self pays neither the surcharge nor the range factor.
        double self = BeaconResolver.fuelPerSecond(
                new EffectSlotConfig(effectKey("speed"), 1, true, AuraMode.SELF), stats, effects);
        assertEquals(2.0, self, 1e-9);
    }

    @Test
    void communionDiscountsTheSurchargeAndNotTheEffect() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(COMMUNION, 1)), AUGMENTS);
        BeaconResolver.Lookup<BeaconEffectDef> effects =
                lookup(Map.of(effectKey("speed"), new BeaconEffectDef(null, 1.0, 2, 1, 2.0)));

        // Allies costs +100%; halving the surcharge leaves +50%, so 1.5 rather than 2.0.
        double allies = BeaconResolver.fuelPerSecond(
                new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.ALLIES), stats, effects);
        assertEquals(1.0 * 1.5 * 1.25, allies, 1e-9);

        // What you keep to yourself is untouched by it.
        double self = BeaconResolver.fuelPerSecond(
                new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.SELF), stats, effects);
        assertEquals(1.0, self, 1e-9);
    }

    /**
     * The free slot must land on the dearest effect, not the first one configured — otherwise the
     * augment is worth whatever order the player happened to set things up in.
     */
    @Test
    void aFreeSlotIsSpentOnTheMostExpensiveEffect() {
        BeaconStats stats = BeaconResolver.resolve(TIER_4,
                List.of(new AugmentInstance(WELLSPRING, 1)), AUGMENTS);
        BeaconResolver.Lookup<BeaconEffectDef> effects = lookup(Map.of(
                effectKey("speed"), new BeaconEffectDef(null, 1.0, 0, 1, 2.0),
                effectKey("haste"), new BeaconEffectDef(null, 5.0, 0, 1, 2.0)));

        BeaconState cheapFirst = new BeaconState(List.of(
                new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.SELF),
                new EffectSlotConfig(effectKey("haste"), 0, true, AuraMode.SELF)),
                1000, true, 12000);

        // Wellspring frees the 5.0 one and charges the 1.0, then its own x1.6 applies.
        assertEquals(1.0 * 1.6, BeaconResolver.fuelPerSecond(cheapFirst, stats, effects), 1e-9);
    }

    @Test
    void inactivePackCostsNothing() {
        BeaconState state = new BeaconState(
                List.of(new EffectSlotConfig(effectKey("speed"), 0, true, AuraMode.SELF)),
                1000, false, 12000);
        BeaconStats stats = BeaconResolver.resolve(TIER_4, List.of(), AUGMENTS);

        assertEquals(0.0, BeaconResolver.fuelPerSecond(state, stats,
                lookup(Map.of(effectKey("speed"), new BeaconEffectDef(null, 1.0, 1, 1, 2.5)))));
    }

    private static <T> BeaconResolver.Lookup<T> lookup(Map<ResourceKey<T>, T> entries) {
        return key -> Optional.ofNullable(entries.get(key));
    }

    private static ResourceKey<AugmentDef> augmentKey(String path) {
        return ResourceKey.create(BPRegistryKeys.AUGMENT,
                Identifier.fromNamespaceAndPath("portablebeacons", path));
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                Identifier.fromNamespaceAndPath("portablebeacons", path));
    }
}
