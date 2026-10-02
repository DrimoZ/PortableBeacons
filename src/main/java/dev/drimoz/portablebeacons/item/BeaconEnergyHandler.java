package dev.drimoz.portablebeacons.item;

import dev.drimoz.portablebeacons.core.BeaconResolver;
import dev.drimoz.portablebeacons.core.BeaconState;
import dev.drimoz.portablebeacons.core.BeaconTierDef;
import dev.drimoz.portablebeacons.registry.BPLookups;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Lets any energy mod's charger fill a beacon: forge energy goes in, fuel units come out.
 *
 * <p>Not NeoForge's {@code ComponentEnergyStorage}, which keeps its energy in a component of its
 * own. A beacon's fuel already has a home - the buffer in its {@link BeaconState} - and a second
 * store beside it would be two tanks pretending to be one. Here the energy <em>is</em> the buffer,
 * read and written at a fixed rate.
 *
 * <p>Insert only. A beacon is a consumer, and handing its fuel back out as energy would make it a
 * battery that also happens to grant effects.
 */
public final class BeaconEnergyHandler implements IEnergyStorage {

    private final ItemStack stack;
    private final int energyPerUnit;

    public BeaconEnergyHandler(ItemStack stack, int energyPerUnit) {
        this.stack = stack;
        this.energyPerUnit = energyPerUnit;
    }

    /** Whole fuel units only: a charger offering 39 FE at 40 per unit is refused rather than rounded up. */
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        if (amount <= 0 || stack.getCount() != 1) {
            return 0;
        }
        BeaconState state = PortableBeaconItem.stateOf(stack);
        int units = Math.min(amount / energyPerUnit, capacity() - state.fuel());
        if (units <= 0) {
            return 0;
        }
        if (!simulate) {
            PortableBeaconItem.setState(stack, state.withFuel(state.fuel() + units));
        }
        return units * energyPerUnit;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, (long) PortableBeaconItem.stateOf(stack).fuel() * energyPerUnit);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, (long) capacity() * energyPerUnit);
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
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
        BeaconState state = PortableBeaconItem.stateOf(stack);
        if (state.capacity() > 0) {
            return state.capacity();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !(stack.getItem() instanceof PortableBeaconItem item)) {
            return 0;
        }
        RegistryAccess registries = server.registryAccess();
        BeaconTierDef tier = BPLookups.tier(registries, item);
        return tier == null ? 0 : BeaconResolver.resolve(tier, BPLookups.installedAugments(stack),
                BPLookups.augments(registries)).fuelCapacity();
    }
}
