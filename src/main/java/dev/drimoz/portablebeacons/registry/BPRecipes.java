package dev.drimoz.portablebeacons.registry;

import com.mojang.serialization.MapCodec;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.item.BeaconUpgradeRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BPRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, PortableBeacons.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BeaconUpgradeRecipe>> BEACON_UPGRADE =
            SERIALIZERS.register("beacon_upgrade", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<BeaconUpgradeRecipe> codec() {
                    return BeaconUpgradeRecipe.MAP_CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, BeaconUpgradeRecipe> streamCodec() {
                    return BeaconUpgradeRecipe.STREAM_CODEC;
                }
            });

    private BPRecipes() {}
}
