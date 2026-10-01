package dev.drimoz.portablebeacons.datagen;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import dev.drimoz.portablebeacons.item.BeaconUpgradeRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Map;

/**
 * A shaped recipe whose result carries data components.
 *
 * <p>Vanilla's {@code ShapedRecipeBuilder} builds its result from an {@link ItemLike}, so it cannot
 * express "one augment item, of this type, at this tier" - and thirteen of this mod's twenty
 * recipes are exactly that. Without this, datagen would have covered half of them and left the
 * rest as hand-written JSON, which is worse than either extreme: two places to look.
 */
final class ComponentShapedRecipe {

    static void save(RecipeOutput output, HolderLookup.Provider registries, ResourceLocation id,
                     ItemStack result, ItemLike unlockedBy,
                     Map<Character, Ingredient> key, String... pattern) {
        save(output, id, unlockedBy, new ShapedRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(key, pattern), result));
    }

    /** The same, as a {@link BeaconUpgradeRecipe}: the beacon in the grid keeps what it carried. */
    static void saveUpgrade(RecipeOutput output, HolderLookup.Provider registries, ResourceLocation id,
                            ItemStack result, ItemLike unlockedBy,
                            Map<Character, Ingredient> key, String... pattern) {
        save(output, id, unlockedBy, new BeaconUpgradeRecipe("", CraftingBookCategory.MISC,
                ShapedRecipePattern.of(key, pattern), result, true));
    }

    private static void save(RecipeOutput output, ResourceLocation id, ItemLike unlockedBy, ShapedRecipe recipe) {

        // The same unlock advancement vanilla's builder writes: without one the recipe never shows
        // up in the recipe book, which is the only way to find it without JEI.
        Advancement.Builder advancement = Advancement.Builder.recipeAdvancement()
                .addCriterion("has_material", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(unlockedBy)))
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
                .rewards(AdvancementRewards.Builder.recipe(id))
                .requirements(AdvancementRequirements.Strategy.OR);

        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/misc/")));
    }

    private ComponentShapedRecipe() {}
}
