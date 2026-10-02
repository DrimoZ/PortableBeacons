package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.data.PackOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Item models: the beacons' 3D model with each one's faces, plus the augment's glyph table.
 *
 * <p>The table is the reason this is worth generating rather than typing. It selects on the
 * glyph name - see {@link AugmentLook} - so a shape added here is one any augment, built-in or
 * from a datapack, can draw by naming it.
 */
public class BPItemModelProvider extends ModelProvider {

    /**
     * Every glyph a model exists for: one per built-in augment, named after it, then generic shapes
     * for datapack augments to borrow. Must match {@code tools/GenerateTextures.java}.
     */
    private static final String[] GLYPHS = {
            "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion",
            "communion", "wellspring", "wayfarer", "sentinel", "vanguard", "prism", "recluse",
            "star", "bolt", "heart", "gem", "shield", "leaf"
    };

    private static final TextureSlot PARTS = TextureSlot.create("parts");
    /** Hand-written in resources: its four cuboids are geometry, not a pattern worth generating. */
    private static final ModelTemplate BEACON = new ModelTemplate(
            Optional.of(BPRegistryKeys.id("item/portable_beacon")), Optional.empty(), PARTS);

    public BPItemModelProvider(PackOutput output) {
        super(output, PortableBeacons.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // The beacons are one 3D model, portable_beacon.json, each wearing its own sheet of faces.
        BPItems.beacons().forEach(beacon -> itemModels.itemModelOutput.accept(beacon.get(),
                ItemModelUtils.plainModel(BEACON.create(beacon.get(),
                        new TextureMapping().put(PARTS, texture("beacon/" + beacon.getId().getPath())),
                        itemModels.modelOutput))));

        List<SelectItemModel.SwitchCase<String>> cases = new ArrayList<>(GLYPHS.length);
        for (String name : GLYPHS) {
            cases.add(ItemModelUtils.when(name,
                    byTier(tier -> augmentModel(itemModels, name, tier))));
        }

        // The fallback has no glyph: what a stack with no component - or a datapack augment that
        // names no glyph, or one no model exists for - looks like. It still shows its colour and its tier.
        itemModels.itemModelOutput.accept(BPItems.AUGMENT.get(), ItemModelUtils.select(
                AugmentLook.GlyphProperty.INSTANCE,
                byTier(tier -> augmentModel(itemModels, null, tier)),
                cases));
    }

    /** One model per tier, chosen by the stack's tier; tier 1 for anything else. */
    private static ItemModel.Unbaked byTier(java.util.function.IntFunction<ItemModel.Unbaked> model) {
        // Each built once: building one writes its model file, and datagen rejects a second write.
        ItemModel.Unbaked first = model.apply(1);
        return ItemModelUtils.select(AugmentLook.TierProperty.INSTANCE, first,
                ItemModelUtils.when(1, first),
                ItemModelUtils.when(2, model.apply(2)),
                ItemModelUtils.when(3, model.apply(3)));
    }

    /**
     * Casing (with the tier's pips), screen, glyph - see {@code tools/GenerateTextures.java}.
     *
     * <p>Only the screen is tinted: tints apply per layer, and tinting the casing or the white glyph
     * by the augment's colour is what made the old icons all one muddy colour.
     */
    private static ItemModel.Unbaked augmentModel(ItemModelGenerators itemModels, @Nullable String glyph,
                                                  int tier) {
        Identifier id = BPRegistryKeys.id("item/augment" + (glyph == null ? "" : "_" + glyph) + "_" + tier);
        TextureMapping textures = new TextureMapping()
                .put(TextureSlot.LAYER0, texture("augment_casing_" + tier))
                .put(TextureSlot.LAYER1, texture("augment_screen"));
        if (glyph == null) {
            ModelTemplates.TWO_LAYERED_ITEM.create(id, textures, itemModels.modelOutput);
            return ItemModelUtils.tintedModel(id, ItemModelUtils.constantTint(-1), AugmentLook.Tint.INSTANCE);
        }
        ModelTemplates.THREE_LAYERED_ITEM.create(id,
                textures.put(TextureSlot.LAYER2, texture("augment_glyph_" + glyph)), itemModels.modelOutput);
        return ItemModelUtils.tintedModel(id, ItemModelUtils.constantTint(-1), AugmentLook.Tint.INSTANCE,
                ItemModelUtils.constantTint(-1));
    }

    private static Material texture(String name) {
        return new Material(BPRegistryKeys.id("item/" + name));
    }
}
