package dev.drimoz.portablebeacons.item;

import com.mojang.serialization.MapCodec;
import dev.drimoz.portablebeacons.registry.BPRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that turns one beacon into the next and keeps everything it carried.
 *
 * <p>A plain {@code crafting_shaped} builds its result from scratch, so upgrading a Beacon II
 * silently destroyed its installed augments, its fuel and its configured effects - the augments
 * alone cost shards and ender eyes. The result here is the input beacon's components moved onto the
 * new item. Whatever the new tier no longer permits is left to {@code BeaconResolver.sanitize},
 * which already runs on every pass and every action.
 *
 * <p>Its codecs are vanilla's shaped ones, mapped: the JSON is a shaped recipe's, field for field.
 */
public class BeaconUpgradeRecipe extends ShapedRecipe {

    public static final MapCodec<BeaconUpgradeRecipe> MAP_CODEC =
            ShapedRecipe.Serializer.CODEC.xmap(BeaconUpgradeRecipe::of, recipe -> recipe);

    public static final StreamCodec<RegistryFriendlyByteBuf, BeaconUpgradeRecipe> STREAM_CODEC =
            ShapedRecipe.Serializer.STREAM_CODEC.map(BeaconUpgradeRecipe::of, recipe -> recipe);

    public BeaconUpgradeRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern,
                               ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }

    private static BeaconUpgradeRecipe of(ShapedRecipe shaped) {
        // getResultItem ignores its argument here; the shaped recipe holds its result outright.
        return new BeaconUpgradeRecipe(shaped.getGroup(), shaped.category(), shaped.pattern,
                shaped.getResultItem(null), shaped.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof PortableBeaconItem) {
                return stack.transmuteCopy(result.getItem(), result.getCount());
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BPRecipes.BEACON_UPGRADE.get();
    }
}
