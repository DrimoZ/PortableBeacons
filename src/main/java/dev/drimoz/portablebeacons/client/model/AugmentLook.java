package dev.drimoz.portablebeacons.client.model;

import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.item.AugmentItem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * How an augment stack decides which glyph to draw, which casing, and what colour.
 *
 * <p>1.21.1 item models can only select on a number, so the glyph and the tier travel as one:
 * {@code glyph slot * 4 + tier}, with slot 0 the bare casing. The model lists one override per
 * value, in ascending order, and the game keeps the last one the value reaches - which, the values
 * being whole numbers, is the exact one. The 26.1 branch selects on the glyph's name instead.
 *
 * <p>The value is the <em>glyph</em>, not the augment, so a datapack augment can borrow any shipped
 * shape by saying {@code "glyph": "bolt"} in its JSON: item models are fixed when resources load,
 * before any world's data exists, so no model could ever name the augment itself.
 */
public final class AugmentLook {

    public static final ResourceLocation PROPERTY = BPRegistryKeys.id("augment_look");

    /**
     * Every glyph a model exists for: one per built-in augment, named after it, then generic shapes
     * for datapack augments to borrow. Must match {@code tools/GenerateTextures.java}.
     */
    public static final List<String> GLYPHS = List.of(
            "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion",
            "communion", "wellspring", "wayfarer", "sentinel", "vanguard", "prism", "recluse",
            "star", "bolt", "heart", "gem", "shield", "leaf");

    public static final int MAX_TIER = 3;

    /** The predicate value for a glyph slot (0 = none, n = {@code GLYPHS[n - 1]}) and a tier. */
    public static int code(int glyphSlot, int tier) {
        return glyphSlot * (MAX_TIER + 1) + tier;
    }

    public static float look(ItemStack stack) {
        AugmentInstance instance = AugmentItem.instanceOf(stack);
        if (instance == null) {
            return 0.0F;
        }
        AugmentDef def = definitionOf(instance);
        String glyph = def != null && def.glyph().isPresent()
                ? def.glyph().get()
                : instance.type().location().getNamespace().equals(BPRegistryKeys.MOD_ID)
                        ? instance.type().location().getPath()
                        : null;
        return code(GLYPHS.indexOf(glyph) + 1, Math.clamp(instance.tier(), 1, MAX_TIER));
    }

    /**
     * Only the screen layer takes the augment's colour: tinting the casing or the white glyph is
     * what made the old icons all one muddy colour.
     */
    public static int tint(ItemStack stack, int layer) {
        if (layer != 1) {
            return -1;
        }
        AugmentInstance instance = AugmentItem.instanceOf(stack);
        AugmentDef def = instance == null ? null : definitionOf(instance);
        // The alpha channel is honoured when tinting, so a plain 0xRRGGBB from the JSON renders the
        // item fully transparent. Force it opaque.
        return 0xFF000000 | (def == null ? 0xFFFFFF : def.color());
    }

    @Nullable
    private static AugmentDef definitionOf(AugmentInstance instance) {
        if (Minecraft.getInstance().level == null) {
            return null;
        }
        return Minecraft.getInstance().level.registryAccess()
                .registryOrThrow(BPRegistryKeys.AUGMENT).get(instance.type());
    }

    private AugmentLook() {}
}
