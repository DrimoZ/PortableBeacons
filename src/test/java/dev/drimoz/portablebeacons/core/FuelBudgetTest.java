package dev.drimoz.portablebeacons.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The fuel rules are cheap to get subtly wrong and expensive when they are. */
class FuelBudgetTest {

    @Test
    void aDrawBelowOneUnitPerTickStillCosts() {
        // Rounded down this would be free forever, and a beacon running on an empty buffer looks
        // exactly like a beacon that is broken.
        assertEquals(1, FuelBudget.costFor(0.1, 2.0));
    }

    @Test
    void noDrawCostsNothing() {
        assertEquals(0, FuelBudget.costFor(0.0, 2.0));
    }

    @Test
    void fuelIsBurnedOnlyWhenItFitsWhole() {
        assertEquals(1, FuelBudget.itemsToBurn(0, 10, 3600, 10800, 64));
        assertEquals(1, FuelBudget.itemsToBurn(7200, 7300, 3600, 10800, 64));
        // 7201 + 3600 overflows: burning it would destroy the surplus.
        assertEquals(0, FuelBudget.itemsToBurn(7201, 7300, 3600, 10800, 64));
    }

    @Test
    void fuelDenserThanTheWholeBufferIsNeverBurned() {
        assertEquals(0, FuelBudget.itemsToBurn(0, 10, 28800, 10800, 64));
    }

    @Test
    void nonFuelIsRejected() {
        assertEquals(0, FuelBudget.itemsToBurn(0, 10, 0, 10800, 64));
    }

    @Test
    void aBufferThatCoversTheChargeBurnsNothing() {
        assertEquals(0, FuelBudget.itemsToBurn(500, 400, 300, 36000, 64));
    }

    /**
     * Regression: one item per pass at most meant a charge larger than one item ran the beacon dry
     * with a full stack in the slot - 720 units a pass against 300-unit iron.
     */
    @Test
    void aChargeLargerThanOneItemBurnsEnoughToCoverIt() {
        assertEquals(3, FuelBudget.itemsToBurn(0, 720, 300, 36000, 64));
        assertEquals(2, FuelBudget.itemsToBurn(200, 720, 300, 36000, 64));
    }

    @Test
    void neverBurnsMoreThanTheSlotHolds() {
        assertEquals(1, FuelBudget.itemsToBurn(0, 720, 300, 36000, 1));
    }

    @Test
    void stopsAtWhatTheBufferCanHold() {
        // Two fit, three are needed: burn the two, keep the units, let the beacon run dry honestly.
        assertEquals(2, FuelBudget.itemsToBurn(0, 1000, 300, 600, 64));
    }
}
