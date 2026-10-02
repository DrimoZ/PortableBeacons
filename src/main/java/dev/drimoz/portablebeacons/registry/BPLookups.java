package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.core.AugmentDef;
import dev.drimoz.portablebeacons.core.AugmentInstance;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconEffectDef;
import dev.drimoz.portablebeacons.core.FuelDef;
import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconStats;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bridge between the pure {@code core} layer and the live registries.
 *
 * <p>Both sides may call this: the four registries are network-synced, so the client resolves the
 * same numbers the server charges instead of asking for them.
 */
public final class BPLookups {

    public static BeaconResolver.Lookup<AugmentDef> augments(RegistryAccess access) {
        Registry<AugmentDef> registry = access.lookupOrThrow(BPRegistryKeys.AUGMENT);
        return registry::getOptional;
    }

    public static BeaconResolver.Lookup<BeaconEffectDef> effects(RegistryAccess access) {
        Registry<BeaconEffectDef> registry = access.lookupOrThrow(BPRegistryKeys.EFFECT);
        return registry::getOptional;
    }

    @Nullable
    public static BeaconTierDef tier(RegistryAccess access, PortableBeaconItem item) {
        return access.lookupOrThrow(BPRegistryKeys.TIER).get(item.tier())
                .map(Holder::value)
                .orElse(null);
    }

    /**
     * Effect keys in a stable order.
     *
     * <p>The GUI addresses effects by index in this list, because {@code clickMenuButton} can only
     * carry an int. Sorting by id is what makes client and server agree on that index without
     * inventing a second packet.
     */
    public static List<ResourceKey<BeaconEffectDef>> sortedEffectKeys(RegistryAccess access) {
        return access.lookupOrThrow(BPRegistryKeys.EFFECT).registryKeySet().stream()
                .sorted(Comparator.comparing(key -> key.identifier().toString()))
                .toList();
    }

    /** Fuel units the given item is worth, or 0 if it is not fuel. */
    public static int fuelValue(HolderLookup.Provider access, Item item) {
        // An item named directly wins over one matched through a tag, so a beacon can price a single
        // metal without having to exclude it from whatever convention tag it belongs to.
        int viaTag = 0;
        for (FuelDef def : (Iterable<FuelDef>) access.lookupOrThrow(BPRegistryKeys.FUEL).listElements()
                .map(Holder::value)::iterator) {
            if (!def.matches(item)) {
                continue;
            }
            if (def.item().isPresent()) {
                return def.units();
            }
            viaTag = Math.max(viaTag, def.units());
        }
        return viaTag;
    }

    /**
     * A beacon's stats as the server charges them: its tier, its augments, then the server's rules.
     *
     * <p>The one way to get them. The ticker, the screen, the item's tooltip each used to resolve on
     * their own, which was fine while the answer depended on the datapack alone; once a server
     * config scales costs and caps reach, any of them skipping the rules would show one price and
     * charge another.
     *
     * @return null for a stack that is not a beacon, or whose tier the datapack does not define
     */
    @Nullable
    public static BeaconStats stats(ItemStack beaconStack, HolderLookup.Provider registries) {
        if (!(beaconStack.getItem() instanceof PortableBeaconItem item)) {
            return null;
        }
        BeaconTierDef tier = registries.lookup(BPRegistryKeys.TIER)
                .flatMap(lookup -> lookup.get(item.tier()))
                .map(Holder::value)
                .orElse(null);
        if (tier == null) {
            return null;
        }
        BeaconResolver.Lookup<AugmentDef> augments = key -> registries.lookup(BPRegistryKeys.AUGMENT)
                .flatMap(lookup -> lookup.get(key))
                .map(Holder::value);
        return BeaconResolver.resolve(tier, installedAugments(beaconStack), augments)
                .withServerRules(BPConfig.fuelCostMultiplier(), BPConfig.maxAuraRange());
    }

    /**
     * Fuel units sitting in a beacon's fuel slot, not yet burned into its buffer.
     *
     * <p>One place for the three that need it - the ticker's warning, the screen's runtime and the
     * item's tooltip - because the tooltip used to leave it out and promise a fraction of the time
     * the screen did.
     */
    public static int reserveUnits(ItemStack beaconStack, HolderLookup.Provider access) {
        ResourceHandler<ItemResource> handler = handlerOf(beaconStack);
        if (handler == null || handler.size() <= PortableBeaconItem.FUEL_SLOT) {
            return 0;
        }
        ItemResource fuel = handler.getResource(PortableBeaconItem.FUEL_SLOT);
        return fuel.isEmpty() ? 0
                : fuelValue(access, fuel.getItem()) * handler.getAmountAsInt(PortableBeaconItem.FUEL_SLOT);
    }

    /** The augments currently installed in a beacon, read straight from its container component. */
    public static List<AugmentInstance> installedAugments(ItemStack beaconStack) {
        ResourceHandler<ItemResource> handler = handlerOf(beaconStack);
        if (handler == null) {
            return List.of();
        }
        List<AugmentInstance> found = new ArrayList<>(PortableBeaconItem.AUGMENT_SLOTS);
        int slots = Math.min(PortableBeaconItem.AUGMENT_SLOTS, handler.size() - 1);
        for (int slot = 0; slot < slots; slot++) {
            AugmentInstance instance = AugmentItem.instanceOf(
                    handler.getResource(PortableBeaconItem.augmentSlot(slot)));
            if (instance != null) {
                found.add(instance);
            }
        }
        return found;
    }

    /**
     * The beacon's own slots, as a resource handler.
     *
     * <p>{@link ItemAccess#forStack} mutates the stack it is given, which is the behaviour the
     * augment and fuel slots have always relied on. It throws on an empty stack rather than
     * returning nothing, so the guard is not optional.
     */
    @Nullable
    public static ResourceHandler<ItemResource> handlerOf(ItemStack beaconStack) {
        if (beaconStack.isEmpty()) {
            return null;
        }
        return ItemAccess.forStack(beaconStack).getCapability(Capabilities.Item.ITEM);
    }

    private BPLookups() {}
}
