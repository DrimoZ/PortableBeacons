package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * One entry of the {@code portablebeacons:tier} datapack registry: the base stats of a beacon item,
 * before augments.
 *
 * <p>Ranges are deliberately far below the vanilla beacon's 20/30/40/50: a beacon that follows you
 * is worth much more than a fixed one at equal range.
 *
 * @param level        1..4, used for ordering and for {@code min_tier} checks
 * @param effectSlots  how many effects may be configured
 * @param augmentSlots how many augment slots are unlocked (of the four the item carries)
 * @param baseRange    aura radius in blocks; ignored by effects set to {@link AuraMode#SELF}
 * @param fuelCapacity internal buffer in fuel units
 * @param maxAmplifier highest amplifier reachable without an Amplification augment
 * @param auraRank     highest {@link AuraMode} rank this beacon offers unaided; an Attunement
 *                     augment adds to it. 0 is self only, which is what a tier gets by default —
 *                     sharing is meant to be earned, either by tier or by augment
 */
public record BeaconTierDef(
        int level,
        int effectSlots,
        int augmentSlots,
        double baseRange,
        int fuelCapacity,
        int maxAmplifier,
        int auraRank,
        List<ResourceKey<BeaconEffectDef>> effectPool
) {
    public static final Codec<BeaconTierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4).fieldOf("level").forGetter(BeaconTierDef::level),
            Codec.intRange(0, BeaconStats.MAX_EFFECT_SLOTS).fieldOf("effect_slots").forGetter(BeaconTierDef::effectSlots),
            Codec.intRange(0, 4).fieldOf("augment_slots").forGetter(BeaconTierDef::augmentSlots),
            Codec.DOUBLE.fieldOf("base_range").forGetter(BeaconTierDef::baseRange),
            Codec.INT.fieldOf("fuel_capacity").forGetter(BeaconTierDef::fuelCapacity),
            Codec.intRange(0, 3).optionalFieldOf("max_amplifier", 0)
                    .forGetter(BeaconTierDef::maxAmplifier),
            Codec.intRange(0, 3).optionalFieldOf("aura_rank", 0)
                    .forGetter(BeaconTierDef::auraRank),
            ResourceKey.codec(BPRegistryKeys.EFFECT).listOf()
                    .optionalFieldOf("effect_pool", List.of()).forGetter(BeaconTierDef::effectPool)
    ).apply(i, BeaconTierDef::new));

    public BeaconTierDef {
        effectPool = List.copyOf(effectPool);
    }

    /**
     * Whether this beacon may project the given effect.
     *
     * <p>An empty pool means "anything the effect registry allows", which is what a datapack gets
     * for free; the shipped tiers all declare one explicitly so that a themed beacon cannot quietly
     * inherit the standard list.
     */
    public boolean allows(ResourceKey<BeaconEffectDef> effect) {
        return effectPool.isEmpty() || effectPool.contains(effect);
    }
}
