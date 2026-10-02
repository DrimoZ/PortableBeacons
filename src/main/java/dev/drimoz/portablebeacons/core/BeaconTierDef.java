package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of the {@code portablebeacons:tier} datapack registry: the base stats of a beacon item,
 * before augments.
 *
 * <p>Ranges are deliberately far below the vanilla beacon's 20/30/40/50: a beacon that follows you
 * is worth much more than a fixed one at equal range.
 *
 * @param level          1..4, used for ordering and for {@code min_tier} checks
 * @param effectSlots    how many effects may be configured
 * @param augmentSlots   how many augment slots are unlocked (of the four the item carries)
 * @param baseRange      aura radius in blocks; ignored by effects set to {@link AuraMode#SELF}
 * @param fuelCapacity   internal buffer in fuel units
 * @param maxAmplifier   highest amplifier reachable without an Amplification augment
 * @param auraRank       highest {@link AuraMode} rank this beacon offers unaided; an Attunement
 *                       augment adds to it. 0 is self only, which is what a tier gets by default —
 *                       sharing is meant to be earned, either by tier or by augment
 * @param effectPool     effects named one by one
 * @param effectPoolTags pools named by {@code #name}: any effect declaring one of them in its
 *                       {@code pools} belongs to this tier too
 */
public record BeaconTierDef(
        int level,
        int effectSlots,
        int augmentSlots,
        double baseRange,
        int fuelCapacity,
        int maxAmplifier,
        int auraRank,
        List<ResourceKey<BeaconEffectDef>> effectPool,
        List<String> effectPoolTags
) {
    /**
     * {@code effect_pool} as written: effect ids and {@code #pool} names in one list.
     *
     * <p>Pools exist so that adding an effect is one file. With ids alone, a datapack effect meant
     * for every standard tier had to be added to four tier files as well - and the tier that was
     * missed simply never offered it, with nothing to say why.
     */
    private record Pool(List<ResourceKey<BeaconEffectDef>> keys, List<String> tags) {
        static final Codec<Pool> CODEC = Codec.STRING.listOf().comapFlatMap(Pool::parse, Pool::write);

        static DataResult<Pool> parse(List<String> entries) {
            List<ResourceKey<BeaconEffectDef>> keys = new ArrayList<>();
            List<String> tags = new ArrayList<>();
            for (String entry : entries) {
                if (entry.startsWith("#")) {
                    tags.add(entry.substring(1));
                    continue;
                }
                Identifier id = Identifier.tryParse(entry);
                if (id == null) {
                    return DataResult.error(() -> "not an effect id or a #pool: " + entry);
                }
                keys.add(ResourceKey.create(BPRegistryKeys.EFFECT, id));
            }
            return DataResult.success(new Pool(keys, tags));
        }

        List<String> write() {
            List<String> entries = new ArrayList<>(keys.size() + tags.size());
            keys.forEach(key -> entries.add(key.identifier().toString()));
            tags.forEach(tag -> entries.add("#" + tag));
            return entries;
        }
    }

    public static final Codec<BeaconTierDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 4).fieldOf("level").forGetter(BeaconTierDef::level),
            Codec.intRange(0, BeaconStats.MAX_EFFECT_SLOTS).fieldOf("effect_slots").forGetter(BeaconTierDef::effectSlots),
            Codec.intRange(0, BeaconStats.MAX_AUGMENT_SLOTS).fieldOf("augment_slots").forGetter(BeaconTierDef::augmentSlots),
            Codec.DOUBLE.fieldOf("base_range").forGetter(BeaconTierDef::baseRange),
            Codec.INT.fieldOf("fuel_capacity").forGetter(BeaconTierDef::fuelCapacity),
            Codec.intRange(0, BeaconStats.MAX_AMPLIFIER).optionalFieldOf("max_amplifier", 0)
                    .forGetter(BeaconTierDef::maxAmplifier),
            Codec.intRange(0, 3).optionalFieldOf("aura_rank", 0)
                    .forGetter(BeaconTierDef::auraRank),
            Pool.CODEC.optionalFieldOf("effect_pool", new Pool(List.of(), List.of()))
                    .forGetter(tier -> new Pool(tier.effectPool, tier.effectPoolTags))
    ).apply(i, (level, slots, augments, range, capacity, amplifier, rank, pool) -> new BeaconTierDef(
            level, slots, augments, range, capacity, amplifier, rank, pool.keys(), pool.tags())));

    public BeaconTierDef {
        effectPool = List.copyOf(effectPool);
        effectPoolTags = List.copyOf(effectPoolTags);
    }

    /** A tier whose pool names effects only, which is every tier written before pools existed. */
    public BeaconTierDef(int level, int effectSlots, int augmentSlots, double baseRange, int fuelCapacity,
                         int maxAmplifier, int auraRank, List<ResourceKey<BeaconEffectDef>> effectPool) {
        this(level, effectSlots, augmentSlots, baseRange, fuelCapacity, maxAmplifier, auraRank, effectPool,
                List.of());
    }

    /**
     * Whether this beacon may project the given effect: named outright, or belonging to one of the
     * tier's pools.
     *
     * <p>An empty pool means "anything the effect registry allows", which is what a datapack gets
     * for free; the shipped tiers all declare one explicitly so that a themed beacon cannot quietly
     * inherit the standard list.
     *
     * @param def the effect's definition, which says which pools it belongs to; null when unknown,
     *            in which case only a pool naming it outright can admit it
     */
    public boolean allows(ResourceKey<BeaconEffectDef> effect, @Nullable BeaconEffectDef def) {
        if (effectPool.isEmpty() && effectPoolTags.isEmpty()) {
            return true;
        }
        if (effectPool.contains(effect)) {
            return true;
        }
        return def != null && def.pools().stream().anyMatch(effectPoolTags::contains);
    }
}
