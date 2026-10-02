package dev.drimoz.portablebeacons.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A tier's pool: effects named one by one, and {@code #pools} any effect can join from its own file. */
class TierPoolTest {

    private static final ResourceKey<BeaconEffectDef> SPEED =
            ResourceKey.create(BPRegistryKeys.EFFECT, BPRegistryKeys.id("speed"));
    private static final ResourceKey<BeaconEffectDef> FLIGHT =
            ResourceKey.create(BPRegistryKeys.EFFECT, BPRegistryKeys.id("flight"));

    private static final BeaconEffectDef IN_STANDARD = new BeaconEffectDef(null, 1.0, 0, 1, 2.0, List.of("standard"));
    private static final BeaconEffectDef IN_NO_POOL = new BeaconEffectDef(null, 1.0, 0, 1, 2.0);

    private static BeaconTierDef parse(String pool) {
        String json = "{ \"level\": 1, \"effect_slots\": 1, \"augment_slots\": 0, \"base_range\": 0.0,"
                + " \"fuel_capacity\": 100, \"effect_pool\": " + pool + " }";
        return BeaconTierDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void idsAndPoolsAreReadFromOneList() {
        BeaconTierDef tier = parse("[ \"#standard\", \"portablebeacons:speed\" ]");
        assertEquals(List.of(SPEED), tier.effectPool());
        assertEquals(List.of("standard"), tier.effectPoolTags());
    }

    /** The point of pools: a new effect joins every tier that lists its pool, with no tier edited. */
    @Test
    void anEffectInAListedPoolIsAllowed() {
        BeaconTierDef tier = parse("[ \"#standard\" ]");
        assertTrue(tier.allows(FLIGHT, IN_STANDARD));
        assertFalse(tier.allows(FLIGHT, IN_NO_POOL));
    }

    @Test
    void anEffectNamedOutrightNeedsNoPool() {
        assertTrue(parse("[ \"portablebeacons:speed\" ]").allows(SPEED, IN_NO_POOL));
    }

    @Test
    void anEmptyPoolStillAllowsEverything() {
        assertTrue(parse("[]").allows(FLIGHT, IN_NO_POOL));
    }

    @Test
    void anEntryThatIsNeitherIsRejectedWhenRead() {
        String json = "{ \"level\": 1, \"effect_slots\": 1, \"augment_slots\": 0, \"base_range\": 0.0,"
                + " \"fuel_capacity\": 100, \"effect_pool\": [ \"Not An Id!\" ] }";
        assertTrue(BeaconTierDef.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).isError());
    }
}
