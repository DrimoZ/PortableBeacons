package dev.drimoz.portablebeacons.net;

import dev.drimoz.portablebeacons.ActionBar;
import dev.drimoz.portablebeacons.BeaconTicker;
import dev.drimoz.portablebeacons.PortableBeacons;
import dev.drimoz.portablebeacons.compat.CuriosCompat;
import dev.drimoz.portablebeacons.core.BPRegistryKeys;
import dev.drimoz.portablebeacons.item.PortableBeaconItem;
import dev.drimoz.portablebeacons.menu.BeaconMenuOpener;
import dev.drimoz.portablebeacons.menu.PortableBeaconMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Switches the running beacon off, or the one the open key would pick on.
 *
 * <p>Carries nothing: as with {@link OpenBeaconPayload}, the server finds the beacon itself. Stopping
 * the drain used to mean opening the screen, which a player mid-fight cannot afford, and a beacon
 * worn as a curio has no item to click at all.
 */
public record ToggleBeaconPayload() implements CustomPacketPayload {

    public static final ToggleBeaconPayload INSTANCE = new ToggleBeaconPayload();

    public static final Type<ToggleBeaconPayload> TYPE = new Type<>(BPRegistryKeys.id("toggle_beacon"));

    public static final StreamCodec<io.netty.buffer.ByteBuf, ToggleBeaconPayload> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void toggle(ServerPlayer player) {
        // Off first: whichever beacon is running is the one the player wants stopped, even if the
        // open key would have picked a different one.
        ItemStack beacon = BeaconTicker.findActiveBeacon(player);
        boolean on = beacon.isEmpty();
        if (on) {
            int slot = BeaconMenuOpener.findBeacon(player);
            if (slot == BeaconMenuOpener.NONE) {
                return;
            }
            beacon = slot == PortableBeaconMenu.CURIO_SLOT
                    ? CuriosCompat.findBeacon(player)
                    : player.getInventory().getItem(slot);
        }
        PortableBeaconItem.setState(beacon, PortableBeaconItem.stateOf(beacon).withActive(on));
        // Same sound as the screen's switch; the action bar is the only sign with the screen closed.
        player.level().playSound(null, player.blockPosition(),
                on ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.4F, 1.0F);
        ActionBar.send(player, Component.translatable(
                on ? "portablebeacons.msg.switched_on" : "portablebeacons.msg.switched_off"));
    }

    @EventBusSubscriber(modid = PortableBeacons.MOD_ID)
    public static final class Handler {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) ->
                    context.enqueueWork(() -> {
                        if (context.player() instanceof ServerPlayer player) {
                            toggle(player);
                        }
                    }));
        }

        private Handler() {}
    }
}
