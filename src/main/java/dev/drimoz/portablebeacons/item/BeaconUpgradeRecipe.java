package dev.drimoz.portablebeacons.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.drimoz.portablebeacons.registry.BPRecipes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/**
 * A shaped recipe that turns one beacon into the next and keeps everything it carried.
 *
 * <p>A plain {@code crafting_shaped} builds its result from scratch, so upgrading a Beacon II
 * silently destroyed its installed augments, its fuel and its configured effects - the augments
 * alone cost shards and ender eyes. The result here is the input beacon's components moved onto the
 * new item, the way vanilla's transmute recipes carry a shulker box's contents through a dye.
 *
 * <p>Whatever the new tier no longer permits - an effect outside a themed beacon's pool, say - is
 * left to {@code BeaconResolver.sanitize}, which already runs on every pass and every action. Doing
 * it here too would be a second copy of the same rules.
 */
public class BeaconUpgradeRecipe extends ShapedRecipe {

    public static final MapCodec<BeaconUpgradeRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(o -> o.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(o -> o.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(o -> o.result)
    ).apply(i, BeaconUpgradeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeaconUpgradeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, o -> o.commonInfo,
                    CraftingRecipe.CraftingBookInfo.STREAM_CODEC, o -> o.bookInfo,
                    ShapedRecipePattern.STREAM_CODEC, o -> o.pattern,
                    ItemStackTemplate.STREAM_CODEC, o -> o.result,
                    BeaconUpgradeRecipe::new);

    /** Kept here as well: {@link ShapedRecipe} holds its own copy privately. */
    private final ItemStackTemplate result;

    public BeaconUpgradeRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo,
                               ShapedRecipePattern pattern, ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
        this.result = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof PortableBeaconItem) {
                return stack.transmuteCopy(result.item().value(), result.count());
            }
        }
        return super.assemble(input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        // ShapedRecipe narrows the return type to its own serializer, so ours is handed back under
        // that type. It only ever encodes this recipe, and through our own codec.
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) BPRecipes.BEACON_UPGRADE.get();
    }
}
