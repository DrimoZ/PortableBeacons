package dev.drimoz.portablebeacons;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Says something above the hotbar.
 *
 * <p>The action bar packet, sent directly: 26.1 removed {@code Player#displayClientMessage(c, true)},
 * and the two branches share the callers. Silently ignores a client-side player, because there is
 * nothing to send to.
 */
public final class ActionBar {

    public static void send(Player player, Component message) {
        if (player instanceof ServerPlayer server) {
            server.connection.send(new ClientboundSetActionBarTextPacket(message));
        }
    }

    private ActionBar() {}
}
