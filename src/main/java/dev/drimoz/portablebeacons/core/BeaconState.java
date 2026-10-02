package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Everything a beacon item remembers, held in the single {@code portablebeacons:beacon} data component.
 *
 * <p>Slot contents (augments + fuel) are deliberately NOT here: they live in the vanilla
 * {@code minecraft:container} component so NeoForge's item-backed handler can write straight back
 * into the stack.
 *
 * <p>This record is the one place a 1.20.1 backport has to be rewritten — everything downstream
 * consumes it, nothing downstream knows how it is stored.
 *
 * <p><b>Once released, this shape is frozen.</b> Every field is {@code optionalFieldOf} with a
 * default, which makes adding a field safe: a beacon saved by an older version simply takes the
 * default. Removing one is safe too. What is not safe is changing what an existing field means or
 * what type it holds — that turns every beacon in every existing world into an item whose component
 * no longer parses, with no way to tell the old encoding from the new. A changed meaning gets a new
 * field name; the old one is read for a version or two and then dropped. This is why there is no
 * version number: absence of a field already is the version.
 *
 * @param effects configured effect slots, ordered; may be shorter than the tier's slot count
 * @param fuel    remaining fuel units in the internal buffer
 * @param active  master switch; false means nothing is applied and nothing is consumed
 * @param starved switched on but out of fuel: nothing is applied until fuel arrives, then it resumes
 *                on its own. Added after release, so it defaults to false like every field
 */
public record BeaconState(List<EffectSlotConfig> effects, int fuel, boolean active, int capacity,
                          boolean starved) {

    public static final BeaconState EMPTY = new BeaconState(List.of(), 0, false, 0);

    public static final Codec<BeaconState> CODEC = RecordCodecBuilder.create(i -> i.group(
            EffectSlotConfig.CODEC.listOf().optionalFieldOf("effects", List.of())
                    .forGetter(BeaconState::effects),
            Codec.INT.optionalFieldOf("fuel", 0).forGetter(BeaconState::fuel),
            Codec.BOOL.optionalFieldOf("active", false).forGetter(BeaconState::active),
            Codec.INT.optionalFieldOf("capacity", 0).forGetter(BeaconState::capacity),
            Codec.BOOL.optionalFieldOf("starved", false).forGetter(BeaconState::starved)
    ).apply(i, BeaconState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeaconState> STREAM_CODEC =
            StreamCodec.composite(
                    EffectSlotConfig.STREAM_CODEC.apply(ByteBufCodecs.list()), BeaconState::effects,
                    ByteBufCodecs.VAR_INT, BeaconState::fuel,
                    ByteBufCodecs.BOOL, BeaconState::active,
                    ByteBufCodecs.VAR_INT, BeaconState::capacity,
                    ByteBufCodecs.BOOL, BeaconState::starved,
                    BeaconState::new);

    public BeaconState {
        effects = List.copyOf(effects);
    }

    /** A beacon that is not starved, which is every beacon anyone constructs by hand. */
    public BeaconState(List<EffectSlotConfig> effects, int fuel, boolean active, int capacity) {
        this(effects, fuel, active, capacity, false);
    }

    public BeaconState withEffects(List<EffectSlotConfig> newEffects) {
        return new BeaconState(newEffects, fuel, active, capacity, starved);
    }

    public BeaconState withFuel(int newFuel) {
        return new BeaconState(effects, Math.max(0, newFuel), active, capacity, starved);
    }

    /** Switching it either way clears starvation: off is off, and on starts a fresh attempt. */
    public BeaconState withActive(boolean newActive) {
        return new BeaconState(effects, fuel, newActive, capacity, false);
    }

    public BeaconState withStarved(boolean newStarved) {
        return new BeaconState(effects, fuel, active, capacity, newStarved);
    }

    /**
     * Cached so the item can draw its own fuel bar.
     *
     * <p>{@code getBarWidth} gets no registry access, and without the capacity the bar had nothing
     * to be a fraction of - it fell back to durability and showed a full bar on an empty beacon.
     * Refreshed by {@link BeaconResolver#sanitize} on every action and every tick.
     */
    public BeaconState withCapacity(int newCapacity) {
        return new BeaconState(effects, fuel, active, newCapacity, starved);
    }

    public double fillRatio() {
        return capacity <= 0 ? 0.0 : Math.min(1.0, fuel / (double) capacity);
    }
}
