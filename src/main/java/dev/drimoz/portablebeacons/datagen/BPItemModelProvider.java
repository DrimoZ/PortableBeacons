package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.client.model.AugmentLook;
import dev.drimoz.portablebeacons.registry.BPItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item models: one flat model per beacon, plus the augment's override table.
 *
 * <p>The table is the reason this is worth generating rather than typing: one model per glyph and
 * tier, sixty-three of them, each selected by the value {@link AugmentLook} computes. A shape added
 * to {@link AugmentLook#GLYPHS} is one any augment, built-in or from a datapack, can draw by naming it.
 */
public class BPItemModelProvider extends ItemModelProvider {

    public BPItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, PortableBeacons.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        BPItems.beacons().forEach(beacon -> withExistingParent(beacon.getId().getPath(), mcLoc("item/generated"))
                .texture("layer0", modLoc("item/" + beacon.getId().getPath())));

        // The fallback has no glyph: what a stack with no component looks like. Overrides ascend,
        // because the game keeps the last one whose threshold the value reaches.
        ItemModelBuilder augment = augmentModel("augment", null, 1);
        for (int slot = 0; slot <= AugmentLook.GLYPHS.size(); slot++) {
            String glyph = slot == 0 ? null : AugmentLook.GLYPHS.get(slot - 1);
            for (int tier = 1; tier <= AugmentLook.MAX_TIER; tier++) {
                String name = "augment" + (glyph == null ? "" : "_" + glyph) + "_" + tier;
                augmentModel(name, glyph, tier);
                augment.override()
                        .predicate(AugmentLook.PROPERTY, AugmentLook.code(slot, tier))
                        .model(getExistingFile(modLoc("item/" + name)))
                        .end();
            }
        }
    }

    /** Casing (with the tier's pips), screen, glyph - see {@code tools/GenerateTextures.java}. */
    private ItemModelBuilder augmentModel(String name, String glyph, int tier) {
        ItemModelBuilder model = withExistingParent(name, mcLoc("item/generated"))
                .texture("layer0", modLoc("item/augment_casing_" + tier))
                .texture("layer1", modLoc("item/augment_screen"));
        return glyph == null ? model : model.texture("layer2", modLoc("item/augment_glyph_" + glyph));
    }
}
