package dev.drimoz.portablebeacons.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code sanitize} is the safety net: it runs after every action and every tick, and is what stops
 * a beacon from projecting something it can no longer pay for after an augment is pulled out.
 */
class SanitizeTest {

    private static final ResourceKey<BeaconEffectDef> SPEED = effectKey("speed");
    private static final ResourceKey<BeaconEffectDef> STRENGTH = effectKey("strength");

    private static final BeaconEffectDef SPEED_DEF =
            new BeaconEffectDef(null, 1.0, 1, 1, 2.0);
    private static final BeaconEffectDef STRENGTH_DEF =
            new BeaconEffectDef(null, 2.0, 1, 3, 2.0);

    private static final BeaconResolver.Lookup<BeaconEffectDef> EFFECTS =
            key -> Optional.ofNullable(Map.of(SPEED, SPEED_DEF, STRENGTH, STRENGTH_DEF).get(key));

    @Test
    void effectsBeyondTheSlotCountAreDropped() {
        BeaconTierDef tier = tier(4, 1, List.of());
        BeaconState state = new BeaconState(List.of(
                slot(SPEED, 0, AuraMode.SELF),
                slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        BeaconState result = BeaconResolver.sanitize(state, stats(tier, 1, 1), EFFECTS, tier);

        assertEquals(1, result.effects().size());
    }

    @Test
    void effectsTheDatapackNoLongerDefinesAreDropped() {
        // A beacon saved in a world whose datapack has since removed the effect. The key still
        // deserialises fine - nothing about a ResourceKey requires the entry to exist - so this is
        // the only place the missing entry can be caught, and every consumer downstream assumes
        // sanitize already did.
        BeaconTierDef tier = tier(4, 2, List.of());
        BeaconState state = new BeaconState(
                List.of(slot(effectKey("removed_by_a_datapack"), 0, AuraMode.SELF)), 0, true, 0);

        assertTrue(BeaconResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier).effects().isEmpty());
    }

    @Test
    void effectsAboveTheTierAreDropped() {
        BeaconTierDef tier = tier(2, 2, List.of());
        BeaconState state = new BeaconState(List.of(slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        assertTrue(BeaconResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier).effects().isEmpty());
    }

    @Test
    void effectsOutsideTheTierPoolAreDropped() {
        // A themed beacon must not keep an effect it was never allowed to project, however it got in.
        BeaconTierDef tier = tier(4, 2, List.of(SPEED));
        BeaconState state = new BeaconState(List.of(slot(STRENGTH, 0, AuraMode.SELF)), 0, true, 0);

        assertTrue(BeaconResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier).effects().isEmpty());
    }

    @Test
    void amplifierIsCappedWhenAmplificationIsRemoved() {
        BeaconTierDef tier = tier(4, 2, List.of());
        BeaconState state = new BeaconState(List.of(slot(SPEED, 1, AuraMode.SELF)), 0, true, 0);

        BeaconState result = BeaconResolver.sanitize(state, stats(tier, 2, 0), EFFECTS, tier);

        assertEquals(0, result.effects().getFirst().amplifier());
    }

    @Test
    void auraFallsBackToSelfWhenTheModeIsNoLongerUnlocked() {
        BeaconTierDef tier = tier(1, 1, List.of());
        BeaconState state = new BeaconState(List.of(slot(SPEED, 0, AuraMode.ALLIES)), 0, true, 0);

        BeaconState result = BeaconResolver.sanitize(state, stats(tier, 1, 0), EFFECTS, tier);

        assertEquals(AuraMode.SELF, result.effects().getFirst().aura());
    }

    @Test
    void capacityIsCachedForTheItemsFuelBar() {
        BeaconTierDef tier = tier(4, 2, List.of());
        BeaconState state = new BeaconState(List.of(), 99999, true, 0);

        BeaconState result = BeaconResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier);

        assertEquals(12000, result.capacity());
    }

    /** Regression: removing a Capacity augment from a full beacon destroyed the surplus. */
    @Test
    void fuelAboveANewlyLowerCapacityIsKept() {
        BeaconTierDef tier = tier(4, 2, List.of());
        BeaconState state = new BeaconState(List.of(), 48000, true, 48000);

        BeaconState result = BeaconResolver.sanitize(state, stats(tier, 2, 1), EFFECTS, tier);

        assertEquals(48000, result.fuel());
        assertEquals(12000, result.capacity());
        assertEquals(1.0, result.fillRatio(), "the bar reads full, not past full");
    }

    @Test
    void aBufferAboveCapacityTakesNoRefill() {
        assertEquals(0, FuelBudget.itemsToBurn(48000, 48100, 300, 12000, 64));
    }

    private static BeaconStats stats(BeaconTierDef tier, int effectSlots, int maxAmplifier) {
        return new BeaconStats(effectSlots, tier.augmentSlots(), tier.baseRange(), 12000,
                maxAmplifier, 1.0, 1.0, 0, 1.0, 1.0,
                java.util.EnumSet.of(AuraMode.SELF), false, false);
    }

    private static BeaconTierDef tier(int level, int slots, List<ResourceKey<BeaconEffectDef>> pool) {
        return new BeaconTierDef(level, slots, 3, 16.0, 12000, 1, 1, pool);
    }

    private static EffectSlotConfig slot(ResourceKey<BeaconEffectDef> effect, int amplifier,
                                         AuraMode aura) {
        return new EffectSlotConfig(effect, amplifier, true, aura);
    }

    private static ResourceKey<BeaconEffectDef> effectKey(String path) {
        return ResourceKey.create(BPRegistryKeys.EFFECT,
                ResourceLocation.fromNamespaceAndPath("portablebeacons", path));
    }
}
