package dev.drimoz.portablebeacons.core;

/**
 * The two fuel decisions, pulled out of the ticker so they can be tested without a world.
 *
 * <p>Both are easy to get subtly wrong and expensive when they are: rounding the charge down makes
 * a cheap effect free forever, and accepting fuel the buffer cannot hold destroys the surplus.
 */
public final class FuelBudget {

    /**
     * What a tick costs.
     *
     * <p>Rounded up rather than down: a draw below one unit per tick would otherwise round to zero
     * and run indefinitely on an empty buffer.
     */
    public static int costFor(double perSecond, double seconds) {
        if (perSecond <= 0.0 || seconds <= 0.0) {
            return 0;
        }
        return (int) Math.ceil(perSecond * seconds);
    }

    /**
     * How many fuel items to burn so the buffer covers a charge.
     *
     * <p>The fewest that close the gap, capped by what the slot holds and by what the buffer can
     * take whole. Only whole items: clamping the overflow away would silently destroy most of a
     * netherite ingot, and refusing instead is what gives the Capacity augment and the higher tiers
     * a purpose - they are what unlock the denser fuels.
     *
     * <p>When even that falls short it still burns what fits. The units stay in the buffer rather
     * than being lost, and the beacon then runs dry for want of fuel, not of a refill.
     *
     * @param available items in the fuel slot
     */
    public static int itemsToBurn(int fuel, int cost, int units, int capacity, int available) {
        if (fuel >= cost || units <= 0 || available <= 0) {
            return 0;
        }
        int needed = (int) Math.ceil((cost - fuel) / (double) units);
        int fits = Math.max(0, (capacity - fuel) / units);
        return Math.min(needed, Math.min(fits, available));
    }

    /**
     * The buffer after a recharge: topped up towards capacity, never past it.
     *
     * <p>Never lowered either. A buffer already above capacity - the surplus kept when a Capacity
     * augment comes out - is left alone rather than "recharged" down to the cap.
     */
    public static int recharge(int fuel, int capacity, int amount) {
        if (amount <= 0 || fuel >= capacity) {
            return fuel;
        }
        return (int) Math.min(capacity, (long) fuel + amount);
    }

    private FuelBudget() {}
}
