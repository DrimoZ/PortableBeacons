package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.registry.BPComponents;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Lets any energy mod's charger fill a beacon: forge energy goes in, fuel units come out.
 *
 * <p>Not NeoForge's {@code ItemAccessEnergyHandler}, which keeps its energy in a component of its
 * own. A beacon's fuel already has a home - the buffer in its {@link BeaconState} - and a second
 * store beside it would be two tanks pretending to be one. Here the energy <em>is</em> the buffer,
 * read and written at a fixed rate.
 *
 * <p>Insert only. A beacon is a consumer, and handing its fuel back out as energy would make it a
 * battery that also happens to grant effects.
 */
public final class BeaconEnergyHandler implements EnergyHandler {

    private final ItemAccess access;
    private final Item validItem;
    private final int energyPerUnit;

    public BeaconEnergyHandler(ItemAccess access, int energyPerUnit) {
        this.access = access;
        this.validItem = access.getResource().getItem();
        this.energyPerUnit = energyPerUnit;
    }

    @Override
    public long getAmountAsLong() {
        return valid() ? (long) state().fuel() * energyPerUnit : 0;
    }

    @Override
    public long getCapacityAsLong() {
        return valid() ? (long) capacity() * energyPerUnit : 0;
    }

    /** Whole fuel units only: a charger offering 39 FE at 40 per unit is refused rather than rounded up. */
    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0 || !valid() || access.getAmount() != 1) {
            return 0;
        }
        BeaconState state = state();
        int room = capacity() - state.fuel();
        int units = Math.min(amount / energyPerUnit, room);
        if (units <= 0) {
            return 0;
        }
        ItemResource filled = access.getResource().with(BPComponents.BEACON.get(),
                state.withFuel(state.fuel() + units));
        return access.exchange(filled, 1, transaction) == 1 ? units * energyPerUnit : 0;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        return 0;
    }

    private boolean valid() {
        return access.getResource().is(validItem);
    }

    private BeaconState state() {
        return PortableBeaconItem.stateOf(access.getResource().toStack());
    }

    /**
     * The cached capacity when there is one, worked out from the registries when there is not.
     *
     * <p>A beacon that has never been ticked or opened - fresh from the crafting table - has none
     * cached, and is exactly the one most likely to go straight into a charger. Working it out
     * needs the datapack registries, which only the server has here; the client only ever reads
     * the amount for display, and the cached figure is enough for that.
     */
    private int capacity() {
        BeaconState state = state();
        if (state.capacity() > 0) {
            return state.capacity();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        ItemStack stack = access.getResource().toStack();
        if (server == null || !(stack.getItem() instanceof PortableBeaconItem item)) {
            return 0;
        }
        RegistryAccess registries = server.registryAccess();
        BeaconTierDef tier = BPLookups.tier(registries, item);
        return tier == null ? 0 : BeaconResolver.resolve(tier, BPLookups.installedAugments(stack),
                BPLookups.augments(registries)).fuelCapacity();
    }
}
