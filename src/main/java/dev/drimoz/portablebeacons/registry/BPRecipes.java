package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.item.BeaconUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BPRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PortableBeacons.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BeaconUpgradeRecipe>> BEACON_UPGRADE =
            SERIALIZERS.register("beacon_upgrade", () -> new RecipeSerializer<>(
                    BeaconUpgradeRecipe.MAP_CODEC, BeaconUpgradeRecipe.STREAM_CODEC));

    private BPRecipes() {}
}
