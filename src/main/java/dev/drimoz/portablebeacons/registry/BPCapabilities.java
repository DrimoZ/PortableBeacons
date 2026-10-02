package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.item.BeaconEnergyHandler;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;

/**
 * Backs each beacon's augment and fuel slots with vanilla's {@code minecraft:container} component.
 *
 * <p>{@link ItemAccessItemHandler} writes every change straight back through the {@code ItemAccess}
 * it was handed, which is what keeps a beacon sitting in a chest, an ender chest or another
 * player's inventory from ever holding a stale copy of its own contents.
 */
@EventBusSubscriber(modid = PortableBeacons.MOD_ID)
public final class BPCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        for (DeferredItem<PortableBeaconItem> beacon : BPItems.beacons()) {
            event.registerItem(
                    Capabilities.Item.ITEM,
                    (stack, access) -> new ItemAccessItemHandler(
                            access, DataComponents.CONTAINER, PortableBeaconItem.CONTAINER_SIZE),
                    beacon.get());
            // Energy as a second way in, for any mod's charger. Asked for every time rather than
            // decided at registration, so the server config - which loads later - is what decides.
            event.registerItem(
                    Capabilities.Energy.ITEM,
                    (stack, access) -> BPConfig.energyPerFuelUnit() > 0
                            ? new BeaconEnergyHandler(access, BPConfig.energyPerFuelUnit())
                            : null,
                    beacon.get());
        }
    }

    private BPCapabilities() {}
}
