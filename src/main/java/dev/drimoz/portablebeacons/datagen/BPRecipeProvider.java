package dev.drimoz.portablebeacons.datagen;

import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Every recipe and its unlock advancement.
 *
 * <p>Written here rather than as JSON because the shapes repeat: four tiers and three themed beacons
 * share one pattern, and the augment tiers share two more. Forty hand-written files were forty
 * chances to mistype an item id that nothing would catch until a player noticed a missing recipe.
 */
public class BPRecipeProvider extends RecipeProvider {

    private final CompletableFuture<HolderLookup.Provider> registries;

    public BPRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.registries = registries;
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        HolderLookup.Provider lookup = registries.join();

        // Every tier consumes a Beacon block, which is what keeps the vanilla beacon on the
        // progression path instead of being replaced by its own portable version.
        beacon(output, lookup, BPItems.BEACON_I.get(), Map.of(
                'I', Ingredient.of(Items.IRON_INGOT),
                'B', Ingredient.of(Items.BEACON)), " I ", "IBI", " I ");
        beacon(output, lookup, BPItems.BEACON_II.get(), Map.of(
                'G', Ingredient.of(Items.GOLD_INGOT),
                'P', Ingredient.of(BPItems.BEACON_I.get()),
                'B', Ingredient.of(Items.BEACON)), " G ", "GPG", " B ");
        beacon(output, lookup, BPItems.BEACON_III.get(), Map.of(
                'S', Ingredient.of(Items.NETHER_STAR),
                'D', Ingredient.of(Items.DIAMOND),
                'P', Ingredient.of(BPItems.BEACON_II.get()),
                'B', Ingredient.of(Items.BEACON)), " S ", "DPD", " B ");
        beacon(output, lookup, BPItems.BEACON_IV.get(), Map.of(
                'S', Ingredient.of(Items.NETHER_STAR),
                'N', Ingredient.of(Items.NETHERITE_INGOT),
                'P', Ingredient.of(BPItems.BEACON_III.get()),
                'B', Ingredient.of(Items.BEACON)), " S ", "NPN", " B ");

        themed(output, lookup, BPItems.BEACON_CINDER.get(), Items.BLAZE_ROD, Items.MAGMA_CREAM);
        themed(output, lookup, BPItems.BEACON_VOID.get(), Items.DRAGON_BREATH, Items.ENDER_EYE);
        themed(output, lookup, BPItems.BEACON_TIDAL.get(), Items.HEART_OF_THE_SEA,
                Items.PRISMARINE_CRYSTALS);

        // Each augment tier is crafted from raw materials rather than by upgrading the previous
        // one: a vanilla ingredient matches on item and tag only, so "a tier-1 Range augment"
        // cannot be named as an ingredient.
        augment(output, lookup, "range", Items.ENDER_PEARL, 3);
        augment(output, lookup, "focus", Items.SPYGLASS, 1);
        augment(output, lookup, "amplification", Items.GLOWSTONE, 1);
        augment(output, lookup, "efficiency", Items.REDSTONE_BLOCK, 3);
        augment(output, lookup, "capacity", Items.GOLD_BLOCK, 3);
        augment(output, lookup, "attunement", Items.ECHO_SHARD, 2);
        augment(output, lookup, "discretion", Items.PHANTOM_MEMBRANE, 2);

        // Augments built on the economy rather than on a stat: they change which strategy is
        // affordable, so they compete with the seven above instead of extending them.
        augment(output, lookup, "communion", Items.GOAT_HORN, 2);
        augment(output, lookup, "wellspring", Items.NAUTILUS_SHELL, 1);
        augment(output, lookup, "wayfarer", Items.RABBIT_FOOT, 2);
        augment(output, lookup, "sentinel", Items.SHIELD, 2);

        // Combinations: several gains across different axes for one cost, so what they really buy
        // is a slot back - the scarcest thing a beacon has.
        augment(output, lookup, "vanguard", Items.END_ROD, 1);
        augment(output, lookup, "prism", Items.AMETHYST_CLUSTER, 1);
        augment(output, lookup, "recluse", Items.SCULK_SENSOR, 1);
    }

    private void beacon(RecipeOutput output, HolderLookup.Provider lookup, ItemLike result,
                      Map<Character, Ingredient> key, String... pattern) {
        if (key.containsKey('P')) {
            ComponentShapedRecipe.saveUpgrade(output, lookup, key(result), new ItemStack(result), Items.BEACON, key, pattern);
        } else {
            ComponentShapedRecipe.save(output, lookup, key(result), new ItemStack(result), Items.BEACON, key, pattern);
        }
    }

    private void themed(RecipeOutput output, HolderLookup.Provider lookup, ItemLike result,
                        ItemLike core, ItemLike material) {
        ComponentShapedRecipe.saveUpgrade(output, lookup, key(result), new ItemStack(result), core, Map.of(
                        'C', Ingredient.of(core),
                        'M', Ingredient.of(material),
                        'P', Ingredient.of(BPItems.BEACON_II.get()),
                        'B', Ingredient.of(Items.BEACON)),
                " C ", "MPM", " B ");
    }

    /**
     * Tier 1 is a ring of shards; higher tiers add amethyst blocks and then ender eyes, so the
     * escalation reads the same for every augment.
     */
    private void augment(RecipeOutput output, HolderLookup.Provider lookup, String name,
                         ItemLike material, int maxTier) {
        for (int tier = 1; tier <= maxTier; tier++) {
            ItemStack result = new ItemStack(BPItems.AUGMENT.get());
            result.set(BPComponents.AUGMENT.get(), new AugmentInstance(
                    ResourceKey.create(BPRegistryKeys.AUGMENT, BPRegistryKeys.id(name)), tier));

            Map<Character, Ingredient> key = switch (tier) {
                case 1 -> Map.of('A', Ingredient.of(Items.AMETHYST_SHARD),
                        'M', Ingredient.of(material));
                case 2 -> Map.of('B', Ingredient.of(Items.AMETHYST_BLOCK),
                        'A', Ingredient.of(Items.AMETHYST_SHARD),
                        'M', Ingredient.of(material));
                default -> Map.of('B', Ingredient.of(Items.AMETHYST_BLOCK),
                        'E', Ingredient.of(Items.ENDER_EYE),
                        'M', Ingredient.of(material));
            };
            String[] pattern = switch (tier) {
                case 1 -> new String[] {" A ", "AMA", " A "};
                case 2 -> new String[] {"BAB", "AMA", "BAB"};
                default -> new String[] {"BEB", "EME", "BEB"};
            };

            String id = tier == 1 ? "augment_" + name : "augment_" + name + "_" + tier;
            ComponentShapedRecipe.save(output, lookup, BPRegistryKeys.id(id), result, material,
                    key, pattern);
        }
    }

    private static net.minecraft.resources.ResourceLocation key(ItemLike item) {
        return BPRegistryKeys.id(net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(item.asItem()).getPath());
    }
}
