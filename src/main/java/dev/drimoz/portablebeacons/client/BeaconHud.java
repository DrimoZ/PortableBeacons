package dev.drimoz.portablebeacons.client;

import dev.drimoz.portablebeacons.BPConfig;
import dev.drimoz.portablebeacons.BeaconTicker;
import dev.drimoz.portablebeacons.client.gui.GuiTheme;
import dev.drimoz.portablebeacons.core.Durations;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.gui.GuiLayer;

/**
 * The running beacon and its remaining runtime, in the bottom-right corner.
 *
 * <p>A fuel-burning item whose gauge only shows with its screen open gets checked by opening it,
 * which is what the one-minute warning was papering over. The corner because vanilla draws nothing
 * there, unlike the space around the hotbar - offhand, attack indicator, hearts and hunger.
 */
final class BeaconHud implements GuiLayer {

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        // F1 is checked by hand: vanilla's layers carry a "HUD visible" condition, a mod's do not.
        if (minecraft.player == null || minecraft.options.hideGui || !BPConfig.showHud()
                || !BPConfig.fuelEnabled() || BPConfig.disabledIn(minecraft.player.level().dimension())) {
            return;
        }
        ItemStack beacon = BeaconTicker.findActiveBeacon(minecraft.player);
        if (beacon.isEmpty()) {
            return;
        }
        int seconds = PortableBeaconItem.runtimeSeconds(beacon, PortableBeaconItem.stateOf(beacon),
                minecraft.player.level().registryAccess());
        // Nothing drawing - nothing enabled, or a creative beacon - leaves nothing worth a corner.
        if (seconds < 0) {
            return;
        }
        // The status light's own colours, so the corner and the screen say "low" the same way.
        GuiTheme.Status status = seconds == 0 ? GuiTheme.Status.PROBLEM
                : seconds < BeaconTicker.LOW_FUEL_SECONDS ? GuiTheme.Status.BLOCKED
                : GuiTheme.Status.WORKING;
        String text = Durations.format(seconds);
        int itemX = graphics.guiWidth() - 18;
        int y = graphics.guiHeight() - 18;
        graphics.item(beacon, itemX, y);
        graphics.text(minecraft.font, text, itemX - 2 - minecraft.font.width(text), y + 4,
                status.tabColour, true);
    }
}
