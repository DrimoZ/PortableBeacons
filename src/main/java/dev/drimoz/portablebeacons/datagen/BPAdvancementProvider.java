package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPTags;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.world.item.Item;

import java.util.function.Consumer;

/**
 * A short advancement branch: get a beacon, kit it out, specialise.
 *
 * <p>Four entries, not a tree. The mod has one item family and no progression of its own beyond the
 * crafting ladder, so a long chain would be padding - and the recipe-unlock advancements that ship
 * alongside these are already hidden ones the player never sees.
 *
 * <p>Every trigger is "you have the item", which is the honest condition: the beacons are craftable,
 * but a modpack may hand them out, and an advancement that only fires on crafting would silently
 * never trigger there.
 */
public class BPAdvancementProvider implements AdvancementSubProvider {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver) {

        AdvancementHolder root = Advancement.Builder.advancement()
                .display(
                        BPItems.BEACON_I.get(),
                        title("root"),
                        description("root"),
                        // Reuses a vanilla background rather than shipping one: a bespoke tab
                        // texture for four advancements is not worth the kilobytes.
                        //
                        // The bare asset id, not a file path: ClientAsset.ResourceTexture adds the
                        // "textures/" prefix and the ".png" itself, so a full path became
                        // textures/textures/....png.png and drew the missing-texture checks.
                        Identifier.withDefaultNamespace("block/deepslate_bricks"),
                        AdvancementType.TASK,
                        true, true, false)
                .addCriterion("has_beacon", hasTaggedPack(registries.lookupOrThrow(Registries.ITEM)))
                .save(saver, id("root"));

        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.AUGMENT.get(), title("augmented"), description("augmented"),
                        null, AdvancementType.TASK, true, true, false)
                .addCriterion("has_augment", has(BPItems.AUGMENT.get()))
                .save(saver, id("augmented"));

        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.BEACON_IV.get(), title("tier_four"), description("tier_four"),
                        null, AdvancementType.CHALLENGE, true, true, false)
                .addCriterion("has_beacon_iv", has(BPItems.BEACON_IV.get()))
                .save(saver, id("tier_four"));

        // Any one of the three, because they are alternatives rather than a set to collect.
        Advancement.Builder.advancement()
                .parent(root)
                .display(BPItems.BEACON_TIDAL.get(), title("themed"), description("themed"),
                        null, AdvancementType.GOAL, true, true, false)
                .addCriterion("has_cinder", has(BPItems.BEACON_CINDER.get()))
                .addCriterion("has_void", has(BPItems.BEACON_VOID.get()))
                .addCriterion("has_tidal", has(BPItems.BEACON_TIDAL.get()))
                .requirements(net.minecraft.advancements.AdvancementRequirements.Strategy.OR)
                .save(saver, id("themed"));
    }

    /**
     * The tag predicate needs a registry lookup now, where it used to resolve the tag on its own.
     */
    private static Criterion<InventoryChangeTrigger.TriggerInstance> hasTaggedPack(
            HolderGetter<Item> items) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(items, BPTags.BEACONS));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    private static Component title(String name) {
        return Component.translatable("advancement." + PortableBeacons.MOD_ID + "." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancement." + PortableBeacons.MOD_ID + "." + name + ".description");
    }

    private static String id(String name) {
        return Identifier.fromNamespaceAndPath(PortableBeacons.MOD_ID, name).toString();
    }
}
