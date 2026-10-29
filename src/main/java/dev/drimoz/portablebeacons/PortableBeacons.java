package dev.drimoz.portablebeacons;

import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPCreativeTabs;
import dev.drimoz.portablebeacons.registry.BPItems;
import dev.drimoz.portablebeacons.registry.BPMenus;
import dev.drimoz.portablebeacons.registry.BPRecipes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(PortableBeacons.MOD_ID)
public class PortableBeacons {

    public static final String MOD_ID = BPRegistryKeys.MOD_ID;

    public PortableBeacons(IEventBus modEventBus, ModContainer modContainer) {
        BPComponents.REGISTRAR.register(modEventBus);
        BPItems.ITEMS.register(modEventBus);
        BPCreativeTabs.TABS.register(modEventBus);
        BPMenus.MENUS.register(modEventBus);
        BPRecipes.SERIALIZERS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, BPConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, BPConfig.CLIENT_SPEC);
        modEventBus.addListener(PortableBeacons::addPacks);
    }

    /**
     * Effects past the vanilla beacon's, as a datapack a world opts into. FEATURE is what keeps it
     * off by default - BUILT_IN would switch it on in every new world, and an unrestricted pool by
     * default turns a utility mod into a cheat item.
     */
    private static void addPacks(AddPackFindersEvent event) {
        event.addPackFinders(BPRegistryKeys.id("datapacks/extended_effects"), PackType.SERVER_DATA,
                Component.translatable("portablebeacons.pack.extended_effects"),
                PackSource.FEATURE, false, Pack.Position.TOP);
    }
}
