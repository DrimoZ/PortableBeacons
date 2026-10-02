package dev.drimoz.portablebeacons.registry;

import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.item.AugmentItem;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class BPItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PortableBeacons.MOD_ID);

    /**
     * Four distinct items rather than one item plus a tier component: recipes, tags and JEI all
     * stay trivial, and a player can tell two beacons apart in a chest at a glance.
     */
    public static final DeferredItem<PortableBeaconItem> BEACON_I =
            beacon("beacon_i", "tier_1", Rarity.COMMON);
    public static final DeferredItem<PortableBeaconItem> BEACON_II =
            beacon("beacon_ii", "tier_2", Rarity.UNCOMMON);
    public static final DeferredItem<PortableBeaconItem> BEACON_III =
            beacon("beacon_iii", "tier_3", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> BEACON_IV =
            beacon("beacon_iv", "tier_4", Rarity.EPIC);

    /**
     * Themed beacons: same machinery, different effect pool.
     *
     * <p>They exist entirely because a tier entry can declare which effects it accepts, so a
     * specialist beacon needs no code of its own - and a datapack can add more the same way. Each
     * trades the standard list for effects the beacon never offered, which is what makes carrying
     * one instead of a tier IV a real choice rather than a downgrade.
     */
    public static final DeferredItem<PortableBeaconItem> BEACON_CINDER =
            beacon("cinder_beacon", "nether", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> BEACON_VOID =
            beacon("void_beacon", "end", Rarity.RARE);
    public static final DeferredItem<PortableBeaconItem> BEACON_TIDAL =
            beacon("tidal_beacon", "tidal", Rarity.RARE);

    /**
     * Everything at the ceiling and nothing burnt: for building and for testing a datapack's effects
     * without feeding it. Creative tab only - it has no recipe. Its tier is ordinary data, so a
     * server that wants it costlier only has to override the file.
     */
    public static final DeferredItem<PortableBeaconItem> BEACON_CREATIVE =
            beacon("creative_beacon", "creative", Rarity.EPIC);

    /**
     * One registered augment item for every augment there will ever be — its identity comes from a
     * component pointing into the datapack registry, so a beacon can add new augments without code.
     */
    public static final DeferredItem<AugmentItem> AUGMENT =
            ITEMS.registerItem("augment", props -> new AugmentItem(props.stacksTo(1)));

    public static List<DeferredItem<PortableBeaconItem>> beacons() {
        return List.of(BEACON_I, BEACON_II, BEACON_III, BEACON_IV, BEACON_CINDER, BEACON_VOID, BEACON_TIDAL,
                BEACON_CREATIVE);
    }

    private static DeferredItem<PortableBeaconItem> beacon(String name, String tierPath, Rarity rarity) {
        ResourceKey<BeaconTierDef> tier =
                ResourceKey.create(BPRegistryKeys.TIER, BPRegistryKeys.id(tierPath));
        return ITEMS.registerItem(name, props ->
                new PortableBeaconItem(props.stacksTo(1).rarity(rarity), tier));
    }

    private BPItems() {}
}
