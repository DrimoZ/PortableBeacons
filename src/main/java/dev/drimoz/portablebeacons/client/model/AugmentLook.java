package dev.drimoz.portablebeacons.client.model;

import com.mojang.serialization.MapCodec;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.item.AugmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * How an augment stack decides which glyph to draw and what colour to draw it.
 *
 * <p>Both used to be one thing: a {@code model_data} integer in the registry entry, exposed as a
 * model predicate, with the model file listing overrides for 1..7. That meant the array order in
 * the model provider and the numbers in seven JSON files had to agree, and when they drifted an
 * augment simply rendered as the wrong glyph — no error anywhere.
 *
 * <p>The 1.21.4 model system selects on values, not just numbers, so the integer is unnecessary:
 * the model selects on a value. There is nothing left to keep in sync.
 *
 * <p>The value is the <em>glyph</em>, not the augment. Selecting on the augment's own key meant a
 * datapack augment could never have an icon: item models are fixed when resources load, before any
 * world's data exists, so no case could ever name it. Selecting on a glyph name lets it borrow any
 * shipped shape by saying {@code "glyph": "bolt"} in its JSON.
 */
public final class AugmentLook {

    /**
     * {@code portablebeacons:augment_glyph} — the glyph a stack draws: the one its augment names, or
     * for the mod's own augments the one named after them. Null - the bare casing - otherwise.
     */
    public static final class GlyphProperty implements SelectItemModelProperty<String> {

        public static final GlyphProperty INSTANCE = new GlyphProperty();
        public static final SelectItemModelProperty.Type<GlyphProperty, String> TYPE =
                SelectItemModelProperty.Type.create(MapCodec.unit(INSTANCE), com.mojang.serialization.Codec.STRING);

        @Override
        @Nullable
        public String get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed,
                          ItemDisplayContext context) {
            AugmentInstance instance = AugmentItem.instanceOf(stack);
            if (instance == null) {
                return null;
            }
            AugmentDef def = definitionOf(stack);
            if (def != null && def.glyph().isPresent()) {
                return def.glyph().get();
            }
            return instance.type().identifier().getNamespace().equals(BPRegistryKeys.MOD_ID)
                    ? instance.type().identifier().getPath()
                    : null;
        }

        @Override
        public com.mojang.serialization.Codec<String> valueCodec() {
            return com.mojang.serialization.Codec.STRING;
        }

        @Override
        public SelectItemModelProperty.Type<GlyphProperty, String> type() {
            return TYPE;
        }

        private GlyphProperty() {}
    }

    /**
     * {@code portablebeacons:augment_tier} — the tier in a stack, which picks the casing with that
     * many pips lit. Without it the icon could not show a tier at all: Range I and Range III looked
     * identical until hovered.
     */
    public static final class TierProperty implements SelectItemModelProperty<Integer> {

        public static final TierProperty INSTANCE = new TierProperty();
        public static final SelectItemModelProperty.Type<TierProperty, Integer> TYPE =
                SelectItemModelProperty.Type.create(MapCodec.unit(INSTANCE), com.mojang.serialization.Codec.INT);

        @Override
        @Nullable
        public Integer get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner,
                           int seed, ItemDisplayContext context) {
            AugmentInstance instance = AugmentItem.instanceOf(stack);
            return instance == null ? null : instance.tier();
        }

        @Override
        public com.mojang.serialization.Codec<Integer> valueCodec() {
            return com.mojang.serialization.Codec.INT;
        }

        @Override
        public SelectItemModelProperty.Type<TierProperty, Integer> type() {
            return TYPE;
        }

        private TierProperty() {}
    }

    /**
     * {@code portablebeacons:augment_colour} — tints the shared texture from the registry entry.
     *
     * <p>Item colours became tint sources declared by the model rather than handlers registered in
     * code, so this now travels with the model it tints.
     */
    public static final class Tint implements ItemTintSource {

        public static final Tint INSTANCE = new Tint();
        public static final MapCodec<Tint> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public int calculate(ItemStack stack, @Nullable ClientLevel level,
                             @Nullable LivingEntity owner) {
            AugmentDef def = definitionOf(stack);
            // The alpha channel is honoured when tinting, so a plain 0xRRGGBB from the JSON renders
            // the item fully transparent. Force it opaque.
            return 0xFF000000 | (def == null ? 0xFFFFFF : def.color());
        }

        @Override
        public MapCodec<Tint> type() {
            return CODEC;
        }

        private Tint() {}
    }

    @Nullable
    private static AugmentDef definitionOf(ItemStack stack) {
        AugmentInstance instance = AugmentItem.instanceOf(stack);
        if (instance == null || Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.registryAccess()
                .lookupOrThrow(BPRegistryKeys.AUGMENT)
                .get(instance.type())
                .map(Holder::value)
                .orElse(null);
    }

    private AugmentLook() {}
}
