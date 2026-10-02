package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Item models: twelve flat models, plus the augment's glyph table.
 *
 * <p>The table is the reason this is worth generating rather than typing. It selects on the
 * augment's registry key — see {@link AugmentLook} — so adding an augment here means naming it
 * once, not keeping an integer in step across two files.
 */
public class BPItemModelProvider extends ModelProvider {

    /** The built-in augments, by registry name. Order is presentational only. */
    private static final String[] AUGMENTS = {
            "range", "focus", "amplification", "efficiency", "capacity", "attunement", "discretion",
            "communion", "wellspring", "wayfarer", "sentinel", "vanguard", "prism", "recluse"
    };

    public BPItemModelProvider(PackOutput output) {
        super(output, PortableBeacons.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        BPItems.beacons().forEach(beacon ->
                itemModels.generateFlatItem(beacon.get(), ModelTemplates.FLAT_ITEM));

        List<SelectItemModel.SwitchCase<ResourceKey<AugmentDef>>> cases =
                new ArrayList<>(AUGMENTS.length);
        for (String name : AUGMENTS) {
            cases.add(ItemModelUtils.when(
                    ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id(name)),
                    byTier(tier -> augmentModel(itemModels, name, tier))));
        }

        // The fallback has no glyph: what a stack with no component - or a datapack augment with
        // no texture of its own - looks like. It still shows its colour and its tier.
        itemModels.itemModelOutput.accept(BPItems.AUGMENT.get(), ItemModelUtils.select(
                AugmentLook.TypeProperty.INSTANCE,
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
