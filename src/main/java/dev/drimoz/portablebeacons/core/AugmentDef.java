package dev.drimoz.portablebeacons.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Optional;

/**
 * One entry of the {@code portablebeacons:augment} datapack registry.
 * <p>
 * A single registered item ({@code portablebeacons:augment}) carries a reference to one of these plus a
 * tier, so a datapack can introduce a brand new augment without any code — which would be
 * impossible if each augment were its own registered item.
 *
 * <pre>{@code
 * {
 *   "max_tier": 3,
 *   "color": 5636095,
 *   "operations": [
 *     { "type": "add_range", "values": [4.0, 8.0, 12.0] }
 *   ]
 * }
 * }</pre>
 *
 * @param maxTier    highest tier this augment exists in (1..3)
 * @param color      tint applied to the augment texture; alpha is forced opaque at render time
 * @param operations modifiers applied to the beacon's resolved stats
 * @param glyph      which glyph its icon draws - one of the shipped shapes, by name. Lets a
 *                   datapack augment look like itself without a resource pack; a built-in augment
 *                   needs none, it draws the glyph named after it
 */
public record AugmentDef(int maxTier, int color, List<Operation> operations, Optional<String> glyph) {

    public static final Codec<AugmentDef> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 3).optionalFieldOf("max_tier", 3).forGetter(AugmentDef::maxTier),
            Codec.INT.optionalFieldOf("color", 0xFFFFFF).forGetter(AugmentDef::color),
            Operation.CODEC.listOf().fieldOf("operations").forGetter(AugmentDef::operations),
            Codec.STRING.optionalFieldOf("glyph").forGetter(AugmentDef::glyph)
    ).apply(i, AugmentDef::new));

    /** An augment with no glyph of its own: a built-in one draws the glyph named after it. */
    public AugmentDef(int maxTier, int color, List<Operation> operations) {
        this(maxTier, color, operations, Optional.empty());
    }

    /**
     * A modifier with one value per augment tier. Indexing by tier rather than defining three
     * separate entries keeps "Range I/II/III" a single JSON file.
     *
     * @param values value for tier 1, 2, 3 — must hold at least {@code maxTier} entries
     */
    public record Operation(Type type, List<Double> values, Optional<ResourceKey<BeaconEffectDef>> effect) {
        public static final Codec<Operation> CODEC = RecordCodecBuilder.<Operation>create(i -> i.group(
                Type.CODEC.fieldOf("type").forGetter(Operation::type),
                Codec.DOUBLE.listOf().fieldOf("values").forGetter(Operation::values),
                ResourceKey.codec(BPRegistryKeys.EFFECT).optionalFieldOf("effect").forGetter(Operation::effect)
        ).apply(i, Operation::new)).validate(Operation::targetedHaveATarget);

        /** An operation on the beacon as a whole, which is every one but the targeted types. */
        public Operation(Type type, List<Double> values) {
            this(type, values, Optional.empty());
        }

        /**
         * Rejected when read, with the file's name, rather than silently doing nothing: a targeted
         * operation without its effect would be an augment that changes nothing and says it does.
         */
        private static DataResult<Operation> targetedHaveATarget(Operation op) {
            if (op.type().targeted() && op.effect().isEmpty()) {
                return DataResult.error(() -> op.type().getSerializedName() + " needs an \"effect\"");
            }
            return DataResult.success(op);
        }

        /** Value for a 1-based augment tier, clamped to what the JSON actually declares. */
        public double valueFor(int tier) {
            if (values.isEmpty()) {
                return 0.0;
            }
            return values.get(Math.clamp(tier - 1, 0, values.size() - 1));
        }
    }

    public enum Type implements StringRepresentable {
        ADD_RANGE("add_range"),
        ADD_EFFECT_SLOT("add_effect_slot"),
        ADD_AMPLIFIER("add_amplifier"),
        MUL_FUEL("mul_fuel"),
        MUL_CAPACITY("mul_capacity"),
        /** Unlocks aura modes whose {@code minTier} the beacon alone would not satisfy. */
        UNLOCK_AURA("unlock_aura"),
        /**
         * Hides the swirl at 1, and the status icon as well at 2.
         *
         * <p>The first operation that is not an amount: the value picks a behaviour rather than
         * scaling one, which is why the resolver reads it as a threshold.
         */
        HIDE_EFFECTS("hide_effects"),
        /**
         * Multiplies the sharing surcharge only, leaving what an effect costs you unchanged.
         *
         * <p>The first operation that touches the economy rather than a stat: every other one makes
         * a number bigger, this one makes a strategy affordable.
         */
        MUL_AURA_COST("mul_aura_cost"),
        /**
         * Runs this many effects for nothing — the most expensive ones, so the value does not
         * depend on the order the player happened to configure them in.
         */
        FREE_EFFECT_SLOT("free_effect_slot"),
        /**
         * Multiplies cost while the carrier is moving. Its counterpart applies while standing
         * still, so an augment can favour either travelling or holding a position.
         */
        MUL_COST_MOVING("mul_cost_moving"),
        MUL_COST_STILL("mul_cost_still"),
        /**
         * Raises one effect's level ceiling, named by the operation's {@code effect}: an augment
         * that specialises - Speed one level higher - rather than lifting everything at once the way
         * Amplification does. The effect's own {@code max_amplifier} still binds.
         */
        ADD_EFFECT_AMPLIFIER("add_effect_amplifier"),
        /** Multiplies one effect's cost, named by {@code effect}: a discount with a subject. */
        MUL_EFFECT_COST("mul_effect_cost");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        /** Whether this type acts on one effect, and so needs the operation's {@code effect}. */
        public boolean targeted() {
            return this == ADD_EFFECT_AMPLIFIER || this == MUL_EFFECT_COST;
        }

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
